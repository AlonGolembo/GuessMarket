package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.xml.jaxb.GuessMarketXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.transform.stream.StreamSource;
import java.io.File;

/**
 * Turns a market XML file into a raw {@link GuessMarketXml} object graph. Does
 * the file-level checks (exists, {@code .xml}) and the JAXB unmarshal; does no
 * semantic validation - that is {@link MarketAssembler}'s job.
 */
final class XmlMarketReader {

    private XmlMarketReader() {}

    static GuessMarketXml read(String filePath) throws XmlValidationException {
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
            Unmarshaller unmarshaller = JAXBContext.newInstance(GuessMarketXml.class).createUnmarshaller();
            GuessMarketXml root = unmarshaller
                    .unmarshal(new StreamSource(file), GuessMarketXml.class)
                    .getValue();
            if (root == null) {
                throw new XmlValidationException("File is not a valid Guess Market XML document.");
            }
            return root;
        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException("Could not read XML file: " + e.getMessage(), e);
        }
    }
}
