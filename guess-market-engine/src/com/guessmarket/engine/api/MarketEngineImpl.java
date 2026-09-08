package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.NewEventDTO;
import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.exception.InsufficientFundsException;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.mapper.EventMapper;
import com.guessmarket.engine.mapper.UserMapper;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.LmsrMethod;
import com.guessmarket.engine.model.Option;
import com.guessmarket.engine.model.OrderBookMethod;
import com.guessmarket.engine.model.OrderOutcome;
import com.guessmarket.engine.model.TradeReceipt;
import com.guessmarket.engine.model.TradingMethod;
import com.guessmarket.engine.model.User;
import com.guessmarket.engine.serialization.MarketSnapshot;
import com.guessmarket.engine.serialization.StateSerializer;
import com.guessmarket.engine.xml.GuessMarketXmlParser;
import com.guessmarket.engine.xml.ParsedMarket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Facade over the market. It owns no rules of its own: it resolves the DTOs the
 * UI passes to the domain objects held in the {@link MarketCatalog}, delegates
 * the work to those objects, and publishes a change notification. Loading,
 * persistence and assembling a new event from a {@link NewEventDTO} are the only
 * logic that lives here directly.
 */
public class MarketEngineImpl implements MarketEngine {

    private static final Logger logger = LogManager.getLogger(MarketEngineImpl.class);

    private final MarketCatalog catalog = new MarketCatalog();
    private final MarketEventPublisher publisher = new MarketEventPublisher();

    @Override
    public void addListener(MarketDataChangeListener listener) {
        publisher.add(listener);
    }

    @Override
    public void removeListener(MarketDataChangeListener listener) {
        publisher.remove(listener);
    }

    // --- Loading & persistence --------------------------------------------

    @Override
    public void loadXmlFile(String filePath) throws MarketException {
        logger.info("Loading market XML: {}", filePath);
        try {
            ParsedMarket parsed = GuessMarketXmlParser.parseAndValidateXml(filePath);
            catalog.replace(parsed.events(), parsed.users());
            logger.info("Loaded {} event(s) and {} user(s) from {}",
                    parsed.events().size(), parsed.users().size(), filePath);
        } catch (MarketException e) {
            logger.warn("Rejected XML {}: {}", filePath, e.getMessage());
            throw e;
        }
        publisher.publish();
    }

    @Override
    public boolean isFileLoaded() {
        return catalog.isLoaded();
    }

    @Override
    public void saveState(String filePath) throws MarketException {
        catalog.requireLoaded();
        logger.info("Saving market state to {}", filePath);
        StateSerializer.save(new MarketSnapshot(catalog.eventMap(), catalog.userMap()), filePath);
    }

    @Override
    public void loadState(String filePath) throws MarketException {
        logger.info("Restoring market state from {}", filePath);
        MarketSnapshot snapshot = StateSerializer.load(filePath);
        catalog.replace(snapshot.events(), snapshot.users());
        logger.info("Restored {} event(s) and {} user(s)", snapshot.events().size(), snapshot.users().size());
        publisher.publish();
    }

    // --- Reads -----------------------------------------------------------

    @Override
    public List<EventDTO> getAllEvents() throws MarketException {
        return catalog.events().stream().map(EventMapper::toEventDTO).toList();
    }

    @Override
    public EventDTO createEvent(NewEventDTO spec) throws MarketException {
        catalog.requireLoaded();

        User marketMaker = catalog.user(spec.marketMakerName());
        List<Option> options = buildOptions(spec.optionNames());
        TradingMethod method = buildMethod(spec);
        try {
            method.validate();
        } catch (XmlValidationException e) {
            throw new MarketException(e.getMessage(), e);
        }

        int id = catalog.nextEventId();
        Event event;
        try {
            event = new Event(id, requireText(spec.name(), "name"), requireText(spec.description(), "description"),
                    spec.commissionPercentage(), spec.commissionType(), options, method);
        } catch (IllegalArgumentException e) {
            throw new MarketException(e.getMessage(), e);
        }

        double subsidy = method.initialSubsidy();
        double balance = marketMaker.getAccountBalance();
        if (subsidy > balance) {
            throw new InsufficientFundsException(subsidy, balance);
        }

        // All checks passed - commit.
        marketMaker.addMarketMakerEvent(id);
        catalog.addEvent(event);
        logger.info("Created event {} ('{}'), market maker '{}' (subsidy {}, balance {})",
                id, event.getName(), marketMaker.getName(), subsidy, balance);
        publisher.publish();
        return EventMapper.toEventDTO(event);
    }

    private static List<Option> buildOptions(List<String> names) {
        if (names == null || names.size() != 2) {
            throw new MarketException("An event needs exactly two options.");
        }
        String first = names.get(0) == null ? "" : names.get(0).trim();
        String second = names.get(1) == null ? "" : names.get(1).trim();
        if (first.isEmpty() || second.isEmpty()) {
            throw new MarketException("Both option names are required.");
        }
        if (first.equalsIgnoreCase(second)) {
            throw new MarketException("The two options must have different names.");
        }
        return List.of(new Option(first), new Option(second));
    }

    private static TradingMethod buildMethod(NewEventDTO spec) {
        if (spec.tradingMethod() == null) {
            throw new MarketException("Choose a trading method.");
        }
        return switch (spec.tradingMethod()) {
            case LMSR -> {
                if (spec.lmsrB() == null) {
                    throw new MarketException("LMSR needs a liquidity parameter (b).");
                }
                yield new LmsrMethod(spec.lmsrB());
            }
            case ORDERBOOK -> {
                if (spec.orderBookD() == null || spec.orderBookInitial() == null) {
                    throw new MarketException("Order Book needs a base value (d) and an initial allocation.");
                }
                yield new OrderBookMethod(spec.orderBookD(), spec.orderBookInitial(), spec.orderBookAllowMint());
            }
            case NONE, NOTDEFINED -> throw new MarketException("Unsupported trading method: " + spec.tradingMethod());
        };
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MarketException("Event " + field + " is required.");
        }
        return value.trim();
    }

    @Override
    public List<EventDTO> getActiveEvents() throws MarketException {
        return getAllEvents().stream()
                .filter(e -> e.status() == EventStatus.ACTIVE)
                .toList();
    }

    @Override
    public Map<String, UserDTO> getAllUsers() throws MarketException {
        return catalog.users().stream()
                .map(UserMapper::toUserDTO)
                .collect(Collectors.toMap(UserDTO::name, u -> u));
    }

    @Override
    public EventDetailsDTO getEventDetails(int eventId) throws MarketException {
        return EventMapper.toEventDetailsDTO(catalog.event(eventId));
    }

    @Override
    public UserDetailsDTO getUserDetails(String name) throws MarketException {
        return UserMapper.toUserDetailsDTO(catalog.user(name));
    }

    @Override
    public int getNumOfLoadedEvents() {
        return catalog.eventCount();
    }

    // --- Commands (delegate to the Event aggregate) ----------------------

    @Override
    public void activateEvent(EventDTO selectedEvent, UserDTO selectedUser) {
        Event event = catalog.event(selectedEvent.id());
        User marketMaker = catalog.user(selectedUser.name());
        event.open(marketMaker);
        logger.info("Event {} ('{}') activated by market maker '{}'",
                event.getId(), event.getName(), marketMaker.getName());
        publisher.publish();
    }

    @Override
    public TradeQuoteDTO quoteTrade(UserDTO buyerDTO, EventDTO eventDTO, int optionIndex1Based, int quantity)
            throws MarketException {
        TradeReceipt r = catalog.event(eventDTO.id()).quote(optionIndex1Based - 1, quantity);
        return new TradeQuoteDTO(r.sharesCost(), r.commission(), r.totalPaid(), r.filledQuantity());
    }

    @Override
    public TradeResultDTO buyShares(UserDTO buyerDTO, EventDTO eventDTO, int optionIndex1Based, int quantity)
            throws MarketException {
        Event event = catalog.event(eventDTO.id());
        User buyer = catalog.user(buyerDTO.name());
        TradeReceipt receipt = event.buy(buyer, optionIndex1Based - 1, quantity);
        logger.info("'{}' bought {} of {} requested share(s) of option #{} in event {} for {} (cost {}, commission {})",
                buyer.getName(), receipt.filledQuantity(), quantity, optionIndex1Based, event.getId(),
                receipt.totalPaid(), receipt.sharesCost(), receipt.commission());
        publisher.publish();
        return new TradeResultDTO(
                receipt.sharesCost(), receipt.commission(), receipt.totalPaid(), receipt.filledQuantity(),
                EventMapper.toEventDetailsDTO(event));
    }

    @Override
    public OrderResultDTO placeOrder(UserDTO userDTO, EventDTO eventDTO, int optionIndex1Based, OrderSide side,
                                      int quantity, double price) throws MarketException {
        Event event = catalog.event(eventDTO.id());
        User user = catalog.user(userDTO.name());
        OrderOutcome outcome = event.placeOrder(user, optionIndex1Based - 1, side, quantity, price);
        logger.info("'{}' placed a {} order for {} share(s) of option #{} in event {} @ {}: filled {}, resting {}",
                user.getName(), side, quantity, optionIndex1Based, event.getId(), price,
                outcome.filledQuantity(), outcome.restingQuantity());
        publisher.publish();
        return new OrderResultDTO(
                outcome.filledQuantity(), outcome.restingQuantity(), outcome.cashMoved(), outcome.commission(),
                outcome.restingQuantity() > 0 ? outcome.orderId() : null,
                EventMapper.toEventDetailsDTO(event));
    }

    @Override
    public void cancelOrder(UserDTO userDTO, EventDTO eventDTO, long orderId) throws MarketException {
        Event event = catalog.event(eventDTO.id());
        User user = catalog.user(userDTO.name());
        event.cancelOrder(user, orderId);
        logger.info("'{}' cancelled order {} in event {}", user.getName(), orderId, event.getId());
        publisher.publish();
    }

    @Override
    public void closeEvent(int eventId, int winningOptionIndex1Based) throws MarketException {
        Event event = catalog.event(eventId);
        event.settleAndClose(winningOptionIndex1Based - 1);
        logger.info("Event {} ('{}') closed; winning option #{}",
                event.getId(), event.getName(), winningOptionIndex1Based);
        publisher.publish();
    }
}
