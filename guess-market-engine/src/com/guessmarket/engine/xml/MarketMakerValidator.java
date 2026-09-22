package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Cross-entity check: every event has exactly one market maker. (That every
 * market-maker assignment points at a real event is already guaranteed by the
 * id-to-name resolution in {@link MarketAssembler}, before this runs.)
 */
final class MarketMakerValidator {

    private MarketMakerValidator() {}

    static void validate(Map<String, Event> events, Map<String, User> users) throws XmlValidationException {
        Map<String, Integer> marketMakersPerEvent = new HashMap<>();

        for (User user : users.values()) {
            for (String eventName : user.getMarketMakerEventNames()) {
                marketMakersPerEvent.merge(eventName, 1, Integer::sum);
            }
        }

        for (Event event : events.values()) {
            int count = marketMakersPerEvent.getOrDefault(event.getName(), 0);
            if (count == 0) {
                throw new XmlValidationException("Event '" + event.getName() + "' has no market maker.");
            }
            if (count > 1) {
                throw new XmlValidationException("Event '" + event.getName()
                        + "' has " + count + " market makers; exactly one is required.");
            }
        }
    }
}
