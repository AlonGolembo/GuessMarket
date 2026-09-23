package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.NewEventDTO;
import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.exception.XmlValidationException;


import java.util.List;
import java.util.Map;

public interface MarketEngine {
    void addListener(MarketDataChangeListener listener);
    void removeListener(MarketDataChangeListener listener);

    // Reads and parses an XML file containing events, and adds them to the market.
    // uploaderName must already be a logged-in user; they become the market maker
    // of every event the file contains. Rejected (as a whole, nothing added) if
    // the file is invalid or any event in it collides by name with one that
    // already exists.
    void loadXmlFile(String filePath, String uploaderName) throws MarketException, XmlValidationException;

    // Same as loadXmlFile, but for XML content already in memory rather than a
    // file on disk - for a server receiving an upload, which must never write
    // the raw bytes to disk. Returns the names of the events that were added.
    List<String> uploadEventsXml(String uploaderName, String xmlContent) throws MarketException, XmlValidationException;

    // Returns true once at least one event has been loaded/created
    boolean isFileLoaded();

    // Registers a new user with a unique (case-insensitive) name, starting at a $0
    // balance. Rejects a blank or already-taken name.
    UserDTO login(String name) throws MarketException;

    // Adds funds to a user's own account.
    void deposit(String userName, double amount) throws MarketException;

    // Returns a summary snapshot of all events in the system
    List<EventDTO> getAllEvents() throws MarketException;

    // Creates a new (NOT_ACTIVE) event with the chosen participant as its market maker,
    // after checking that participant can fund the trading-method subsidy. The event is
    // not opened - activate it separately. Returns the created event.
    EventDTO createEvent(NewEventDTO spec) throws MarketException;

    Map<String, UserDTO> getAllUsers() throws MarketException;

    // Returns a summary snapshot of all ACTIVE events in the system
    List<EventDTO> getActiveEvents() throws MarketException;

    // Returns detailed trading status, option prices, account balance, trade history of a specific event
    EventDetailsDTO getEventDetails(String eventName) throws MarketException;

    // Returns a user's summary plus their full cash-balance history (oldest entry first)
    UserDetailsDTO getUserDetails(String name) throws MarketException;

    // Prices a prospective trade without executing it. buyShares() charges exactly
    // the returned breakdown.
    TradeQuoteDTO quoteTrade(String userName, String eventName, int optionIndex1Based, int quantity) throws MarketException;

    // Buys shares for a specific active event
    // optionIndex1Based is the required option index but starting with 1 to be user-friendly
    TradeResultDTO buyShares(String userName, String eventName, int optionIndex1Based, int quantity) throws MarketException;

    // Places a limit order on an Order Book event; matched immediately against
    // the book, remainder rests. optionIndex1Based starts at 1.
    OrderResultDTO placeOrder(String userName, String eventName, int optionIndex1Based, OrderSide side,
                               int quantity, double price) throws MarketException;

    // Cancels a still-resting order the given user placed on an Order Book event.
    void cancelOrder(String userName, String eventName, long orderId) throws MarketException;

    // Closes an event and declares the winning option
    void closeEvent(String eventName, int winningOptionIndex1Based) throws MarketException;

    // saves the current market system state
    void saveState(String filePath) throws MarketException;

    // loads a save market system state
    void loadState(String filePath) throws MarketException;

    // get number of loaded events
    int getNumOfLoadedEvents();

    void activateEvent(String eventName, String userName);
}
