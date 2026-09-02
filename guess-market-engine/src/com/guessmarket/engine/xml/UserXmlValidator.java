package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.model.User;
import com.guessmarket.engine.xml.jaxb.UserXml;

import java.util.Set;

/** Validates one {@code <GM-user>} and builds the domain {@link User}. */
final class UserXmlValidator {

    private UserXmlValidator() {}

    static User validate(UserXml xml, Set<String> usedNames) throws XmlValidationException {
        if (xml.getName() == null || xml.getName().isBlank()) {
            throw new XmlValidationException("User is missing its 'name' attribute.");
        }
        String name = xml.getName().trim();
        if (!usedNames.add(name)) {
            throw new XmlValidationException("Duplicate user name: " + name + ".");
        }
        if (xml.getInitialCash() == null || xml.getInitialCash() < 0) {
            throw new XmlValidationException("User '" + name + "': <initial-cash> is missing or negative.");
        }
        return new User(name, xml.getInitialCash(), xml.getMarketMakerEvents());
    }
}
