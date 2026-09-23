package com.guessmarket.engine.api;

import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the currently known market — events and users — and does the lookups.
 * Users register independently of any file (via {@link #addUser}) and events
 * accumulate across uploads (via {@link #addEvent}); neither collection is
 * gated behind a "something must be loaded first" flag, so both start empty
 * and just stay that way until something adds to them.
 */
final class MarketCatalog {

    private Map<String, Event> events = new LinkedHashMap<>();
    private Map<String, User> users = new LinkedHashMap<>();

    /** Full state restore (state-file load): replaces both events and users wholesale. */
    void replace(Map<String, Event> events, Map<String, User> users) {
        this.events = new LinkedHashMap<>(events);
        this.users = new LinkedHashMap<>(users);
    }

    /** Registers a newly logged-in user. Rejects a name already taken (case-insensitive). */
    void addUser(User user) {
        if (hasUser(user.getName())) {
            throw new MarketException("A user named '" + user.getName() + "' is already logged in.");
        }
        users.put(user.getName(), user);
    }

    /** Adds one event - freshly created, or from an XML upload. Rejects a name already in use (case-insensitive). */
    void addEvent(Event event) {
        if (hasEvent(event.getName())) {
            throw new MarketException("An event named '" + event.getName() + "' already exists.");
        }
        events.put(event.getName(), event);
    }

    boolean hasEvent(String name) {
        return events.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
    }

    boolean hasUser(String name) {
        return users.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
    }

    Collection<Event> events() {
        return events.values();
    }

    Collection<User> users() {
        return users.values();
    }

    /** The live event map, for the serializer only. */
    Map<String, Event> eventMap() {
        return events;
    }

    Map<String, User> userMap() {
        return users;
    }

    Event event(String name) {
        Event event = events.get(name);
        if (event == null) {
            throw new MarketException("Event '" + name + "' does not exist.");
        }
        return event;
    }

    User user(String name) {
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
