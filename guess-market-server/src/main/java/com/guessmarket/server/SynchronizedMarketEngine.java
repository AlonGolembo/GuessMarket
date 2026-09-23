package com.guessmarket.server;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.NewEventDTO;
import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.exception.XmlValidationException;

import java.util.List;
import java.util.Map;

/**
 * Wraps a {@link MarketEngine} so every call is serialized behind one lock.
 * {@code MarketEngineImpl} was written assuming single-threaded (JavaFX)
 * access; the server hits it from many concurrent request threads, so this is
 * the seam that makes that safe. Coarse-grained on purpose - correctness over
 * throughput for a system this size; no per-aggregate locking.
 */
final class SynchronizedMarketEngine implements MarketEngine {

    private final MarketEngine delegate;
    private final Object lock = new Object();

    SynchronizedMarketEngine(MarketEngine delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addListener(MarketDataChangeListener listener) {
        synchronized (lock) {
            delegate.addListener(listener);
        }
    }

    @Override
    public void removeListener(MarketDataChangeListener listener) {
        synchronized (lock) {
            delegate.removeListener(listener);
        }
    }

    @Override
    public void loadXmlFile(String filePath, String uploaderName) throws MarketException, XmlValidationException {
        synchronized (lock) {
            delegate.loadXmlFile(filePath, uploaderName);
        }
    }

    @Override
    public List<String> uploadEventsXml(String uploaderName, String xmlContent)
            throws MarketException, XmlValidationException {
        synchronized (lock) {
            return delegate.uploadEventsXml(uploaderName, xmlContent);
        }
    }

    @Override
    public boolean isFileLoaded() {
        synchronized (lock) {
            return delegate.isFileLoaded();
        }
    }

    @Override
    public UserDTO login(String name) throws MarketException {
        synchronized (lock) {
            return delegate.login(name);
        }
    }

    @Override
    public void deposit(String userName, double amount) throws MarketException {
        synchronized (lock) {
            delegate.deposit(userName, amount);
        }
    }

    @Override
    public List<EventDTO> getAllEvents() throws MarketException {
        synchronized (lock) {
            return delegate.getAllEvents();
        }
    }

    @Override
    public EventDTO createEvent(NewEventDTO spec) throws MarketException {
        synchronized (lock) {
            return delegate.createEvent(spec);
        }
    }

    @Override
    public Map<String, UserDTO> getAllUsers() throws MarketException {
        synchronized (lock) {
            return delegate.getAllUsers();
        }
    }

    @Override
    public List<EventDTO> getActiveEvents() throws MarketException {
        synchronized (lock) {
            return delegate.getActiveEvents();
        }
    }

    @Override
    public EventDetailsDTO getEventDetails(String eventName) throws MarketException {
        synchronized (lock) {
            return delegate.getEventDetails(eventName);
        }
    }

    @Override
    public UserDetailsDTO getUserDetails(String name) throws MarketException {
        synchronized (lock) {
            return delegate.getUserDetails(name);
        }
    }

    @Override
    public TradeQuoteDTO quoteTrade(String userName, String eventName, int optionIndex1Based, int quantity)
            throws MarketException {
        synchronized (lock) {
            return delegate.quoteTrade(userName, eventName, optionIndex1Based, quantity);
        }
    }

    @Override
    public TradeResultDTO buyShares(String userName, String eventName, int optionIndex1Based, int quantity)
            throws MarketException {
        synchronized (lock) {
            return delegate.buyShares(userName, eventName, optionIndex1Based, quantity);
        }
    }

    @Override
    public OrderResultDTO placeOrder(String userName, String eventName, int optionIndex1Based, OrderSide side,
                                      int quantity, double price) throws MarketException {
        synchronized (lock) {
            return delegate.placeOrder(userName, eventName, optionIndex1Based, side, quantity, price);
        }
    }

    @Override
    public void cancelOrder(String userName, String eventName, long orderId) throws MarketException {
        synchronized (lock) {
            delegate.cancelOrder(userName, eventName, orderId);
        }
    }

    @Override
    public void closeEvent(String eventName, int winningOptionIndex1Based) throws MarketException {
        synchronized (lock) {
            delegate.closeEvent(eventName, winningOptionIndex1Based);
        }
    }

    @Override
    public void saveState(String filePath) throws MarketException {
        synchronized (lock) {
            delegate.saveState(filePath);
        }
    }

    @Override
    public void loadState(String filePath) throws MarketException {
        synchronized (lock) {
            delegate.loadState(filePath);
        }
    }

    @Override
    public int getNumOfLoadedEvents() {
        synchronized (lock) {
            return delegate.getNumOfLoadedEvents();
        }
    }

    @Override
    public void activateEvent(String eventName, String userName) {
        synchronized (lock) {
            delegate.activateEvent(eventName, userName);
        }
    }
}
