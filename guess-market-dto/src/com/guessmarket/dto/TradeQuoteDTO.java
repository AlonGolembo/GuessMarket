package com.guessmarket.dto;

/**
 * A non-binding price quote for a prospective trade: what the buyer would pay if
 * they executed it right now. The UI shows these numbers, and {@code buyShares}
 * charges exactly the same breakdown.
 *
 * @param sharesCost     price of the shares themselves
 * @param commission     fee charged now (0 when the event charges on close)
 * @param total          {@code sharesCost + commission}
 * @param filledQuantity shares this would actually buy; always the requested
 *                       quantity for LMSR, but may be less for Order Book if
 *                       the ask book can't fill the whole request
 */
public record TradeQuoteDTO(double sharesCost, double commission, double total, int filledQuantity) {}
