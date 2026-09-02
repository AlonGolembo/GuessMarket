package com.guessmarket.engine.xml;

import com.guessmarket.engine.exception.XmlValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuessMarketXmlParserTest {

    @TempDir
    Path dir;

    private String write(String name, String xml) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, xml);
        return file.toString();
    }

    private static String event(String methodXml) {
        return """
            <Guess-Market>
              <GM-events>
                <GM-event name="Coin flip">
                  <id>1</id>
                  <description>heads?</description>
                  <commission type="on-purchase">5</commission>
                  <GM-options><GM-option>Heads</GM-option><GM-option>Tails</GM-option></GM-options>
                  <GM-method>%s</GM-method>
                </GM-event>
              </GM-events>
              <GM-users>
                <GM-user name="mm">
                  <initial-cash>1000</initial-cash>
                  <GM-market-maker><event id="1"/></GM-market-maker>
                </GM-user>
              </GM-users>
            </Guess-Market>
            """.formatted(methodXml);
    }

    @Test
    void parsesAValidFile() throws IOException {
        ParsedMarket market = GuessMarketXmlParser.parseAndValidateXml(
                write("ok.xml", event("<GM-LMSR><b>100</b></GM-LMSR>")));

        assertEquals(1, market.events().size());
        assertEquals("Coin flip", market.events().get(1).getName());
        assertTrue(market.users().containsKey("mm"));
        assertTrue(market.users().get("mm").isMarketMakerFor(1));
    }

    @Test
    void rejectsTwoTradingMethodsInOneEvent() throws IOException {
        String path = write("two.xml",
                event("<GM-LMSR><b>100</b></GM-LMSR><GM-order-book d=\"1\" initial=\"0\"/>"));
        XmlValidationException ex = assertThrows(XmlValidationException.class,
                () -> GuessMarketXmlParser.parseAndValidateXml(path));
        assertTrue(ex.getMessage().toLowerCase().contains("more than one"));
    }

    @Test
    void rejectsNonPositiveLiquidityParameter() throws IOException {
        String path = write("b.xml", event("<GM-LMSR><b>0</b></GM-LMSR>"));
        assertThrows(XmlValidationException.class,
                () -> GuessMarketXmlParser.parseAndValidateXml(path));
    }

    @Test
    void rejectsEventWithNoMarketMaker() throws IOException {
        String xml = """
            <Guess-Market>
              <GM-events>
                <GM-event name="E"><id>1</id><description>d</description>
                  <commission type="on-close">10</commission>
                  <GM-options><GM-option>A</GM-option><GM-option>B</GM-option></GM-options>
                  <GM-method><GM-LMSR><b>50</b></GM-LMSR></GM-method>
                </GM-event>
              </GM-events>
              <GM-users><GM-user name="u"><initial-cash>10</initial-cash></GM-user></GM-users>
            </Guess-Market>
            """;
        XmlValidationException ex = assertThrows(XmlValidationException.class,
                () -> GuessMarketXmlParser.parseAndValidateXml(write("nomm.xml", xml)));
        assertTrue(ex.getMessage().contains("market maker"));
    }

    @Test
    void rejectsAMissingFile() {
        assertThrows(XmlValidationException.class,
                () -> GuessMarketXmlParser.parseAndValidateXml(dir.resolve("nope.xml").toString()));
    }
}
