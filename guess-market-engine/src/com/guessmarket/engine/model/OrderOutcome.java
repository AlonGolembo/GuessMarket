package com.guessmarket.engine.model;

/**
 * Result of {@link Event#placeOrder}: how much filled immediately by matching
 * against the book, and how much (if any) is now resting under
 * {@code orderId}. The two always sum to the requested quantity - a fully
 * matched order has {@code restingQuantity() == 0}; an order that never
 * crossed has {@code filledQuantity() == 0}.
 *
 * @param cashMoved  total dollars that changed hands for the placer across
 *                   every fill this call produced (direct match and mint) -
 *                   paid if they were buying, received if they were selling
 * @param commission commission the placer themselves paid within
 *                   {@code cashMoved} (0 if they were selling in a direct
 *                   match, since the buyer pays on-purchase commission)
 */
public record OrderOutcome(long orderId, int filledQuantity, int restingQuantity, double cashMoved, double commission) {
}
