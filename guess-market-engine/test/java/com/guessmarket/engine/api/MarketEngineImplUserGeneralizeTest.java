package com.guessmarket.engine.api;

import com.guessmarket.dto.LedgerEntryDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.MarketException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MarketEngine} through {@code login}/{@code deposit} and the cumulative,
 * uploader-attributed {@code loadXmlFile}: users no longer come from the XML
 * file, and every upload adds to the catalog rather than replacing it.
 */
class MarketEngineImplUserGeneralizeTest {

    private static final String COIN_FLIP_XML = """
        <Guess-Market>
          <GM-events>
            <GM-event name="Coin flip">
              <id>1</id><description>heads?</description>
              <commission type="on-purchase">10</commission>
              <GM-options><GM-option>Heads</GM-option><GM-option>Tails</GM-option></GM-options>
              <GM-method><GM-LMSR><b>100</b></GM-LMSR></GM-method>
            </GM-event>
          </GM-events>
        </Guess-Market>
        """;

    private static String eventXml(String name) {
        return """
            <Guess-Market>
              <GM-events>
                <GM-event name="%s">
                  <id>1</id><description>d</description>
                  <commission type="on-purchase">5</commission>
                  <GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>
                  <GM-method><GM-LMSR><b>50</b></GM-LMSR></GM-method>
                </GM-event>
              </GM-events>
            </Guess-Market>
            """.formatted(name);
    }

    @TempDir
    Path dir;
    private MarketEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MarketEngineImpl();
    }

    private String write(String fileName, String xml) throws IOException {
        Path p = dir.resolve(fileName);
        Files.writeString(p, xml);
        return p.toString();
    }

    // --- login -------------------------------------------------------------

    @Test
    void loginRegistersAUserStartingAtZeroBalance() {
        UserDTO user = engine.login("alice");

        assertEquals("alice", user.name());
        assertEquals(0.0, user.balance(), 1e-9);
        assertTrue(engine.getAllUsers().containsKey("alice"));
    }

    @Test
    void loginRejectsABlankName() {
        assertThrows(MarketException.class, () -> engine.login(""));
        assertThrows(MarketException.class, () -> engine.login("   "));
    }

    @Test
    void loginRejectsANameAlreadyTakenCaseInsensitively() {
        engine.login("alice");

        assertThrows(MarketException.class, () -> engine.login("alice"));
        assertThrows(MarketException.class, () -> engine.login("ALICE"));
    }

    // --- deposit -------------------------------------------------------------

    @Test
    void depositAddsFundsAndAppearsInTheLedgerAsANonEventEntry() {
        engine.login("alice");

        engine.deposit("alice", 250.0);

        assertEquals(250.0, engine.getAllUsers().get("alice").balance(), 1e-9);
        LedgerEntryDTO entry = engine.getUserDetails("alice").balanceHistory().get(0);
        assertEquals("DEPOSIT", entry.type());
        assertEquals(250.0, entry.delta(), 1e-9);
        assertNull(entry.eventName());
    }

    @Test
    void depositRejectsAnUnknownUser() {
        assertThrows(MarketException.class, () -> engine.deposit("nobody", 100.0));
    }

    // --- loadXmlFile: uploader identity --------------------------------------

    @Test
    void loadXmlFileRequiresTheUploaderToBeALoggedInUser() throws IOException {
        String path = write("market.xml", COIN_FLIP_XML);

        assertThrows(MarketException.class, () -> engine.loadXmlFile(path, "nobody"));
        assertEquals(0, engine.getAllEvents().size());
    }

    @Test
    void loadXmlFileAssignsTheUploaderAsMarketMakerOfEveryEventInTheFile() throws IOException {
        engine.login("uploader");
        String path = write("market.xml", COIN_FLIP_XML);

        engine.loadXmlFile(path, "uploader");

        assertTrue(engine.getAllUsers().get("uploader").marketMakerEventNames().contains("Coin flip"));
    }

    // --- uploadEventsXml: same rules, but content already in memory ----------

    @Test
    void uploadEventsXmlAddsEventsFromInMemoryContentJustLikeLoadXmlFile() {
        engine.login("uploader");

        var addedNames = engine.uploadEventsXml("uploader", COIN_FLIP_XML);

        assertEquals(java.util.List.of("Coin flip"), addedNames);
        assertEquals(1, engine.getAllEvents().size());
        assertTrue(engine.getAllUsers().get("uploader").marketMakerEventNames().contains("Coin flip"));
    }

    @Test
    void uploadEventsXmlAlsoRejectsACollidingEventAndAddsNothing() throws IOException {
        engine.login("uploader");
        engine.loadXmlFile(write("first.xml", COIN_FLIP_XML), "uploader");

        assertThrows(MarketException.class, () -> engine.uploadEventsXml("uploader", COIN_FLIP_XML));

        assertEquals(1, engine.getAllEvents().size());
    }

    // --- loadXmlFile: accumulation, not replacement --------------------------

    @Test
    void loadXmlFileAccumulatesAcrossUploadsWithoutTouchingExistingUsersOrEvents() throws IOException {
        engine.login("uploader1");
        engine.deposit("uploader1", 1000);
        engine.login("uploader2");
        engine.loadXmlFile(write("first.xml", COIN_FLIP_XML), "uploader1");

        engine.loadXmlFile(write("second.xml", eventXml("Second event")), "uploader2");

        assertEquals(2, engine.getAllEvents().size());
        assertEquals(1000.0, engine.getAllUsers().get("uploader1").balance(), 1e-9);   // untouched by the second upload
        assertTrue(engine.getAllUsers().get("uploader1").marketMakerEventNames().contains("Coin flip"));
        assertTrue(engine.getAllUsers().get("uploader2").marketMakerEventNames().contains("Second event"));
    }

    // --- loadXmlFile: duplicate event name across files -----------------------

    @Test
    void loadXmlFileRejectsAFileThatCollidesWithAnExistingEventNameCaseInsensitivelyAndAddsNothing()
            throws IOException {
        engine.login("uploader");
        engine.loadXmlFile(write("first.xml", COIN_FLIP_XML), "uploader");

        String collidingPath = write("second.xml", eventXml("COIN FLIP"));   // same name, different case
        MarketException ex = assertThrows(MarketException.class,
                () -> engine.loadXmlFile(collidingPath, "uploader"));
        assertTrue(ex.getMessage().contains("COIN FLIP"));

        assertEquals(1, engine.getAllEvents().size());   // nothing from the rejected file was added
    }

    @Test
    void loadXmlFileRejectsTheWholeFileIfAnyOneEventInItCollides() throws IOException {
        engine.login("uploader");
        engine.loadXmlFile(write("first.xml", COIN_FLIP_XML), "uploader");

        String twoEventsPath = write("second.xml", """
            <Guess-Market>
              <GM-events>
                <GM-event name="Brand new event">
                  <id>1</id><description>d</description>
                  <commission type="on-purchase">5</commission>
                  <GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>
                  <GM-method><GM-LMSR><b>50</b></GM-LMSR></GM-method>
                </GM-event>
                <GM-event name="Coin flip">
                  <id>2</id><description>d</description>
                  <commission type="on-purchase">5</commission>
                  <GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>
                  <GM-method><GM-LMSR><b>50</b></GM-LMSR></GM-method>
                </GM-event>
              </GM-events>
            </Guess-Market>
            """);

        assertThrows(MarketException.class, () -> engine.loadXmlFile(twoEventsPath, "uploader"));

        // "Brand new event" must not have been added either - the file is rejected as a whole.
        assertEquals(1, engine.getAllEvents().size());
    }
}
