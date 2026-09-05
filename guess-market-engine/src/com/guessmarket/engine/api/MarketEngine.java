package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.exception.XmlValidationException;


import java.util.List;
import java.util.Map;

public interface MarketEngine {
    void addListener(MarketDataChangeListener listener);
    void removeListener(MarketDataChangeListener listener);

    // Reads and parses an XML file containing events
    // Successfully loading a file, resets the current market state
    // Invalid files are rejected and market state isn't changed
    void loadXmlFile(String filePath) throws MarketException, XmlValidationException;

    // Returns true if a valid Events XML is currently loaded
    boolean isFileLoaded();

    // Returns a summary snapshot of all events in the system
    List<EventDTO> getAllEvents() throws MarketException;

    Map<String, UserDTO> getAllUsers() throws MarketException;

    // Returns a summary snapshot of all ACTIVE events in the system
    List<EventDTO> getActiveEvents() throws MarketException;

    // Returns detailed trading status, option prices, account balance, trade history of a specific event
    EventDetailsDTO getEventDetails(int eventId) throws MarketException;

    // Prices a prospective trade without executing it. buyShares() charges exactly
    // the returned breakdown.
    TradeQuoteDTO quoteTrade(UserDTO buyer, EventDTO event, int optionIndex1Based, int quantity) throws MarketException;

    // Buys shares for a specific active event
    // optionIndex1Based is the required option index but starting with 1 to be user-friendly
    TradeResultDTO buyShares(UserDTO buyer, EventDTO event, int optionIndex1Based, int quantity) throws MarketException;

    // Places a limit order on an Order Book event; matched immediately against
    // the book, remainder rests. optionIndex1Based starts at 1.
    OrderResultDTO placeOrder(UserDTO user, EventDTO event, int optionIndex1Based, OrderSide side,
                               int quantity, double price) throws MarketException;

    // Cancels a still-resting order the given user placed on an Order Book event.
    void cancelOrder(UserDTO user, EventDTO event, long orderId) throws MarketException;

    // Closes an event and declares the winning option
    void closeEvent(int eventId, int winningOptionIndex1Based) throws MarketException;

    // saves the current market system state
    void saveState(String filePath) throws MarketException;

    // loads a save market system state
    void loadState(String filePath) throws MarketException;

    // get number of loaded events
    int getNumOfLoadedEvents();

    void activateEvent(EventDTO selectedEvent, UserDTO selectedUser);
}
