package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;
import com.guessmarket.engine.xml.jaxb.EventXml;
import com.guessmarket.engine.xml.jaxb.GuessMarketXml;
import com.guessmarket.engine.xml.jaxb.UserXml;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a raw {@link GuessMarketXml} tree into a validated {@link ParsedMarket},
 * delegating field checks to the focused validators and finishing with the
 * cross-entity market-maker check.
 *
 * <p>Events are identified by name everywhere outside this class, but the XML
 * file itself still assigns each {@code <GM-event>} a numeric {@code <id>} and
 * expresses {@code <GM-market-maker>} refs against that id (the file format is
 * unchanged). This class is the one place that still deals with those raw ids:
 * it resolves each ref to the event it names, then discards the id.
 */
final class MarketAssembler {

    private static final Logger LOG = LogManager.getLogger(MarketAssembler.class);

    private MarketAssembler() {}

    static ParsedMarket assemble(GuessMarketXml root) throws XmlValidationException {
        if (root.getEvents() == null || root.getEvents().isEmpty()) {
            throw new XmlValidationException("XML file contains no events (<GM-event> tags).");
        }

        Map<Integer, Event> eventsByRawId = new LinkedHashMap<>();
        Map<String, Event> events = new LinkedHashMap<>();
        HashSet<Integer> usedIds = new HashSet<>();
        HashSet<String> usedEventNames = new HashSet<>();
        for (EventXml eventXml : root.getEvents()) {
            Event event = EventXmlValidator.validate(eventXml, usedIds);
            if (!usedEventNames.add(event.getName())) {
                throw new XmlValidationException("Duplicate event name: " + event.getName() + ".");
            }
            eventsByRawId.put(eventXml.getId(), event);
            events.put(event.getName(), event);
        }

        Map<String, User> users = new LinkedHashMap<>();
        HashSet<String> usedUserNames = new HashSet<>();
        for (UserXml userXml : root.getUsers()) {
            User user = UserXmlValidator.validate(userXml, usedUserNames);
            for (Integer rawEventId : userXml.getMarketMakerEvents()) {
                Event event = eventsByRawId.get(rawEventId);
                if (event == null) {
                    throw new XmlValidationException("User '" + user.getName()
                            + "' is the market maker for event ID " + rawEventId + ", which does not exist.");
                }
                user.addMarketMakerEvent(event.getName());
            }
            users.put(user.getName(), user);
        }

        MarketMakerValidator.validate(events, users);
        LOG.debug("Assembled market from XML: events={}, users={}", events.keySet(), users.keySet());
        return new ParsedMarket(events, users);
    }
}
