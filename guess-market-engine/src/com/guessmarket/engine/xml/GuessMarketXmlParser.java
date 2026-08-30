package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.*;
import com.guessmarket.engine.xml.jaxb.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import javax.xml.transform.stream.StreamSource;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuessMarketXmlParser {

    @Contract("null -> fail")
    public static @NotNull ParsedXmlWrapper parseAndValidateXml(String filePath) throws XmlValidationException {
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
                // The Event constructor seeds its pool from tradingMethod.getInitialSubsidy().
                Event event = validateAndConvertEvent(eventXml, usedIds);
                parsedEvents.put(event.getId(), event);
            }

            // 4. Validate users
            Map<String, User> parsedUsers = new HashMap<>();
            Set<String> usedNames = new HashSet<>();
            for (UserXml userXml : root.getUsers()) {
                User user = validateAndConvertUser(userXml, usedNames);
                parsedUsers.put(user.getName(), user);
            }

            // 5. Validate market maker to event mapping
            validateMarketMakerToEventMapping(parsedEvents, parsedUsers);

            return new  ParsedXmlWrapper(parsedEvents, parsedUsers);

        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException("Failed to parse XML file: " + e.getMessage(), e);
        }
    }

    private static void validateMarketMakerToEventMapping(Map<Integer, Event> parsedEvents, Map<String, User> parsedUsers)
            throws XmlValidationException {
        // Validate Market Makers do not reference to an event that doesn't exist
        for (User user : parsedUsers.values()) {
            user.getMarketMakerEventIds().stream()
                    .filter(eventId -> !parsedEvents.containsKey(eventId))
                    .findFirst()
                    .ifPresent(invalidId -> {
                        throw new XmlValidationException("User '" + user.getName() +
                                "' is configured as a market maker for non-existent event ID: " + invalidId);
                    });
        }

        // Validate every event has exactly one MM
        Map<Integer, Long> mmCountPerEvent = parsedUsers.values()
                .stream()
                .flatMap(user -> user.getMarketMakerEventIds().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 2. Validate each parsed event
        for (Map.Entry<Integer, Event> entry : parsedEvents.entrySet()) {
            Integer eventId = entry.getKey();
            Event event = entry.getValue();
            long count = mmCountPerEvent.getOrDefault(eventId, 0L);

            if (count == 0) {
                throw new XmlValidationException(
                        "Event ID " + eventId + " ('" + event.getName() + "') has no assigned market maker."
                );
            } else if (count > 1) {
                throw new XmlValidationException(
                        "Event ID " + eventId + " ('" + event.getName() + "') has multiple market makers (" + count + " users assigned)."
                );
            }
        }
    }

    private static User validateAndConvertUser(UserXml userXml, Set<String> usedNames) throws XmlValidationException {

        // Validate name
        if(userXml.getName() == null || userXml.getName().isBlank()) {
            throw new XmlValidationException("User is missing a valid 'name' attrivute");
        }

        String name = userXml.getName();
        if(usedNames.contains(name)) {
            throw new XmlValidationException("Duplicate user name found: " + name + " is already in use.");
        }

        // Validate initial cash
        if(userXml.getInitialCash() == null || userXml.getInitialCash() < 0) {
            throw new XmlValidationException("Initial cash amount is missing or invalid.");
        }

        return new User(userXml.getName(), userXml.getInitialCash(), userXml.getMarketMakerEvents());
    }

    private static Event validateAndConvertEvent(EventXml xml, Set<Integer> usedIds) throws XmlValidationException {
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

        // Validate Options
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

        // Validate Method
        if(xml.getMethod() == null){
            throw new XmlValidationException("Event ID " + id + ": Missing <GM-method> element.");
        }

        TradingMethodType tradingMethodType = xml.getMethod().getType();
        ITradingMethod tradingMethod = null;

        if(tradingMethodType == TradingMethodType.NOTDEFINED){
            throw new XmlValidationException("Event ID " + id + ": More than one trading method is defined.");
        }

        // Validate LMSR Liquidity Parameter 'b'
        if (tradingMethodType == TradingMethodType.LMSR){
            if (xml.getMethod().getLmsr().getB() == null) {
                throw new XmlValidationException("Event ID " + id + ": Missing LMSR method parameter b configuration.");
            }
            int b = xml.getMethod().getLmsr().getB();
            if (b <= 0) {
                throw new XmlValidationException("Event ID " + id + ": LMSR parameter 'b' must be strictly positive (> 0). Got: " + b);
            }
        }

        // Validate Order Book initial and d
        if (tradingMethodType == TradingMethodType.ORDERBOOK){
            if (xml.getMethod().getOrderBook().getD() == null || xml.getMethod().getOrderBook().getInitial() == null) {
                throw new XmlValidationException("Event ID " + id + ": Missing Order Book method configuration <d> / <initial>.");
            }
            int d = xml.getMethod().getOrderBook().getD();
            if (d <= 0) {
                throw new XmlValidationException("Event ID " + id + ": Order Book parameter 'd' must be strictly positive (> 0). Got: " + d);
            }
            int initial = xml.getMethod().getOrderBook().getInitial();
            if (initial < 0){
                throw new XmlValidationException("Event ID " + id + ": Order Book paramater 'initial' must be positive (>=0). Got: " + initial);
            }
        }

        switch (tradingMethodType){
            case LMSR -> tradingMethod = new LmsrMethod(xml.getMethod().getLmsr().getB());
            case ORDERBOOK-> tradingMethod = new OrderBookMethod();
        }

        return new Event(id, name, description, commission, commissionType, options, tradingMethod);
    }
}