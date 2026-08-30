package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Cross-entity check: every market-maker assignment points at a real event, and
 * every event has exactly one market maker.
 */
final class MarketMakerValidator {

    private MarketMakerValidator() {}

    static void validate(Map<Integer, Event> events, Map<String, User> users) throws XmlValidationException {
        Map<Integer, Integer> marketMakersPerEvent = new HashMap<>();

        for (User user : users.values()) {
            for (int eventId : user.getMarketMakerEventIds()) {
                if (!events.containsKey(eventId)) {
                    throw new XmlValidationException("User '" + user.getName()
                            + "' is the market maker for event ID " + eventId + ", which does not exist.");
                }
                marketMakersPerEvent.merge(eventId, 1, Integer::sum);
            }
        }

        for (Event event : events.values()) {
            int count = marketMakersPerEvent.getOrDefault(event.getId(), 0);
            if (count == 0) {
                throw new XmlValidationException(
                        "Event ID " + event.getId() + " ('" + event.getName() + "') has no market maker.");
            }
            if (count > 1) {
                throw new XmlValidationException("Event ID " + event.getId() + " ('" + event.getName()
                        + "') has " + count + " market makers; exactly one is required.");
            }
        }
    }
}
