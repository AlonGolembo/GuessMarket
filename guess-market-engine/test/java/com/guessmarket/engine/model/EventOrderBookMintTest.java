package com.guessmarket.engine.model;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.OrderSide;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventOrderBookMintTest {

    private Event orderBookEvent(CommissionType type, int commissionPct, int d, int initial, boolean allowMint) {
        return new Event(1, "Coin flip", "heads?", commissionPct, type,
                List.of(new Option("Heads"), new Option("Tails")), new OrderBookMethod(d, initial, allowMint));
    }

    private User user(String name, double cash, Set<Integer> mmEvents) {
        return new User(name, cash, mmEvents);
    }

    @Test
    void equalQuantityMintCreatesNewSharesForBothSidesAndFundsThePool() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 0, true);   // d=1, no initial allocation
        e.open(user("mm", 1000, Set.of(1)));
        User a = user("a", 1000, Set.of());
        User b = user("b", 1000, Set.of());

        e.placeOrder(a, 0, OrderSide.BID, 50, 0.6);                       // rests: no Heads asks to match
        OrderOutcome outcome = e.placeOrder(b, 1, OrderSide.BID, 50, 0.5); // 0.6+0.5 >= d=1 -> mints against a's bid

        assertEquals(50, outcome.filledQuantity());
        assertEquals(0, outcome.restingQuantity());

        assertEquals(1000 - 50 * 0.6, a.getAccountBalance(), 1e-9);   // a pays its own bid price in full
        assertEquals(1000 - 50 * 0.4, b.getAccountBalance(), 1e-9);   // b pays the complement (1 - 0.6)

        assertEquals(50, e.getOptions().get(0).getSharesOutstanding());
        assertEquals(50, e.getOptions().get(1).getSharesOutstanding());
        assertArrayEquals(new int[]{50, 0}, e.holdingsOf("a"));
        assertArrayEquals(new int[]{0, 50}, e.holdingsOf("b"));
        assertEquals(50.0, e.getEventAccountBalance(), 1e-9);          // 50 pairs * d=1

        assertTrue(e.getOrderBook().bestBid(0).isEmpty());             // a's bid fully consumed
    }

    @Test
    void partialMintLeavesTheLargerBidsRemainderResting() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 0, true);
        e.open(user("mm", 1000, Set.of(1)));
        User a = user("a", 1000, Set.of());
        User b = user("b", 1000, Set.of());

        e.placeOrder(a, 0, OrderSide.BID, 30, 0.6);
        OrderOutcome outcome = e.placeOrder(b, 1, OrderSide.BID, 50, 0.5);   // only 30 pairs can mint

        assertEquals(30, outcome.filledQuantity());
        assertEquals(20, outcome.restingQuantity());
        assertEquals(30, e.getOptions().get(0).getSharesOutstanding());
        assertEquals(30, e.getOptions().get(1).getSharesOutstanding());
        assertTrue(e.getOrderBook().bestBid(0).isEmpty());              // a's bid fully consumed

        LimitOrder remainder = e.getOrderBook().bestBid(1).orElseThrow();
        assertEquals("b", remainder.getUserName());
        assertEquals(20, remainder.getRemaining());
    }

    @Test
    void mintingIsSkippedWhenTheMethodDisallowsIt() {
        // initial > 0 so validate() accepts allowMint=false; prices kept below mm's
        // 0.5 asks so both bids purely rest instead of direct-matching mm.
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 10, false);
        e.open(user("mm", 1000, Set.of(1)));
        User a = user("a", 1000, Set.of());
        User b = user("b", 1000, Set.of());

        e.placeOrder(a, 0, OrderSide.BID, 5, 0.4);
        OrderOutcome outcome = e.placeOrder(b, 1, OrderSide.BID, 5, 0.4);   // would sum to 0.8 < d=1 anyway,
        // but the point is tryMint must never even run when allowMint=false.

        assertEquals(0, outcome.filledQuantity());
        assertEquals(5, outcome.restingQuantity());
    }

    @Test
    void mintingRequiresThePricesToSumToAtLeastTheBaseValue() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 0, true);
        e.open(user("mm", 1000, Set.of(1)));
        User a = user("a", 1000, Set.of());
        User b = user("b", 1000, Set.of());

        e.placeOrder(a, 0, OrderSide.BID, 50, 0.3);
        OrderOutcome outcome = e.placeOrder(b, 1, OrderSide.BID, 50, 0.3);   // 0.3+0.3 = 0.6 < d=1

        assertEquals(0, outcome.filledQuantity());
        assertEquals(50, outcome.restingQuantity());
        assertEquals(0, e.getOptions().get(0).getSharesOutstanding());
        assertEquals(0, e.getOptions().get(1).getSharesOutstanding());
    }

    @Test
    void mintChargesOnPurchaseCommissionToEachBuyerCreditedToTheMarketMakerImmediately() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 10, 1, 0, true);
        User mm = user("mm", 1000, Set.of(1));
        e.open(mm);
        User a = user("a", 1000, Set.of());
        User b = user("b", 1000, Set.of());

        e.placeOrder(a, 0, OrderSide.BID, 50, 0.6);
        e.placeOrder(b, 1, OrderSide.BID, 50, 0.5);

        // a pays 50*0.6=30 + 10% commission = 33; b pays 50*0.4=20 + 10% commission = 22.
        assertEquals(1000 - 33.0, a.getAccountBalance(), 1e-9);
        assertEquals(1000 - 22.0, b.getAccountBalance(), 1e-9);
        assertEquals(1000 + 5.0, mm.getAccountBalance(), 1e-9);       // 3 + 2 commission credited immediately
        assertEquals(50.0, e.getEventAccountBalance(), 1e-9);          // pool unaffected by commission
    }
}
