package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;
import com.guessmarket.engine.xml.jaxb.EventXml;
import com.guessmarket.engine.xml.jaxb.GuessMarketXml;
import com.guessmarket.engine.xml.jaxb.UserXml;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a raw {@link GuessMarketXml} tree into a validated {@link ParsedMarket},
 * delegating field checks to the focused validators and finishing with the
 * cross-entity market-maker check.
 */
final class MarketAssembler {

    private MarketAssembler() {}

    static ParsedMarket assemble(GuessMarketXml root) throws XmlValidationException {
        if (root.getEvents() == null || root.getEvents().isEmpty()) {
            throw new XmlValidationException("XML file contains no events (<GM-event> tags).");
        }

        Map<Integer, Event> events = new LinkedHashMap<>();
        HashSet<Integer> usedIds = new HashSet<>();
        for (EventXml eventXml : root.getEvents()) {
            Event event = EventXmlValidator.validate(eventXml, usedIds);
            events.put(event.getId(), event);
        }

        Map<String, User> users = new LinkedHashMap<>();
        HashSet<String> usedNames = new HashSet<>();
        for (UserXml userXml : root.getUsers()) {
            User user = UserXmlValidator.validate(userXml, usedNames);
            users.put(user.getName(), user);
        }

        MarketMakerValidator.validate(events, users);
        return new ParsedMarket(events, users);
    }
}
