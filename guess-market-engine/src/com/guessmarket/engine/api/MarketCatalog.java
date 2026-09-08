package com.guessmarket.engine.api;

import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the currently loaded market — events and users — and does the lookups.
 * Every accessor first checks that something is loaded, so callers get a clear
 * "load a file first" message instead of a {@code null}.
 */
final class MarketCatalog {

    private Map<Integer, Event> events = new LinkedHashMap<>();
    private Map<String, User> users = new LinkedHashMap<>();
    private boolean loaded;

    /** Replaces the whole catalog (XML load, or state restore). */
    void replace(Map<Integer, Event> events, Map<String, User> users) {
        this.events = new LinkedHashMap<>(events);
        this.users = new LinkedHashMap<>(users);
        this.loaded = true;
    }

    /** Adds one freshly created event to the loaded market. */
    void addEvent(Event event) {
        requireLoaded();
        events.put(event.getId(), event);
    }

    /** The lowest positive id not already used by a loaded event. */
    int nextEventId() {
        requireLoaded();
        return events.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    boolean isLoaded() {
        return loaded;
    }

    void requireLoaded() {
        if (!loaded) {
            throw new MarketException("No market data is loaded. Load an XML file first.");
        }
    }

    Collection<Event> events() {
        requireLoaded();
        return events.values();
    }

    Collection<User> users() {
        requireLoaded();
        return users.values();
    }

    /** The live event map, for the serializer only. */
    Map<Integer, Event> eventMap() {
        requireLoaded();
        return events;
    }

    Map<String, User> userMap() {
        requireLoaded();
        return users;
    }

    Event event(int id) {
        requireLoaded();
        Event event = events.get(id);
        if (event == null) {
            throw new MarketException("Event with ID " + id + " does not exist.");
        }
        return event;
    }

    User user(String name) {
        requireLoaded();
        User user = users.get(name);
        if (user == null) {
            throw new MarketException("Unknown user: " + name);
        }
        return user;
    }

    int eventCount() {
        return events.size();
    }
}
