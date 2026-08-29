package com.guessmarket.engine.xml.jaxb;

import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;

import java.util.*;

public class ParsedXmlWrapper {
    Map<Integer, Event> parsedEvents;
    Map<String, User> users;

    public ParsedXmlWrapper(Map<Integer, Event> parsedEvents, Map<String, User> users) {
        this.parsedEvents = parsedEvents;
        this.users = users;
    }

    public Map<Integer, Event> getParsedEvents() {
        return parsedEvents;
    }

    public Map<String, User> getUsers() {
        return users;
    }
}
