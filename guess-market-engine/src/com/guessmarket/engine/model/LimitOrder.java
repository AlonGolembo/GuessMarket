package com.guessmarket.engine.model;

import com.guessmarket.dto.OrderSide;

import java.io.Serializable;
import java.time.Instant;

/**
 * One resting or matched limit order in an {@link OrderBook}: an offer by
 * {@code userName} to buy or sell {@code quantity} shares of one option at
 * {@code price}. Mutable only in {@link #remaining} - it shrinks as the order
 * is filled and never grows back. Package-private: {@link OrderBook} creates
 * and orders these, {@link Event} matches them; nothing outside
 * {@code engine.model} needs to see one directly yet.
 */
final class LimitOrder implements Serializable {

    private final long id;
    private final String userName;
    private final int optionIndex;
    private final OrderSide side;
    private final int quantity;
    private int remaining;
    private final double price;
    private final Instant placedAt;

    LimitOrder(long id, String userName, int optionIndex, OrderSide side, int quantity, double price) {
        this.id = id;
        this.userName = userName;
        this.optionIndex = optionIndex;
        this.side = side;
        this.quantity = quantity;
        this.remaining = quantity;
        this.price = price;
        this.placedAt = Instant.now();
    }

    long getId() {
        return id;
    }

    String getUserName() {
        return userName;
    }

    int getOptionIndex() {
        return optionIndex;
    }

    OrderSide getSide() {
        return side;
    }

    int getQuantity() {
        return quantity;
    }

    int getRemaining() {
        return remaining;
    }

    double getPrice() {
        return price;
    }

    Instant getPlacedAt() {
        return placedAt;
    }

    boolean isFilled() {
        return remaining <= 0;
    }

    /** Reduces the unfilled quantity by {@code amount}, which must be in {@code (0, remaining]}. */
    void reduce(int amount) {
        if (amount <= 0 || amount > remaining) {
            throw new IllegalArgumentException(
                    "Cannot reduce order " + id + " by " + amount + " (remaining " + remaining + ").");
        }
        remaining -= amount;
    }

    @Override
    public String toString() {
        return "LimitOrder[" + id + " " + side + " " + remaining + "/" + quantity + " @ " + price
                + " by '" + userName + "']";
    }
}
