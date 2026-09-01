package com.guessmarket.engine.xml;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.LmsrMethod;
import com.guessmarket.engine.model.Option;
import com.guessmarket.engine.model.OrderBookMethod;
import com.guessmarket.engine.model.TradingMethod;
import com.guessmarket.engine.xml.jaxb.EventXml;
import com.guessmarket.engine.xml.jaxb.LmsrXml;
import com.guessmarket.engine.xml.jaxb.OrderBookXml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Validates one {@code <GM-event>} and builds the domain {@link Event}. */
final class EventXmlValidator {

    private EventXmlValidator() {}

    static Event validate(EventXml xml, Set<Integer> usedIds) throws XmlValidationException {
        String name = requireText(xml.getName(), "Event is missing its 'name' attribute.");

        if (xml.getId() == null) {
            throw new XmlValidationException("Event '" + name + "' is missing its <id>.");
        }
        int id = xml.getId();
        if (!usedIds.add(id)) {
            throw new XmlValidationException("Duplicate event id: " + id + " (ids must be unique).");
        }

        String description = requireText(xml.getDescription(),
                "Event ID " + id + " (" + name + ") is missing a <description>.");

        Commission commission = validateCommission(xml, id);
        List<Option> options = validateOptions(xml, id);
        TradingMethod method = validateMethod(xml, id);

        return new Event(id, name, description,
                commission.percentage(), commission.type(), options, method);
    }

    private record Commission(int percentage, CommissionType type) {}

    private static Commission validateCommission(EventXml xml, int id) throws XmlValidationException {
        if (xml.getCommission() == null || xml.getCommission().getValue() == null) {
            throw new XmlValidationException("Event ID " + id + ": missing <commission>.");
        }
        int percentage = xml.getCommission().getValue();
        if (percentage < 0 || percentage > 90) {
            throw new XmlValidationException(
                    "Event ID " + id + ": commission must be between 0 and 90, got: " + percentage);
        }
        CommissionType type;
        try {
            type = CommissionType.fromXmlString(xml.getCommission().getType());
        } catch (IllegalArgumentException e) {
            throw new XmlValidationException("Event ID " + id + ": " + e.getMessage());
        }
        return new Commission(percentage, type);
    }

    private static List<Option> validateOptions(EventXml xml, int id) throws XmlValidationException {
        if (xml.getOptions() == null || xml.getOptions().size() != 2) {
            throw new XmlValidationException("Event ID " + id + ": must contain exactly 2 <GM-option> entries.");
        }
        List<Option> options = new ArrayList<>();
        for (String optionName : xml.getOptions()) {
            if (optionName == null || optionName.isBlank()) {
                throw new XmlValidationException("Event ID " + id + ": option name cannot be empty.");
            }
            options.add(new Option(optionName.trim()));
        }
        return options;
    }

    private static TradingMethod validateMethod(EventXml xml, int id) throws XmlValidationException {
        if (xml.getMethod() == null) {
            throw new XmlValidationException("Event ID " + id + ": missing <GM-method>.");
        }
        TradingMethod method = switch (xml.getMethod().getType()) {
            case LMSR -> {
                LmsrXml lmsr = xml.getMethod().getLmsr();
                if (lmsr.getB() == null) {
                    throw new XmlValidationException("Event ID " + id + ": missing LMSR <b>.");
                }
                yield new LmsrMethod(lmsr.getB());
            }
            case ORDERBOOK -> {
                OrderBookXml ob = xml.getMethod().getOrderBook();
                if (ob.getD() == null || ob.getInitial() == null) {
                    throw new XmlValidationException("Event ID " + id + ": missing Order Book <d> / <initial>.");
                }
                yield new OrderBookMethod(ob.getD(), ob.getInitial(), ob.isAllowMint());
            }
            case NONE -> throw new XmlValidationException(
                    "Event ID " + id + ": <GM-method> must contain <GM-LMSR> or <GM-order-book>.");
            case NOTDEFINED -> throw new XmlValidationException(
                    "Event ID " + id + ": <GM-method> defines more than one trading method.");
        };
        try {
            method.validate();
        } catch (XmlValidationException e) {
            throw new XmlValidationException("Event ID " + id + ": " + e.getMessage());
        }
        return method;
    }

    private static String requireText(String value, String message) throws XmlValidationException {
        if (value == null || value.isBlank()) {
            throw new XmlValidationException(message);
        }
        return value.trim();
    }
}
