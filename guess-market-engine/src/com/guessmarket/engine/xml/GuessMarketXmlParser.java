package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;

/**
 * Entry point for loading a market XML file. Thin: it wires
 * {@link XmlMarketReader} (file + JAXB) to {@link MarketAssembler}
 * (validation + domain construction).
 */
public final class GuessMarketXmlParser {

    private GuessMarketXmlParser() {}

    public static ParsedMarket parseAndValidateXml(String filePath) throws XmlValidationException {
        return MarketAssembler.assemble(XmlMarketReader.read(filePath));
    }
}
