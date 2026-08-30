package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.mapper.EventMapper;
import com.guessmarket.engine.mapper.UserMapper;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.TradeReceipt;
import com.guessmarket.engine.model.User;
import com.guessmarket.engine.serialization.StateSerializer;
import com.guessmarket.engine.xml.GuessMarketXmlParser;
import com.guessmarket.engine.xml.jaxb.ParsedXmlWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Facade over the market. It owns no rules of its own: it resolves the DTOs the
 * UI passes to the domain objects held in the {@link MarketCatalog}, delegates
 * the work to those objects, and publishes a change notification. Loading and
 * persistence are the only logic that lives here directly.
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
        ParsedXmlWrapper parsed = GuessMarketXmlParser.parseAndValidateXml(filePath);
        logger.info("Parsed {}: {} events, {} users", filePath,
                parsed.getParsedEvents().size(), parsed.getUsers().size());
        catalog.replace(parsed.getParsedEvents(), parsed.getUsers());
        publisher.publish();
    }

    @Override
    public boolean isFileLoaded() {
        return catalog.isLoaded();
    }

    @Override
    public void saveState(String filePath) throws MarketException {
        catalog.requireLoaded();
        StateSerializer.saveEngineState(catalog.eventMap(), filePath);
    }

    @Override
    public void loadState(String filePath) throws MarketException {
        // NOTE: users are not persisted yet, so a restored session has events only.
        Map<Integer, Event> events = StateSerializer.loadEngineState(filePath);
        catalog.replace(events, Map.of());
        publisher.publish();
    }

    // --- Reads -----------------------------------------------------------

    @Override
    public List<EventDTO> getAllEvents() throws MarketException {
        return catalog.events().stream().map(EventMapper::toEventDTO).toList();
    }

    @Override
    public List<EventDTO> getActiveEvents() throws MarketException {
        return getAllEvents().stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.status()))
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
    public int getNumOfLoadedEvents() {
        return catalog.eventCount();
    }

    // --- Commands (delegate to the Event aggregate) ----------------------

    @Override
    public void activateEvent(EventDTO selectedEvent, UserDTO selectedUser) {
        Event event = catalog.event(selectedEvent.id());
        User marketMaker = catalog.user(selectedUser.name());
        event.open(marketMaker);
        publisher.publish();
    }

    @Override
    public TradeResultDTO buyShares(UserDTO buyerDTO, EventDTO eventDTO, int optionIndex1Based, int quantity)
            throws MarketException {
        Event event = catalog.event(eventDTO.id());
        User buyer = catalog.user(buyerDTO.name());
        TradeReceipt receipt = event.buy(buyer, optionIndex1Based - 1, quantity);
        publisher.publish();
        return new TradeResultDTO(
                receipt.sharesCost(), receipt.commission(), receipt.totalPaid(),
                EventMapper.toEventDetailsDTO(event));
    }

    @Override
    public void closeEvent(int eventId, int winningOptionIndex1Based) throws MarketException {
        Event event = catalog.event(eventId);
        event.settleAndClose(winningOptionIndex1Based - 1);
        publisher.publish();
    }
}
