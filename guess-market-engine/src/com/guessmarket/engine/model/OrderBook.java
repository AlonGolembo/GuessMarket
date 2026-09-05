package com.guessmarket.engine.model;

import com.guessmarket.dto.OrderSide;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The public bid/ask book for one Order Book event: one independent pair of
 * price-then-time-ordered sides per option, plus the last traded price on
 * each. Bids are kept price-descending, asks price-ascending; equal prices
 * keep arrival order, giving strict price-time priority.
 *
 * <p>Pure book-keeping - it knows nothing about money, holdings or
 * commission. {@link Event} owns the matching rules and applies those
 * effects; this class only tracks which orders exist, in what order, and
 * assigns each a unique id. Package-private for the same reason as
 * {@link LimitOrder}.
 */
final class OrderBook implements Serializable {

    private long nextOrderId = 1;
    private final Book[] books = {new Book(), new Book()};

    private static final class Book implements Serializable {
        final List<LimitOrder> bids = new ArrayList<>();   // price desc, then arrival order
        final List<LimitOrder> asks = new ArrayList<>();   // price asc, then arrival order
        double lastTradePrice = Double.NaN;
    }

    /** Creates a new order with the next id; it is not yet in the book (see {@link #rest}). */
    LimitOrder createOrder(String userName, int optionIndex, OrderSide side, int quantity, double price) {
        return new LimitOrder(nextOrderId++, userName, optionIndex, side, quantity, price);
    }

    /** Inserts an order into its side, keeping price-then-time priority. */
    void rest(LimitOrder order) {
        List<LimitOrder> list = sideList(order.getOptionIndex(), order.getSide());
        int insertAt = list.size();
        for (int i = 0; i < list.size(); i++) {
            LimitOrder existing = list.get(i);
            boolean better = order.getSide() == OrderSide.BID
                    ? order.getPrice() > existing.getPrice()
                    : order.getPrice() < existing.getPrice();
            if (better) {
                insertAt = i;
                break;
            }
        }
        list.add(insertAt, order);
    }

    /** Convenience for the initial allocation: create and rest in one call, with no matching. */
    LimitOrder restNew(String userName, int optionIndex, OrderSide side, int quantity, double price) {
        LimitOrder order = createOrder(userName, optionIndex, side, quantity, price);
        rest(order);
        return order;
    }

    void remove(LimitOrder order) {
        sideList(order.getOptionIndex(), order.getSide()).remove(order);
    }

    List<LimitOrder> bids(int optionIndex) {
        return books[optionIndex].bids;
    }

    List<LimitOrder> asks(int optionIndex) {
        return books[optionIndex].asks;
    }

    Optional<LimitOrder> bestBid(int optionIndex) {
        List<LimitOrder> bids = bids(optionIndex);
        return bids.isEmpty() ? Optional.empty() : Optional.of(bids.get(0));
    }

    Optional<LimitOrder> bestAsk(int optionIndex) {
        List<LimitOrder> asks = asks(optionIndex);
        return asks.isEmpty() ? Optional.empty() : Optional.of(asks.get(0));
    }

    /** {@code NaN} until the first trade on this option. */
    double lastTradePrice(int optionIndex) {
        return books[optionIndex].lastTradePrice;
    }

    void recordTrade(int optionIndex, double price) {
        books[optionIndex].lastTradePrice = price;
    }

    /** Finds a resting order by id across every option/side, or {@code null}. */
    LimitOrder findById(long orderId) {
        for (Book book : books) {
            for (LimitOrder order : book.bids) {
                if (order.getId() == orderId) {
                    return order;
                }
            }
            for (LimitOrder order : book.asks) {
                if (order.getId() == orderId) {
                    return order;
                }
            }
        }
        return null;
    }

    /** Discards every resting order on both sides of both options (called on settlement). */
    void clear() {
        for (Book book : books) {
            book.bids.clear();
            book.asks.clear();
        }
    }

    private List<LimitOrder> sideList(int optionIndex, OrderSide side) {
        return side == OrderSide.BID ? bids(optionIndex) : asks(optionIndex);
    }
}
