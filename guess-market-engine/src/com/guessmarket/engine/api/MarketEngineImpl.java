package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.lmsr.LmsrCalculator;
import com.guessmarket.engine.mapper.EventMapper;
import com.guessmarket.engine.model.*;
import com.guessmarket.engine.serialization.StateSerializer;
import com.guessmarket.engine.xml.GuessMarketXmlParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MarketEngineImpl implements MarketEngine{

    private static final Logger logger = LogManager.getLogger(MarketEngineImpl.class);
    private Map<Integer, Event> loadedEvents;
    private boolean isLoaded;

    public MarketEngineImpl(){
        this.loadedEvents = new LinkedHashMap<>();
        this.isLoaded = false;
    }

    @Override
    public void loadXmlFile(String filePath) throws MarketException {

        // Create a list to hold all new events coming from the xml file
        Map<Integer, Event> newEvents = GuessMarketXmlParser.parseAndValidateXml(filePath);
        logger.info("XML File: {} parsed successfully", filePath);

        // Create initial subsidy for each event
        for(Event event : newEvents.values()){
            double initialSubsidy;
            switch (event.getTradingMethod()) {
                case LmsrMethod lmsr -> initialSubsidy = lmsr.getInitialSubsidy();
                case OrderBookMethod ob -> initialSubsidy = ob.getInitialSubsidy();
            };

            event.setEventAccountBalance(initialSubsidy);
            logger.debug("Event {} balance was successfully subsidised", event.getId());
        }

        this.loadedEvents = newEvents;
        logger.debug("{} new events were loaded", newEvents.size());
        this.isLoaded = true;
        logger.debug("isLoaded flag was set to true");
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
    public List<EventDTO> getActiveEvents() throws MarketException {
        return getAllEvents().stream()
                .filter(EventDTO::isActive)
                .toList();
    }

    @Override
    public EventDetailsDTO getEventDetails(int eventId) throws MarketException {
        ensureLoaded();
        Event event = findEventById(eventId);
        return EventMapper.toEventDetailsDTO(event);
    }

    @Override
    public TradeResultDTO buyShares(int eventId, int optionIndex1Based, int quantity) throws MarketException {
        ensureLoaded();
        Event event = findEventById(eventId);

        if (!event.isActive()) {
            throw new MarketException("Cannot buy shares: Event ID " + eventId + " is closed.");
        }

        if (quantity <= 0) {
            throw new MarketException("Quantity to buy must be strictly positive (> 0). Got: " + quantity);
        }

        List<Option> options = event.getOptions();
        if (optionIndex1Based < 1 || optionIndex1Based > options.size()) {
            throw new MarketException("Invalid option choice: " + optionIndex1Based + ". Select between 1 and " + options.size() + ".");
        }

        Option selectedOption = options.get(optionIndex1Based - 1);
        boolean isYesOption = (optionIndex1Based == 1);

        int qYes = options.get(0).getSharesBought();
        int qNo = options.get(1).getSharesBought();

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

        // 3. Mutate Domain State
        selectedOption.addShares(quantity);
        event.setEventAccountBalance(event.getEventAccountBalance() + sharesCost);

        event.addCommission(commissionCost);

        TradeRecord record = new TradeRecord(selectedOption.getName(), quantity, totalPaid);
        event.addTradeRecord(record);

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
}
