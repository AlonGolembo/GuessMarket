package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.xml.jaxb.EventXml;
import com.guessmarket.engine.xml.jaxb.GuessMarketXml;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a raw {@link GuessMarketXml} tree into a validated {@link ParsedMarket},
 * delegating field checks (including the case-insensitive duplicate-name
 * rejection) to {@link EventXmlValidator}. The file may still carry a numeric
 * {@code <id>} per {@code <GM-event>} (the element still parses if present),
 * but nothing reads it - names are the only identity that survives assembly.
 */
final class MarketAssembler {

    private static final Logger LOG = LogManager.getLogger(MarketAssembler.class);

    private MarketAssembler() {}

    static ParsedMarket assemble(GuessMarketXml root) throws XmlValidationException {
        if (root.getEvents() == null || root.getEvents().isEmpty()) {
            throw new XmlValidationException("XML file contains no events (<GM-event> tags).");
        }

        Map<String, Event> events = new LinkedHashMap<>();
        HashSet<String> usedNamesLowercase = new HashSet<>();
        for (EventXml eventXml : root.getEvents()) {
            Event event = EventXmlValidator.validate(eventXml, usedNamesLowercase);
            events.put(event.getName(), event);
        }

        LOG.debug("Assembled market from XML: events={}", events.keySet());
        return new ParsedMarket(events);
    }
}
