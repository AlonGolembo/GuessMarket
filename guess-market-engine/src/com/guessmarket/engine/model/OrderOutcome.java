package com.guessmarket.engine.model;

/**
 * Result of {@link Event#placeOrder}: how much filled immediately by matching
 * against the book, and how much (if any) is now resting under
 * {@code orderId}. The two always sum to the requested quantity - a fully
 * matched order has {@code restingQuantity() == 0}; an order that never
 * crossed has {@code filledQuantity() == 0}.
 */
public record OrderOutcome(long orderId, int filledQuantity, int restingQuantity) {
}
