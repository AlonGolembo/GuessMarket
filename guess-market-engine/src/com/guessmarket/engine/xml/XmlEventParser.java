package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.CommissionType;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.Option;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XmlEventParser {

    public static List<Event> parseAndValidateXml(String filePath) throws XmlValidationException {
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
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Prevent XML External Entity (XXE) vulnerabilities
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            if (!"Guess-Market".equalsIgnoreCase(root.getNodeName())) {
                throw new XmlValidationException("Invalid root element. Expected <Guess-Market>, got <" + root.getNodeName() + ">");
            }

            NodeList eventsList = doc.getElementsByTagName("GM-event");
            if (eventsList.getLength() == 0) {
                throw new XmlValidationException("XML file contains no events (<GM-event> tags).");
            }

            List<Event> parsedEvents = new ArrayList<>();
            Set<Integer> usedIds = new HashSet<>();

            for (int i = 0; i < eventsList.getLength(); i++) {
                Element eventElement = (Element) eventsList.item(i);
                Event event = parseSingleEvent(eventElement, usedIds);
                parsedEvents.add(event);
            }

            return parsedEvents;

        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException("Failed to parse XML file: " + e.getMessage(), e);
        }
    }

    private static Event parseSingleEvent(Element element, Set<Integer> usedIds) throws XmlValidationException {
        // 1. Name attribute
        String name = element.getAttribute("name");
        if (name == null || name.isBlank()) {
            throw new XmlValidationException("Event is missing a valid 'name' attribute.");
        }
        name = name.trim();

        // 2. Event ID
        int id = parseIntegerTag(element, "id", "Event ID");
        if (usedIds.contains(id)) {
            throw new XmlValidationException("Duplicate Event ID found: " + id + ". Event IDs must be unique.");
        }
        usedIds.add(id);

        // 3. Description
        String description = getTagContent(element, "description");
        if (description == null || description.isBlank()) {
            throw new XmlValidationException("Event ID " + id + " (" + name + ") is missing a description.");
        }

        // 4. Commission & Commission Type
        Element commissionElement = getSingleElementByTagName(element, "comision");
        int commission = parseInteger(commissionElement.getTextContent(), "Commission percentage for event ID " + id);
        if (commission < 0 || commission > 90) {
            throw new XmlValidationException("Event ID " + id + ": Commission must be between 0 and 90. Got: " + commission);
        }

        String commissionTypeAttr = commissionElement.getAttribute("type");
        CommissionType commissionType;
        try {
            commissionType = CommissionType.fromXmlString(commissionTypeAttr);
        } catch (IllegalArgumentException e) {
            throw new XmlValidationException("Event ID " + id + ": " + e.getMessage());
        }

        // 5. Options
        NodeList optionNodes = element.getElementsByTagName("GM-option");
        if (optionNodes.getLength() < 2) {
            throw new XmlValidationException("Event ID " + id + ": Must contain at least 2 options (<GM-option>).");
        }

        List<Option> options = new ArrayList<>();
        for (int i = 0; i < optionNodes.getLength(); i++) {
            String optName = optionNodes.item(i).getTextContent();
            if (optName == null || optName.isBlank()) {
                throw new XmlValidationException("Event ID " + id + ": Option name cannot be empty.");
            }
            options.add(new Option(optName.trim()));
        }

        // 6. LMSR Liquidity Parameter (b)
        Element methodElement = getSingleElementByTagName(element, "GM-method");
        Element lmsrElement = getSingleElementByTagName(methodElement, "GM-LMSR");
        int b = parseIntegerTag(lmsrElement, "b", "LMSR liquidity parameter 'b' for event ID " + id);
        if (b <= 0) {
            throw new XmlValidationException("Event ID " + id + ": LMSR parameter 'b' must be strictly positive (> 0). Got: " + b);
        }

        return new Event(id, name, description, commission, commissionType, options, b);
    }

    // --- Helper Parsing Utilities ---

    private static String getTagContent(Element parent, String tagName) throws XmlValidationException {
        Element el = getSingleElementByTagName(parent, tagName);
        return el.getTextContent() != null ? el.getTextContent().trim() : "";
    }

    private static int parseIntegerTag(Element parent, String tagName, String fieldName) throws XmlValidationException {
        String content = getTagContent(parent, tagName);
        return parseInteger(content, fieldName);
    }

    private static int parseInteger(String value, String fieldName) throws XmlValidationException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new XmlValidationException(fieldName + " must be a valid integer. Got: '" + value + "'");
        }
    }

    private static Element getSingleElementByTagName(Element parent, String tagName) throws XmlValidationException {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            throw new XmlValidationException("Missing required XML tag: <" + tagName + "> inside <" + parent.getNodeName() + ">.");
        }
        return (Element) list.item(0);
    }
}