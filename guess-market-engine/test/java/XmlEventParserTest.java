package java;

import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.xml.XmlEventParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlEventParserTest {

    @Test
    @DisplayName("Valid XML should parse successfully")
    void testParseValidXml(@TempDir Path tempDir) throws IOException, MarketException {
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Guess-Market xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="GM-EX1-schema.xsd">
            	<GM-events>
            		<GM-event name="Test Event">
            			<id>1</id>
            			<description>Test Description</description>
            			<comision type="on-purchase">5</comision>
            			<GM-options>
            				<GM-option>Yes</GM-option>
            				<GM-option>No</GM-option>
            			</GM-options>
            			<GM-method>
            				<GM-LMSR>
            					<b>100</b>
            				</GM-LMSR>
            			</GM-method>
            		</GM-event>
            	</GM-events>
            </Guess-Market>
            """;

        File xmlFile = tempDir.resolve("test.xml").toFile();
        Files.writeString(xmlFile.toPath(), xmlContent);

        Map<Integer, Event> events = XmlEventParser.parseAndValidateXml(xmlFile.getAbsolutePath());

        assertNotNull(events);
        assertEquals(1, events.size());
        assertTrue(events.containsKey(1));
        assertEquals("Test Event", events.get(1).getName());
    }

    @Test
    @DisplayName("Non-existent file path should throw Exception")
    void testFileNotFound() {
        assertThrows(Exception.class, () ->
                XmlEventParser.parseAndValidateXml("non_existent_file.xml")
        );
    }
}