package com.guessmarket.engine.xml;

import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.User;

import java.util.Map;

/** The validated result of reading a market XML file: its events and its users. */
public record ParsedMarket(Map<Integer, Event> events, Map<String, User> users) {}
