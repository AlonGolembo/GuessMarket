package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.CommissionType;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.Option;
import com.guessmarket.engine.xml.jaxb.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import javax.xml.transform.stream.StreamSource;

import java.io.File;
import java.util.*;

public class XmlEventParser {

    @Contract("null -> fail")
    public static @NotNull Map<Integer, Event> parseAndValidateXml(String filePath) throws XmlValidationException {
        if (filePath == null || filePath.isBlank()) {
            throw new XmlValidationException("File path cannot be empty.");
        }

        File file = new File(filePath.trim());

        if (!file.exists()) {
            throw new XmlValidationException("File does not exist at path: " + filePath);
        }

        if (!file.getName().toLowerCase().endsWith(".xml")) {
            throw new XmlValidationException("File must have a .xml extension. Provided: " + file.getName());
        }

        try {
            // 1. Initialize JAXB Unmarshaller
            JAXBContext context = JAXBContext.newInstance(GuessMarketXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            // 2. Unmarshall XML into Java Object Graph
            JAXBElement<GuessMarketXml> jaxbElement = unmarshaller.unmarshal(
                    new StreamSource(file),
                    GuessMarketXml.class
            );
            GuessMarketXml root = jaxbElement.getValue();

            if (root == null || root.getEvents() == null || root.getEvents().isEmpty()) {
                throw new XmlValidationException("XML file contains no events (<GM-event> tags).");
            }

            // 3. Map & Validate Events
            Map<Integer, Event> parsedEvents = new LinkedHashMap<>();
            Set<Integer> usedIds = new HashSet<>();

            for (EventXml eventXml : root.getEvents()) {
                Event event = validateAndConvert(eventXml, usedIds);
                parsedEvents.put(event.getId(), event);
            }

            return parsedEvents;

        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException("Failed to parse XML file: " + e.getMessage(), e);
        }
    }

    private static Event validateAndConvert(EventXml xml, Set<Integer> usedIds) throws XmlValidationException {
        // 1. Validate Name
        if (xml.getName() == null || xml.getName().isBlank()) {
            throw new XmlValidationException("Event is missing a valid 'name' attribute.");
        }
        String name = xml.getName().trim();

        // 2. Validate ID
        if (xml.getId() == null) {
            throw new XmlValidationException("Event '" + name + "' is missing required <id> tag.");
        }
        int id = xml.getId();
        if (usedIds.contains(id)) {
            throw new XmlValidationException("Duplicate Event ID found: " + id + ". Event IDs must be unique.");
        }
        usedIds.add(id);

        // 3. Validate Description
        if (xml.getDescription() == null || xml.getDescription().isBlank()) {
            throw new XmlValidationException("Event ID " + id + " (" + name + ") is missing a description.");
        }
        String description = xml.getDescription().trim();

        // 4. Validate Commission
        if (xml.getCommission() == null || xml.getCommission().getValue() == null) {
            throw new XmlValidationException("Event ID " + id + ": Missing required <comision> tag.");
        }
        int commission = xml.getCommission().getValue();
        if (commission < 0 || commission > 90) {
            throw new XmlValidationException("Event ID " + id + ": Commission must be between 0 and 90. Got: " + commission);
        }

        CommissionType commissionType;
        try {
            commissionType = CommissionType.fromXmlString(xml.getCommission().getType());
        } catch (IllegalArgumentException e) {
            throw new XmlValidationException("Event ID " + id + ": " + e.getMessage());
        }

        // 5. Validate Options
        if (xml.getOptions() == null || xml.getOptions().size() < 2) {
            throw new XmlValidationException("Event ID " + id + ": Must contain at least 2 options (<GM-option>).");
        }

        List<Option> options = new ArrayList<>();
        for (String optName : xml.getOptions()) {
            if (optName == null || optName.isBlank()) {
                throw new XmlValidationException("Event ID " + id + ": Option name cannot be empty.");
            }
            options.add(new Option(optName.trim()));
        }

        // 6. Validate LMSR Liquidity Parameter 'b'
        if (xml.getMethod() == null || xml.getMethod().getLmsr() == null || xml.getMethod().getLmsr().getB() == null) {
            throw new XmlValidationException("Event ID " + id + ": Missing LMSR method configuration (<GM-method>/<GM-LMSR>/<b>).");
        }
        int b = xml.getMethod().getLmsr().getB();
        if (b <= 0) {
            throw new XmlValidationException("Event ID " + id + ": LMSR parameter 'b' must be strictly positive (> 0). Got: " + b);
        }

        // 7. Validate Order Book initial and d
        if (xml.getMethod() == null || xml.getMethod().getOrderBook() == null
                || xml.getMethod().getOrderBook().getD() == null || xml.getMethod().getOrderBook().getInitial() == null) {
            throw new XmlValidationException("Event ID " + id + ": Missing Order Book method configuration (<GM-method>/<GM-order-book>/<d>/<initial>).");
        }
        int d = xml.getMethod().getOrderBook().getD();
        if (d <= 0) {
            throw new XmlValidationException("Event ID " + id + ": Order Book parameter 'd' must be strictly positive (> 0). Got: " + d);
        }

        int initial = xml.getMethod().getOrderBook().getInitial();
        if (initial<0){
           throw new XmlValidationException("Event ID " + id + ": Order Book paramater 'initial' must be positive (>=0). Got: " + initial);
        }

        return new Event(id, name, description, commission, commissionType, options, b);
    }
}