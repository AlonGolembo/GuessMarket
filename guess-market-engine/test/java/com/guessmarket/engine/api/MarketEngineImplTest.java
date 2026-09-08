package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.HoldingDTO;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.LedgerEntryDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.exception.MarketException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(EventStatus.NOT_ACTIVE, event().status());
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
    void holdingsAreExposedPerUserAndPerEvent() {
        engine.activateEvent(event(), user("mm"));
        engine.buyShares(user("trader"), event(), 1, 20);   // 20 Heads
        engine.buyShares(user("trader"), event(), 2, 5);    // 5 Tails

        // per user
        assertEquals(Map.of("Heads", 20, "Tails", 5),
                engine.getAllUsers().get("trader").holdings().get(1));

        // per event
        assertEquals(
                java.util.Set.of(new HoldingDTO("trader", "Heads", 20), new HoldingDTO("trader", "Tails", 5)),
                java.util.Set.copyOf(engine.getEventDetails(1).participantHoldings()));
    }

    @Test
    void stateRoundTripRestoresEventsUsersBalancesAndHoldings() throws MarketException {
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
        assertEquals(Map.of("Heads", 20, "Tails", 0),
                restored.getAllUsers().get("trader").holdings().get(1));
    }

    @Test
    void getUserDetailsReturnsTheBalanceLedgerOldestFirst() {
        UserDetailsDTO fresh = engine.getUserDetails("trader");
        assertEquals("trader", fresh.userInfo().name());
        assertEquals(1, fresh.balanceHistory().size());
        LedgerEntryDTO initial = fresh.balanceHistory().get(0);
        assertEquals("INITIAL", initial.type());
        assertEquals(500.0, initial.balanceAfter(), 1e-9);
        assertNull(initial.eventId());

        engine.activateEvent(event(), user("mm"));
        engine.buyShares(user("trader"), event(), 1, 20);   // 20 Heads
        engine.closeEvent(1, 1);                            // Heads wins -> trader paid out

        UserDetailsDTO details = engine.getUserDetails("trader");
        var history = details.balanceHistory();
        assertEquals(3, history.size());

        assertEquals("PURCHASE", history.get(1).type());
        assertTrue(history.get(1).delta() < 0);
        assertEquals(1, history.get(1).eventId());

        LedgerEntryDTO payout = history.get(2);
        assertEquals("PAYOUT", payout.type());
        assertEquals(20.0, payout.delta(), 1e-9);           // 20 shares * $1
        // last entry's running balance is the user's current balance
        assertEquals(user("trader").balance(), payout.balanceAfter(), 1e-9);
        assertEquals(details.userInfo().balance(), payout.balanceAfter(), 1e-9);
    }

    @Test
    void closeEventDeclaresTheWinnerPaysHoldersAndSweepsThePoolToTheMarketMaker() {
        engine.activateEvent(event(), user("mm"));
        engine.buyShares(user("trader"), event(), 1, 20);   // 20 Heads
        double afterBuy = user("trader").balance();
        double mmBeforeClose = user("mm").balance();
        double poolBeforeClose = engine.getEventDetails(1).eventAccountBalance();

        engine.closeEvent(1, 1);

        assertEquals("Heads", engine.getEventDetails(1).winningOption());
        assertEquals(EventStatus.CLOSED, engine.getEventDetails(1).eventInfo().status());
        assertEquals(afterBuy + 20.0, user("trader").balance(), 1e-9);   // on-purchase: full $1/share
        assertEquals(0.0, engine.getEventDetails(1).eventAccountBalance(), 1e-9);
        assertEquals(mmBeforeClose + poolBeforeClose - 20.0, user("mm").balance(), 1e-9);
    }
}
