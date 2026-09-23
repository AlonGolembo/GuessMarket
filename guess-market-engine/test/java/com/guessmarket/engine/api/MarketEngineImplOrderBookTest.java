package com.guessmarket.engine.api;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.LimitOrderDTO;
import com.guessmarket.dto.OrderBookQuoteDTO;
import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.MarketException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link MarketEngine} through the Order Book method: placeOrder/cancelOrder and the DTOs they surface. */
class MarketEngineImplOrderBookTest {

    private static final String XML = """
        <Guess-Market>
          <GM-events>
            <GM-event name="Coin flip">
              <id>1</id><description>heads?</description>
              <commission type="on-purchase">10</commission>
              <GM-options><GM-option>Heads</GM-option><GM-option>Tails</GM-option></GM-options>
              <GM-method><GM-order-book d="1" initial="100" allow-mint="true"/></GM-method>
            </GM-event>
          </GM-events>
        </Guess-Market>
        """;

    @TempDir
    Path dir;
    private MarketEngine engine;

    @BeforeEach
    void setUp() throws IOException {
        String xmlPath = dir.resolve("market.xml").toString();
        Files.writeString(Path.of(xmlPath), XML);
        engine = new MarketEngineImpl();
        engine.login("mm");
        engine.deposit("mm", 1000);
        engine.login("a");
        engine.deposit("a", 1000);
        engine.login("b");
        engine.deposit("b", 1000);
        engine.loadXmlFile(xmlPath, "mm");
        engine.activateEvent(event().name(), user("mm").name());   // mm posts 100 Heads @ 0.5, 100 Tails @ 0.5
    }

    private EventDTO event() {
        return engine.getAllEvents().get(0);
    }

    private UserDTO user(String name) {
        return engine.getAllUsers().get(name);
    }

    @Test
    void placeOrderMatchesDirectlyAndReportsCashMovedAndCommission() {
        OrderResultDTO result = engine.placeOrder(user("a").name(), event().name(), 1, OrderSide.BID, 30, 0.6);

        assertEquals(30, result.filledQuantity());
        assertEquals(0, result.restingQuantity());
        assertNull(result.restingOrderId());
        assertEquals(30 * 0.5, result.cashMoved(), 1e-9);
        assertEquals(30 * 0.5 * 0.10, result.commission(), 1e-9);
        assertEquals(Map.of("Heads", 30, "Tails", 0), engine.getAllUsers().get("a").holdings().get("Coin flip"));
    }

    @Test
    void aRestingBidThatCanNoLongerBeAffordedForcesTheOwnerNegativeAndBlocksThem() throws IOException {
        String xml = """
            <Guess-Market>
              <GM-events>
                <GM-event name="E"><id>1</id><description>d</description>
                  <commission type="on-close">0</commission>
                  <GM-options><GM-option>Heads</GM-option><GM-option>Tails</GM-option></GM-options>
                  <GM-method><GM-order-book d="1" initial="100" allow-mint="false"/></GM-method>
                </GM-event>
              </GM-events>
            </Guess-Market>
            """;
        Path p = dir.resolve("m2.xml");
        Files.writeString(p, xml);
        MarketEngine e = new MarketEngineImpl();
        e.login("mm");
        e.deposit("mm", 1000);
        e.login("a");
        e.deposit("a", 200);
        e.login("b");
        e.deposit("b", 30);
        e.loadXmlFile(p.toString(), "mm");
        EventDTO ev = e.getAllEvents().get(0);
        e.activateEvent(ev.name(), e.getAllUsers().get("mm").name());

        // 'a' buys all 100 of the market maker's Heads (bid @ 0.5 crosses the ask @ 0.5).
        e.placeOrder(e.getAllUsers().get("a").name(), ev.name(), 1, OrderSide.BID, 100, 0.5);

        // 'b' has only $30 but rests two Heads bids of 20 @ $1.00 - jointly $40.
        e.placeOrder(e.getAllUsers().get("b").name(), ev.name(), 1, OrderSide.BID, 20, 1.0);
        e.placeOrder(e.getAllUsers().get("b").name(), ev.name(), 1, OrderSide.BID, 20, 1.0);
        assertFalse(e.getAllUsers().get("b").blocked());

        // 'a' sells 40 Heads @ 0.5 - both of b's $1.00 bids match; the second can't be covered.
        e.placeOrder(e.getAllUsers().get("a").name(), ev.name(), 1, OrderSide.ASK, 40, 0.5);

        UserDTO b = e.getAllUsers().get("b");
        assertTrue(b.blocked(), "b should be blocked after being forced negative");
        assertTrue(b.balance() < 0, "b's balance should be negative, was " + b.balance());

        // A blocked user is refused every further action.
        MarketException ex = assertThrows(MarketException.class,
                () -> e.placeOrder(e.getAllUsers().get("b").name(), ev.name(), 2, OrderSide.BID, 1, 0.1));
        assertTrue(ex.getMessage().toLowerCase().contains("block"));
    }

    @Test
    void placeOrderThatDoesNotCrossRestsAndIsReportedInEventDetails() {
        OrderResultDTO result = engine.placeOrder(user("a").name(), event().name(), 1, OrderSide.BID, 10, 0.2);

        assertEquals(0, result.filledQuantity());
        assertEquals(10, result.restingQuantity());
        assertNotNull(result.restingOrderId());

        EventDetailsDTO details = engine.getEventDetails("Coin flip");
        assertTrue(details.restingOrders().stream()
                .anyMatch(o -> o.id() == result.restingOrderId() && o.userName().equals("a") && o.remaining() == 10));
    }

    @Test
    void cancelOrderRemovesARestingOrder() {
        OrderResultDTO placed = engine.placeOrder(user("a").name(), event().name(), 1, OrderSide.BID, 10, 0.2);

        engine.cancelOrder(user("a").name(), event().name(), placed.restingOrderId());

        assertTrue(engine.getEventDetails("Coin flip").restingOrders().stream()
                .noneMatch(o -> o.id() == placed.restingOrderId()));
    }

    @Test
    void cancelOrderRejectsSomeoneElsesOrder() {
        OrderResultDTO placed = engine.placeOrder(user("a").name(), event().name(), 1, OrderSide.BID, 10, 0.2);

        assertThrows(MarketException.class, () -> engine.cancelOrder(user("b").name(), event().name(), placed.restingOrderId()));
    }

    @Test
    void buyIsAMarketOrderThatCanPartiallyFill() {
        TradeResultDTO result = engine.buyShares(user("a").name(), event().name(), 2, 150);   // only 100 Tails exist

        assertEquals(100, result.filledQuantity());
        assertEquals(100 * 0.5, result.sharesCost(), 1e-9);
    }

    @Test
    void eventDetailsExposeTheFiveOrderBookIndicators() {
        engine.placeOrder(user("a").name(), event().name(), 1, OrderSide.BID, 30, 0.6);   // trades at 0.5

        OrderBookQuoteDTO quote = engine.getEventDetails("Coin flip").orderBookQuotes().get("Heads");

        assertEquals(0.5, quote.lastTrade(), 1e-9);
        assertEquals(0.5, quote.bestAsk(), 1e-9);         // mm's remaining ask
        assertNull(quote.bestBid());                       // the incoming bid fully filled, nothing rests
    }
}
