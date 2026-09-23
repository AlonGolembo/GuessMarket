package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.xml.jaxb.GuessMarketXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;

/**
 * Turns a market XML file - or, for a server receiving an upload, XML content
 * already in memory - into a raw {@link GuessMarketXml} object graph. Does the
 * file-level checks (exists, {@code .xml}) and the JAXB unmarshal; does no
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
        return unmarshal(new StreamSource(file), "file");
    }

    /** Parses XML already held in memory - e.g. an upload the caller must not write to disk. */
    static GuessMarketXml readContent(String xmlContent) throws XmlValidationException {
        if (xmlContent == null || xmlContent.isBlank()) {
            throw new XmlValidationException("XML content cannot be empty.");
        }
        return unmarshal(new StreamSource(new StringReader(xmlContent)), "uploaded content");
    }

    private static GuessMarketXml unmarshal(StreamSource source, String errorContext) throws XmlValidationException {
        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(GuessMarketXml.class).createUnmarshaller();
            GuessMarketXml root = unmarshaller.unmarshal(source, GuessMarketXml.class).getValue();
            if (root == null) {
                throw new XmlValidationException("The " + errorContext + " is not a valid Guess Market XML document.");
            }
            return root;
        } catch (XmlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException("Could not read the " + errorContext + ": " + e.getMessage(), e);
        }
    }
}
