package com.guessmarket.engine.model;

import com.guessmarket.dto.OrderSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookTest {

    @Test
    void bidsAreOrderedPriceDescendingThenByArrival() {
        OrderBook book = new OrderBook();
        book.restNew("a", 0, OrderSide.BID, 10, 0.4);
        book.restNew("b", 0, OrderSide.BID, 10, 0.6);
        book.restNew("c", 0, OrderSide.BID, 10, 0.5);
        book.restNew("d", 0, OrderSide.BID, 10, 0.6);   // ties with b, arrives later

        assertEquals(List.of("b", "d", "c", "a"), book.bids(0).stream().map(LimitOrder::getUserName).toList());
    }

    @Test
    void asksAreOrderedPriceAscendingThenByArrival() {
        OrderBook book = new OrderBook();
        book.restNew("a", 0, OrderSide.ASK, 10, 0.6);
        book.restNew("b", 0, OrderSide.ASK, 10, 0.4);
        book.restNew("c", 0, OrderSide.ASK, 10, 0.5);
        book.restNew("d", 0, OrderSide.ASK, 10, 0.4);   // ties with b, arrives later

        assertEquals(List.of("b", "d", "c", "a"), book.asks(0).stream().map(LimitOrder::getUserName).toList());
    }

    @Test
    void bestBidAndAskReflectTheTopOfEachSideAndOptionsAreIndependent() {
        OrderBook book = new OrderBook();
        assertTrue(book.bestBid(0).isEmpty());
        assertTrue(book.bestAsk(0).isEmpty());

        book.restNew("a", 0, OrderSide.BID, 10, 0.4);
        book.restNew("b", 0, OrderSide.ASK, 10, 0.6);

        assertEquals(0.4, book.bestBid(0).orElseThrow().getPrice(), 1e-9);
        assertEquals(0.6, book.bestAsk(0).orElseThrow().getPrice(), 1e-9);
        assertTrue(book.bids(1).isEmpty());
        assertTrue(book.asks(1).isEmpty());
    }

    @Test
    void removeAndFindById() {
        OrderBook book = new OrderBook();
        LimitOrder order = book.restNew("a", 0, OrderSide.BID, 10, 0.4);

        assertSame(order, book.findById(order.getId()));

        book.remove(order);

        assertNull(book.findById(order.getId()));
        assertTrue(book.bids(0).isEmpty());
    }

    @Test
    void clearDiscardsEveryRestingOrderOnBothOptions() {
        OrderBook book = new OrderBook();
        book.restNew("a", 0, OrderSide.BID, 10, 0.4);
        book.restNew("a", 1, OrderSide.ASK, 5, 0.7);

        book.clear();

        assertTrue(book.bids(0).isEmpty());
        assertTrue(book.asks(1).isEmpty());
    }

    @Test
    void lastTradePriceIsNanUntilRecorded() {
        OrderBook book = new OrderBook();
        assertTrue(Double.isNaN(book.lastTradePrice(0)));

        book.recordTrade(0, 0.42);

        assertEquals(0.42, book.lastTradePrice(0), 1e-9);
        assertTrue(Double.isNaN(book.lastTradePrice(1)));   // options are independent
    }
}
