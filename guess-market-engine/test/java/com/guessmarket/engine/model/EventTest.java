package com.guessmarket.engine.model;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.engine.exception.InsufficientFundsException;
import com.guessmarket.engine.exception.MarketException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest {

    private static final int B = 100;
    private static final double SUBSIDY = B * Math.log(2);   // LMSR C(0,0)

    private Event event(CommissionType type, int commissionPct) {
        return new Event(1, "Coin flip", "heads?", commissionPct, type,
                List.of(new Option("Heads"), new Option("Tails")), new LmsrMethod(B));
    }

    private User user(String name, double cash, Set<Integer> mmEvents) {
        return new User(name, cash, mmEvents);
    }

    // --- open ------------------------------------------------------------

    @Test
    void openDebitsTheSubsidyFromTheMarketMakerAndActivates() {
        Event e = event(CommissionType.ON_PURCHASE, 0);
        User mm = user("mm", 1000, Set.of(1));

        e.open(mm);

        assertEquals(EventStatus.ACTIVE, e.getStatus());
        assertEquals(1000 - SUBSIDY, mm.getAccountBalance(), 1e-6);
        assertEquals(SUBSIDY, e.getEventAccountBalance(), 1e-6);
    }

    @Test
    void onlyTheMarketMakerMayOpenTheEvent() {
        Event e = event(CommissionType.ON_PURCHASE, 0);
        User notMm = user("bob", 1000, Set.of());
        assertThrows(MarketException.class, () -> e.open(notMm));
        assertEquals(EventStatus.NOT_ACTIVE, e.getStatus());
    }

    @Test
    void openFailsWithoutFundsAndLeavesEventPending() {
        Event e = event(CommissionType.ON_PURCHASE, 0);
        User brokeMm = user("mm", 10, Set.of(1));
        assertThrows(InsufficientFundsException.class, () -> e.open(brokeMm));
        assertEquals(EventStatus.NOT_ACTIVE, e.getStatus());
        assertEquals(10, brokeMm.getAccountBalance(), 1e-9);
    }

    // --- buy -----------------------------------------------------------

    @Test
    void buyChargesLmsrCostPlusOnPurchaseCommissionAndRecordsTheTrade() {
        Event e = event(CommissionType.ON_PURCHASE, 10);
        e.open(user("mm", 1000, Set.of(1)));
        User trader = user("t", 500, Set.of());

        TradeReceipt r = e.buy(trader, 0, 50);

        assertTrue(r.sharesCost() > 0);
        assertEquals(r.sharesCost() * 0.10, r.commission(), 1e-9);
        assertEquals(r.sharesCost() + r.commission(), r.totalPaid(), 1e-9);
        assertEquals(500 - r.totalPaid(), trader.getAccountBalance(), 1e-9);
        assertEquals(50, e.getOptions().get(0).getSharesOutstanding());
        assertEquals(1, e.getTradeHistory().size());
        assertEquals("t", e.getTradeHistory().get(0).getBuyerName());
        assertArrayEquals(new int[]{50, 0}, e.holdingsOf("t"));   // 50 of option 0, none of option 1
    }

    @Test
    void holdingsAccumulateAcrossOptionsAndTrades() {
        Event e = event(CommissionType.ON_PURCHASE, 0);
        e.open(user("mm", 10_000, Set.of(1)));
        User trader = user("t", 10_000, Set.of());

        e.buy(trader, 0, 10);
        e.buy(trader, 0, 5);
        e.buy(trader, 1, 7);

        assertArrayEquals(new int[]{15, 7}, e.holdingsOf("t"));
        assertArrayEquals(new int[]{0, 0}, e.holdingsOf("someone-who-never-traded"));
    }

    @Test
    void quoteMatchesWhatBuyCharges() {
        Event e = event(CommissionType.ON_PURCHASE, 15);
        e.open(user("mm", 1000, Set.of(1)));
        User trader = user("t", 1000, Set.of());

        TradeReceipt quoted = e.quote(1, 20);
        TradeReceipt charged = e.buy(trader, 1, 20);

        assertEquals(quoted.totalPaid(), charged.totalPaid(), 1e-12);
    }

    @Test
    void unaffordableTradeIsRejectedWithoutChangingEventState() {
        Event e = event(CommissionType.ON_PURCHASE, 0);
        e.open(user("mm", 1000, Set.of(1)));
        User poor = user("t", 1.0, Set.of());

        assertThrows(InsufficientFundsException.class, () -> e.buy(poor, 0, 10_000));
        assertEquals(0, e.getOptions().get(0).getSharesOutstanding());
        assertEquals(0, e.getTradeHistory().size());
    }

    @Test
    void cannotBuyFromAPendingOrClosedEvent() {
        Event e = event(CommissionType.ON_PURCHASE, 0);
        User trader = user("t", 1000, Set.of());
        assertThrows(MarketException.class, () -> e.buy(trader, 0, 1));
    }

    // --- settle ------------------------------------------------------

    @Test
    void onCloseSettlementTakesTheFeeFromTheWinningPotAndPaysHoldersNet() {
        Event e = event(CommissionType.ON_CLOSE, 10);
        e.open(user("mm", 10_000, Set.of(1)));
        User trader = user("t", 1000, Set.of());
        e.buy(trader, 0, 40);                    // 40 Heads, no commission charged now
        double afterBuy = trader.getAccountBalance();

        e.settleAndClose(0);                     // Heads wins

        assertEquals(EventStatus.CLOSED, e.getStatus());
        assertEquals("Heads", e.getWinningOption().getName());
        // 40 winning shares * (1 - 0.10) = 36 paid out
        assertEquals(afterBuy + 36.0, trader.getAccountBalance(), 1e-9);
        assertEquals(40 * 0.10, e.getTotalCommissionCollected(), 1e-9);
    }

    @Test
    void onPurchaseSettlementPaysFullDollarPerWinningShare() {
        Event e = event(CommissionType.ON_PURCHASE, 10);
        e.open(user("mm", 10_000, Set.of(1)));
        User trader = user("t", 1000, Set.of());
        e.buy(trader, 1, 25);                    // 25 Tails
        double afterBuy = trader.getAccountBalance();

        e.settleAndClose(1);                     // Tails wins

        assertEquals(afterBuy + 25.0, trader.getAccountBalance(), 1e-9);
    }

    @Test
    void settlingTwiceIsRejected() {
        Event e = event(CommissionType.ON_CLOSE, 0);
        e.open(user("mm", 10_000, Set.of(1)));
        e.settleAndClose(0);
        assertThrows(MarketException.class, () -> e.settleAndClose(1));
    }
}
