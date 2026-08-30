package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.MarketException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketEngineImplTest {

    private static final String XML = """
        <Guess-Market>
          <GM-events>
            <GM-event name="Coin flip">
              <id>1</id><description>heads?</description>
              <commission type="on-purchase">10</commission>
              <GM-options><GM-option>Heads</GM-option><GM-option>Tails</GM-option></GM-options>
              <GM-method><GM-LMSR><b>100</b></GM-LMSR></GM-method>
            </GM-event>
          </GM-events>
          <GM-users>
            <GM-user name="mm"><initial-cash>1000</initial-cash>
              <GM-market-maker><event id="1"/></GM-market-maker></GM-user>
            <GM-user name="trader"><initial-cash>500</initial-cash></GM-user>
          </GM-users>
        </Guess-Market>
        """;

    @TempDir
    Path dir;
    private MarketEngine engine;
    private String xmlPath;

    @BeforeEach
    void setUp() throws IOException {
        xmlPath = dir.resolve("market.xml").toString();
        Files.writeString(Path.of(xmlPath), XML);
        engine = new MarketEngineImpl();
        engine.loadXmlFile(xmlPath);
    }

    private EventDTO event() {
        return engine.getAllEvents().get(0);
    }

    private UserDTO user(String name) {
        return engine.getAllUsers().get(name);
    }

    @Test
    void loadExposesEventsAndUsers() {
        assertEquals(1, engine.getNumOfLoadedEvents());
        assertEquals("NOT_ACTIVE", event().status());
        assertTrue(engine.getAllUsers().keySet().containsAll(java.util.Set.of("mm", "trader")));
    }

    @Test
    void tradingIsRejectedUntilTheEventIsActivated() {
        assertThrows(MarketException.class, () -> engine.buyShares(user("trader"), event(), 1, 10));
    }

    @Test
    void quoteEqualsTheAmountCharged() {
        engine.activateEvent(event(), user("mm"));
        TradeQuoteDTO quote = engine.quoteTrade(user("trader"), event(), 1, 30);
        TradeResultDTO result = engine.buyShares(user("trader"), event(), 1, 30);
        assertEquals(quote.total(), result.totalPaid(), 1e-9);
    }

    @Test
    void getActiveEventsReflectsActivation() {
        assertTrue(engine.getActiveEvents().isEmpty());
        engine.activateEvent(event(), user("mm"));
        assertEquals(1, engine.getActiveEvents().size());
    }

    @Test
    void stateRoundTripRestoresEventsUsersAndBalances() throws MarketException {
        engine.activateEvent(event(), user("mm"));
        engine.buyShares(user("trader"), event(), 1, 20);
        double traderBalance = user("trader").balance();

        String statePath = dir.resolve("state.bin").toString();
        engine.saveState(statePath);

        MarketEngine restored = new MarketEngineImpl();
        restored.loadState(statePath);

        assertEquals(1, restored.getNumOfLoadedEvents());
        assertEquals(java.util.Set.of("mm", "trader"), restored.getAllUsers().keySet());
        assertEquals(traderBalance, restored.getAllUsers().get("trader").balance(), 1e-9);
        assertEquals(1, restored.getEventDetails(1).tradeHistory().size());
    }

    @Test
    void closeEventDeclaresTheWinnerAndPaysHolders() {
        engine.activateEvent(event(), user("mm"));
        engine.buyShares(user("trader"), event(), 1, 20);   // 20 Heads
        double afterBuy = user("trader").balance();

        engine.closeEvent(1, 1);

        assertEquals("Heads", engine.getEventDetails(1).winningOption());
        assertEquals("CLOSED", engine.getEventDetails(1).eventInfo().status());
        assertEquals(afterBuy + 20.0, user("trader").balance(), 1e-9);   // on-purchase: full $1/share
    }
}
