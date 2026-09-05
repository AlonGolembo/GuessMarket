package com.guessmarket.engine.model;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.engine.exception.InsufficientFundsException;
import com.guessmarket.engine.exception.MarketException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventOrderBookTest {

    private Event orderBookEvent(CommissionType type, int commissionPct, int d, int initial, boolean allowMint) {
        return new Event(1, "Coin flip", "heads?", commissionPct, type,
                List.of(new Option("Heads"), new Option("Tails")), new OrderBookMethod(d, initial, allowMint));
    }

    private User user(String name, double cash, Set<Integer> mmEvents) {
        return new User(name, cash, mmEvents);
    }

    // --- open --------------------------------------------------------------

    @Test
    void openPaysTheMarketMakerForThePairsAndPostsInitialAsksAtHalfBaseValue() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        User mm = user("mm", 1000, Set.of(1));

        e.open(mm);

        assertEquals(EventStatus.ACTIVE, e.getStatus());
        assertEquals(900.0, mm.getAccountBalance(), 1e-9);              // 100 pairs * d=1
        assertArrayEquals(new int[]{100, 100}, e.holdingsOf("mm"));
        assertEquals(100, e.getOptions().get(0).getSharesOutstanding());
        assertEquals(100, e.getOptions().get(1).getSharesOutstanding());

        for (int opt = 0; opt < 2; opt++) {
            LimitOrder ask = e.getOrderBook().bestAsk(opt).orElseThrow();
            assertEquals(0.5, ask.getPrice(), 1e-9);
            assertEquals(100, ask.getRemaining());
            assertEquals("mm", ask.getUserName());
        }
    }

    @Test
    void openWithNoInitialAllocationStillCreatesAnEmptyBookWhenMintingIsAllowed() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 0, true);
        User mm = user("mm", 1000, Set.of(1));

        e.open(mm);

        assertEquals(1000.0, mm.getAccountBalance(), 1e-9);   // no pairs bought
        assertTrue(e.getOrderBook().bestAsk(0).isEmpty());
        assertTrue(e.getOrderBook().bestAsk(1).isEmpty());
    }

    // --- placeOrder guards ---------------------------------------------------

    @Test
    void placeOrderRejectsAnEventThatIsNotOrderBook() {
        Event e = new Event(2, "E", "d", 0, CommissionType.ON_PURCHASE,
                List.of(new Option("Heads"), new Option("Tails")), new LmsrMethod(100));
        e.open(user("mm", 1000, Set.of(2)));

        assertThrows(MarketException.class, () -> e.placeOrder(user("t", 100, Set.of()), 0, OrderSide.BID, 10, 0.5));
    }

    @Test
    void placeOrderValidatesPriceIsWithinZeroToBaseValue() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));
        User t = user("t", 1000, Set.of());

        assertThrows(MarketException.class, () -> e.placeOrder(t, 0, OrderSide.BID, 10, 0.0));
        assertThrows(MarketException.class, () -> e.placeOrder(t, 0, OrderSide.BID, 10, 1.01));
    }

    @Test
    void askIsRejectedWithoutEnoughFreeShares() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));
        User t = user("t", 1000, Set.of());   // holds nothing

        assertThrows(MarketException.class, () -> e.placeOrder(t, 0, OrderSide.ASK, 10, 0.5));
    }

    @Test
    void bidIsRejectedWhenUnaffordable() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));
        User poor = user("t", 1.0, Set.of());

        assertThrows(InsufficientFundsException.class, () -> e.placeOrder(poor, 0, OrderSide.BID, 10, 0.5));
    }

    // --- matching ------------------------------------------------------------

    @Test
    void unmatchedBidRestsUntouched() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));
        User t = user("t", 1000, Set.of());

        OrderOutcome outcome = e.placeOrder(t, 0, OrderSide.BID, 10, 0.4);   // below the 0.5 ask

        assertEquals(0, outcome.filledQuantity());
        assertEquals(10, outcome.restingQuantity());
        assertArrayEquals(new int[]{0, 0}, e.holdingsOf("t"));
        assertEquals(1000.0, t.getAccountBalance(), 1e-9);
        assertEquals(10, e.getOrderBook().bestBid(0).orElseThrow().getRemaining());
    }

    @Test
    void directMatchExecutesAtTheRestingPriceAndChargesOnPurchaseCommission() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 10, 1, 100, true);
        User mm = user("mm", 1000, Set.of(1));
        e.open(mm);                                          // mm posts 100 Heads @ 0.5
        User trader = user("t", 1000, Set.of());

        OrderOutcome outcome = e.placeOrder(trader, 0, OrderSide.BID, 30, 0.6);   // willing to pay up to 0.6

        assertEquals(30, outcome.filledQuantity());
        assertEquals(0, outcome.restingQuantity());
        // executes at the resting ask's price (0.5), not the bid's price (0.6)
        assertEquals(1000 - (30 * 0.5 * 1.10), trader.getAccountBalance(), 1e-9);
        assertEquals(900.0 + 30 * 0.5 + 30 * 0.5 * 0.10, mm.getAccountBalance(), 1e-9);   // proceeds + commission
        assertArrayEquals(new int[]{30, 0}, e.holdingsOf("t"));
        assertArrayEquals(new int[]{70, 100}, e.holdingsOf("mm"));
        assertEquals(70, e.getOrderBook().bestAsk(0).orElseThrow().getRemaining());
        assertEquals(1, e.getTradeHistory().size());
    }

    @Test
    void anIncomingOrderCanFillAcrossTwoPriceLevelsFromTwoSellers() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, false);
        User mm = user("mm", 1000, Set.of(1));
        e.open(mm);                                          // mm: ask 100 Heads @ 0.5
        User a = user("a", 1000, Set.of());
        User b = user("b", 1000, Set.of());

        e.placeOrder(a, 0, OrderSide.BID, 20, 0.5);           // a buys 20 @ 0.5 from mm (mm ask -> 80 left)
        e.placeOrder(a, 0, OrderSide.ASK, 20, 0.55);          // a resells all 20 @ 0.55

        OrderOutcome outcome = e.placeOrder(b, 0, OrderSide.BID, 90, 0.60);   // crosses both levels

        assertEquals(90, outcome.filledQuantity());
        assertEquals(0, outcome.restingQuantity());

        assertEquals(1000 - 10.0 + 5.5, a.getAccountBalance(), 1e-9);     // paid 10 to buy, got 5.5 selling 10 of 20
        assertEquals(1000 - 40.0 - 5.5, b.getAccountBalance(), 1e-9);     // 80 @ 0.50 + 10 @ 0.55
        assertEquals(900.0 + 10.0 + 40.0, mm.getAccountBalance(), 1e-9);  // sold 20 to a, then 80 to b

        assertArrayEquals(new int[]{10, 0}, e.holdingsOf("a"));          // bought 20, sold 10
        assertArrayEquals(new int[]{90, 0}, e.holdingsOf("b"));
        assertArrayEquals(new int[]{0, 100}, e.holdingsOf("mm"));

        LimitOrder remainingAsk = e.getOrderBook().bestAsk(0).orElseThrow();
        assertEquals("a", remainingAsk.getUserName());
        assertEquals(10, remainingAsk.getRemaining());
    }

    // --- cancelOrder -----------------------------------------------------------

    @Test
    void cancelOrderRemovesAnOwnedRestingOrder() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));
        User t = user("t", 1000, Set.of());
        OrderOutcome outcome = e.placeOrder(t, 0, OrderSide.BID, 10, 0.4);

        e.cancelOrder(t, outcome.orderId());

        assertNull(e.getOrderBook().findById(outcome.orderId()));
    }

    @Test
    void cancelOrderRejectsSomeoneElsesOrder() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));
        User t = user("t", 1000, Set.of());
        User other = user("other", 1000, Set.of());
        OrderOutcome outcome = e.placeOrder(t, 0, OrderSide.BID, 10, 0.4);

        assertThrows(MarketException.class, () -> e.cancelOrder(other, outcome.orderId()));
    }

    @Test
    void cancelOrderRejectsAnUnknownId() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));

        assertThrows(MarketException.class, () -> e.cancelOrder(user("t", 100, Set.of()), 999L));
    }

    // --- settlement --------------------------------------------------------

    @Test
    void settlementPaysBaseValuePerWinningShareAndClearsTheBook() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 5, 100, true);   // d=5
        User mm = user("mm", 10_000, Set.of(1));
        e.open(mm);
        User trader = user("t", 10_000, Set.of());
        e.placeOrder(trader, 0, OrderSide.BID, 40, 2.5);      // buys 40 Heads @ mm's initial ask price (d/2)
        double traderAfterBuy = trader.getAccountBalance();

        e.settleAndClose(0);                                   // Heads wins, base value 5 per share

        assertEquals(EventStatus.CLOSED, e.getStatus());
        assertEquals(traderAfterBuy + 40 * 5.0, trader.getAccountBalance(), 1e-9);
        assertEquals(0.0, e.getEventAccountBalance(), 1e-9);
        assertTrue(e.getOrderBook().bestAsk(0).isEmpty());
        assertTrue(e.getOrderBook().bestAsk(1).isEmpty());
    }
}
