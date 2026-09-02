/**
 * Loads a market XML file into validated domain objects.
 * {@link com.guessmarket.engine.xml.GuessMarketXmlParser} is the entry point; it
 * wires {@link com.guessmarket.engine.xml.XmlMarketReader} (file + JAXB) to
 * {@link com.guessmarket.engine.xml.MarketAssembler}, which delegates field
 * checks to the {@code *Validator} classes and returns a
 * {@link com.guessmarket.engine.xml.ParsedMarket}.
 */
package com.guessmarket.engine.xml;
