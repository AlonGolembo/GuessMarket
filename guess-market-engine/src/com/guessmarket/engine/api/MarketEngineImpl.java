package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.InsufficientFundsException;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.lmsr.LmsrCalculator;
import com.guessmarket.engine.mapper.EventMapper;
import com.guessmarket.engine.mapper.UserMapper;
import com.guessmarket.engine.model.*;
import com.guessmarket.engine.serialization.StateSerializer;
import com.guessmarket.engine.xml.GuessMarketXmlParser;
import com.guessmarket.engine.xml.jaxb.ParsedXmlWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class MarketEngineImpl implements MarketEngine{


    private static final Logger logger = LogManager.getLogger(MarketEngineImpl.class);
    private Map<Integer, Event> loadedEvents;
    private Map<String, User> users;
    private boolean isLoaded;
    private final List<MarketDataChangeListener> listeners = new ArrayList<>();

    public MarketEngineImpl(){
        this.loadedEvents = new LinkedHashMap<>();
        this.users = new HashMap<>();
        this.isLoaded = false;
    }

    @Override
    public void addListener(MarketDataChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(MarketDataChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (MarketDataChangeListener listener : listeners) {
            listener.onMarketDataChanged();
        }
    }

    @Override
    public void loadXmlFile(String filePath) throws MarketException {

        // Create a list to hold all new events coming from the xml file
        ParsedXmlWrapper parsedXml = GuessMarketXmlParser.parseAndValidateXml(filePath);
        logger.info("XML File: {} parsed successfully", filePath);

        Map<Integer, Event> newEvents = parsedXml.getParsedEvents();
        Map<String,User>  newUsers = parsedXml.getUsers();

        this.loadedEvents = newEvents;
        logger.debug("{} new events were loaded", newEvents.size());
        this.users = newUsers;
        logger.debug("{} new users were loaded", newUsers.size());
        this.isLoaded = true;
        logger.debug("isLoaded flag was set to true");

        notifyListeners();
    }

    @Override
    public boolean isFileLoaded() {
        return this.isLoaded;
    }

    @Override
    public List<EventDTO> getAllEvents() throws MarketException {
        ensureLoaded();
        return this.loadedEvents.values().stream()
                .map(EventMapper::toEventDTO)
                .toList();
    }

    @Override
    public Map<String, UserDTO> getAllUsers() throws MarketException {
        return this.users.values().stream()
                .map(UserMapper::toUserDTO)
                .collect(java.util.stream.Collectors.toMap(UserDTO::name, u -> u));
    }

    @Override
    public List<EventDTO> getActiveEvents() throws MarketException {
        return getAllEvents().stream()
                .filter(event -> "ACTIVE".equalsIgnoreCase(event.status()))
                .toList();
    }

    @Override
    public EventDetailsDTO getEventDetails(int eventId) throws MarketException {
        ensureLoaded();
        Event event = findEventById(eventId);
        return EventMapper.toEventDetailsDTO(event);
    }

    @Override
    public TradeResultDTO buyShares(UserDTO buyerDTO, EventDTO eventDTO, int optionIndex1Based, int quantity) throws MarketException {
        ensureLoaded();
        Event event = findEventById(eventDTO.id());

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new MarketException("Cannot buy shares: Event ID " + event.getId() + " is not open for trading.");
        }

        if (quantity <= 0) {
            throw new MarketException("Quantity to buy must be strictly positive (> 0). Got: " + quantity);
        }

        List<Option> options = event.getOptions();
        if (optionIndex1Based < 1 || optionIndex1Based > options.size()) {
            throw new MarketException("Invalid option choice: " + optionIndex1Based + ". Select between 1 and " + options.size() + ".");
        }

        User buyer = this.users.get(buyerDTO.name());
        if (buyer == null) {
            throw new MarketException("Unknown user: " + buyerDTO.name());
        }

        Option selectedOption = options.get(optionIndex1Based - 1);
        boolean isYesOption = (optionIndex1Based == 1);

        int qYes = options.get(0).getSharesOutstanding();
        int qNo = options.get(1).getSharesOutstanding();

        // HACK: Temporary use this switch case to continue only with LMSR method
        // FIXME: Refactor when implement Order-Book method
        int b = switch (event.getTradingMethod()) {
            case LmsrMethod lmsr -> lmsr.getB();
            case OrderBookMethod ob -> 0; // Order books don't have b
        };

        // 1. Calculate LMSR cost
        double sharesCost = LmsrCalculator.calculateTradeCost(qYes, qNo, b, isYesOption, quantity);

        // 2. Calculate fee if 'on-purchase'
        double commissionCost = 0.0;
        if (event.getCommissionType() == CommissionType.ON_PURCHASE) {
            commissionCost = sharesCost * (event.getCommissionPercentage() / 100.0);
        }

        double totalPaid = sharesCost + commissionCost;

        // 3. Mutate state. Debit the buyer first: if they cannot pay this throws
        //    InsufficientFundsException before any event state has changed.
        buyer.debit(totalPaid);
        event.issueShares(selectedOption, quantity);
        event.recordTradeProceeds(sharesCost);
        event.addCommission(commissionCost);

        TradeRecord record = new TradeRecord(buyer.getName(), selectedOption.getName(), quantity, totalPaid);
        event.addTradeRecord(record);

        notifyListeners();

        // 4. Return execution receipt DTO
        return new TradeResultDTO(sharesCost, commissionCost, totalPaid, EventMapper.toEventDetailsDTO(event));
    }

    @Override
    public void closeEvent(int eventId, int winningOptionIndex1Based) throws MarketException {
        ensureLoaded();
        Event event = findEventById(eventId);

        List<Option> options = event.getOptions();
        if (winningOptionIndex1Based < 1 || winningOptionIndex1Based > options.size()) {
            throw new MarketException("Invalid winning option choice: " + winningOptionIndex1Based);
        }

        Option winningOption = options.get(winningOptionIndex1Based - 1);

        // Delegate settlement, commission calculation, payout distribution, and closing to Event
        event.settleAndClose(winningOption);

        notifyListeners();
    }

    @Override
    public void saveState(String filePath) throws MarketException {
        ensureLoaded();
        StateSerializer.saveEngineState(this.loadedEvents, filePath);
    }

    @Override
    public void loadState(String filePath) throws MarketException {
        this.loadedEvents = StateSerializer.loadEngineState(filePath);
        this.isLoaded = true;
    }

    // Private helper methods
    private void ensureLoaded() throws MarketException {
        if(!isFileLoaded()){
            throw new MarketException("No valid XML file is currently loaded in the system! Please load an XML file first.");
        }
    }

    private Event findEventById(int eventId) throws MarketException {
        if (!this.loadedEvents.containsKey(eventId)) {
            throw new MarketException("Event with ID " + eventId + " does not exist.");
        }

        return this.loadedEvents.get(eventId);
    }

    @Override
    public int getNumOfLoadedEvents() {
        return this.loadedEvents.keySet().size();
    }

    @Override
    public void activateEvent(EventDTO selectedEvent, UserDTO selectedUser) throws MarketException {
        ensureLoaded();
        Event event = findEventById(selectedEvent.id());
        User user = this.users.get(selectedUser.name());
        if (user == null) {
            throw new MarketException("Unknown user: " + selectedUser.name());
        }

        // Only the event's market maker may open it, and only if they can fund the subsidy.
        if (!user.isMarketMakerFor(event.getId())) {
            throw new MarketException(
                    "User '" + user.getName() + "' is not the market maker for event ID " + event.getId() + ".");
        }

        Double rawSubsidy = event.getTradingMethod().getInitialSubsidy();
        double subsidy = (rawSubsidy == null || rawSubsidy.isNaN()) ? 0.0 : rawSubsidy;
        if (user.getAccountBalance() < subsidy) {
            throw new InsufficientFundsException(subsidy, user.getAccountBalance());
        }

        // Validation passed: now mutate, in an order that cannot leave a half-open event.
        if (subsidy > 0) {
            user.debit(subsidy);
        }
        event.activate();

        notifyListeners();
    }
}
