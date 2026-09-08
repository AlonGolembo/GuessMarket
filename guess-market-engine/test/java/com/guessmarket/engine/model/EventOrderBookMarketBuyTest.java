package com.guessmarket.engine.model;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.OrderSide;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link Event#buy}/{@link Event#quote} on an Order Book event: a market order against the ask book. */
class EventOrderBookMarketBuyTest {

    private Event orderBookEvent(CommissionType type, int commissionPct, int d, int initial, boolean allowMint) {
        return new Event(1, "Coin flip", "heads?", commissionPct, type,
                List.of(new Option("Heads"), new Option("Tails")), new OrderBookMethod(d, initial, allowMint));
    }

    private User user(String name, double cash, Set<Integer> mmEvents) {
        return new User(name, cash, mmEvents);
    }

    @Test
    void marketBuySweepsTheCheapestAskAndReportsTheFilledQuantity() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 10, 1, 100, true);
        User mm = user("mm", 1000, Set.of(1));
        e.open(mm);                                      // mm posts 100 Heads @ 0.5

        TradeReceipt receipt = e.buy(user("t", 1000, Set.of()), 0, 30);

        assertEquals(30, receipt.filledQuantity());
        assertEquals(30 * 0.5, receipt.sharesCost(), 1e-9);
        assertEquals(30 * 0.5 * 0.10, receipt.commission(), 1e-9);
        assertEquals(900.0 + 30 * 0.5 + 30 * 0.5 * 0.10, mm.getAccountBalance(), 1e-9);   // proceeds + commission
    }

    @Test
    void marketBuyStopsWhenTheBookRunsDryAndReportsAPartialFill() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 50, true);
        e.open(user("mm", 1000, Set.of(1)));               // mm posts only 50 Heads @ 0.5
        User trader = user("t", 1000, Set.of());

        TradeReceipt receipt = e.buy(trader, 0, 80);        // asks for 80, only 50 exist

        assertEquals(50, receipt.filledQuantity());
        assertEquals(50 * 0.5, receipt.sharesCost(), 1e-9);
        assertEquals(1000 - 25.0, trader.getAccountBalance(), 1e-9);
        assertArrayEquals(new int[]{50, 0}, e.holdingsOf("t"));
    }

    @Test
    void marketBuyNeverMintsAndNeverRestsARemainder() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 0, true);   // no initial allocation
        e.open(user("mm", 1000, Set.of(1)));
        User bidder = user("bidder", 1000, Set.of());
        User buyer = user("buyer", 1000, Set.of());

        e.placeOrder(bidder, 1, OrderSide.BID, 50, 0.9);   // a resting Tails bid that could otherwise mint

        TradeReceipt receipt = e.buy(buyer, 0, 20);         // market-buys Heads: no Heads asks exist at all

        assertEquals(0, receipt.filledQuantity());
        assertEquals(0, e.getOptions().get(0).getSharesOutstanding());
        assertEquals(0, e.getOptions().get(1).getSharesOutstanding());   // bidder's bid untouched, nothing minted
        assertEquals(1000.0, buyer.getAccountBalance(), 1e-9);
    }

    @Test
    void marketBuyClampsRatherThanThrowingWhenTheBuyerCantAffordEverything() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 0, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));                // mm posts 100 Heads @ 0.5
        User poor = user("t", 3.0, Set.of());

        TradeReceipt receipt = e.buy(poor, 0, 10);           // can afford floor(3.0/0.5) = 6

        assertEquals(6, receipt.filledQuantity());
        assertEquals(3.0, receipt.sharesCost(), 1e-9);
        assertEquals(0.0, poor.getAccountBalance(), 1e-9);
    }

    @Test
    void quoteMatchesWhatBuyChargesForAnOrderBookEvent() {
        Event e = orderBookEvent(CommissionType.ON_PURCHASE, 15, 1, 100, true);
        e.open(user("mm", 1000, Set.of(1)));
        User trader = user("t", 1000, Set.of());

        TradeReceipt quoted = e.quote(0, 40);
        TradeReceipt charged = e.buy(trader, 0, 40);

        assertEquals(quoted.filledQuantity(), charged.filledQuantity());
        assertEquals(quoted.totalPaid(), charged.totalPaid(), 1e-9);
    }
}
