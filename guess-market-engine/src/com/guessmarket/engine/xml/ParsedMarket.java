package com.guessmarket.engine.xml;

import com.guessmarket.engine.model.Event;

import java.util.Map;

/**
 * The validated result of reading an events XML file. The schema no longer
 * carries users - they register via {@code MarketEngine.login()} instead.
 */
public record ParsedMarket(Map<String, Event> events) {}
