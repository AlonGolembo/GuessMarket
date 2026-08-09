package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.exception.XmlValidationException;

import java.util.List;

public interface MarketEngine {

    // Reads and parses an XML file containing events
    // Successfully loading a file, resets the current market state
    // Invalid files are rejected and market state isn't changed
    void loadXmlFile(String filePath) throws MarketException, XmlValidationException;

    // Returns true if a valid Events XML is currently loaded
    boolean isFileLoaded();

    // Returns a summary snapshot of all events in the system
    List<EventDTO> getAllEvents() throws MarketException;

    // Returns a summary snapshot of all ACTIVE events in the system
    List<EventDTO> getActiveEvents() throws MarketException;

    // Returns detailed trading status, option prices, account balance, trade history of a specific event
    EventDetailsDTO getEventDetails(int eventId) throws MarketException;

    // Buys shares for a specific active event
    // optionIndex1Based is the required option index but starting with 1 to be user-friendly
    TradeResultDTO buyShares(int eventId, int optionIndex1Based, int quantity) throws MarketException;

    // Closes an event and declares the winning option
    EventDetailsDTO closeEvent(int eventId, int winningOptionIndex1Based) throws MarketException;

    // saves the current market system state
    void saveState(String filePath) throws MarketException;

    // loads a save market system state
    void loadState(String filePath) throws MarketException;

    // get number of loaded events
    int getNumOfLoadedEvents();
}
