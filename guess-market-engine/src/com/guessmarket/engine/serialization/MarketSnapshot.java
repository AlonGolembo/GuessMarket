package com.guessmarket.engine.serialization;

import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The complete persisted market state: every event and every user. Users share
 * object identity with the {@link Event#getParticipants()} they appear in, so a
 * round-trip restores the whole graph consistently.
 */
public record MarketSnapshot(Map<Integer, Event> events, Map<String, User> users) implements Serializable {

    public MarketSnapshot {
        events = new LinkedHashMap<>(events);
        users = new LinkedHashMap<>(users);
    }
}
