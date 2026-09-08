package com.guessmarket.engine.model;

/**
 * What a single {@link Event#buy} transaction cost, broken out for the receipt
 * the caller shows the buyer.
 *
 * @param sharesCost     the price of the shares themselves (from the trading method)
 * @param commission     the fee charged now (0 when the event charges on close)
 * @param totalPaid      {@code sharesCost + commission}, the amount debited from the buyer
 * @param filledQuantity shares actually bought; always the requested quantity for
 *                       LMSR, but may be less for Order Book if the book runs out
 *                       of asks (a market buy never mints or rests a remainder)
 */
public record TradeReceipt(double sharesCost, double commission, double totalPaid, int filledQuantity) {}
