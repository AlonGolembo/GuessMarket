package com.guessmarket.engine.model;

import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.engine.exception.XmlValidationException;

import java.util.List;

/**
 * Order-book trading: a public book of bids/asks per option, matched
 * price-then-time, with share minting when cross-option demand covers the base
 * value. The configuration is captured from the XML and validated here; the
 * book, matching and minting themselves live on {@link Event} /
 * {@link OrderBook} (a follow-up phase). {@link #priceOf} / {@link #costToBuy}
 * still throw until that lands.
 *
 * @see #d() the base value: what a winning share pays out, and what a YES+NO
 *      pair is always worth together
 * @see #initial() shares of *each* option the market maker receives (and
 *      posts for sale) when opening the event
 * @see #allowMint() whether new share pairs may be minted from matching
 *      cross-option demand
 */
public final class OrderBookMethod implements TradingMethod {

    private final int d;
    private final int initial;
    private final boolean allowMint;

    public OrderBookMethod(int d, int initial, boolean allowMint) {
        this.d = d;
        this.initial = initial;
        this.allowMint = allowMint;
    }

    public int d() {
        return d;
    }

    public int initial() {
        return initial;
    }

    public boolean allowMint() {
        return allowMint;
    }

    @Override
    public TradingMethodType type() {
        return TradingMethodType.ORDERBOOK;
    }

    /** The market maker buys {@code initial} pairs (one of each option) at {@code d} each. */
    @Override
    public double initialSubsidy() {
        return initial * (double) d;
    }

    @Override
    public double baseValue() {
        return d;
    }

    @Override
    public int initialShares() {
        return initial;
    }

    @Override
    public double priceOf(int optionIndex, List<Option> options) {
        throw new UnsupportedOperationException("Order Book pricing is not implemented yet.");
    }

    @Override
    public double costToBuy(int optionIndex, int quantity, List<Option> options) {
        throw new UnsupportedOperationException("Order Book trading is not implemented yet.");
    }

    @Override
    public void validate() throws XmlValidationException {
        if (d <= 0) {
            throw new XmlValidationException("Order Book parameter 'd' must be strictly positive (> 0), got: " + d);
        }
        if (initial < 0) {
            throw new XmlValidationException("Order Book parameter 'initial' must be >= 0, got: " + initial);
        }
        if (initial == 0 && !allowMint) {
            throw new XmlValidationException(
                    "Order Book event has no initial allocation (initial=0) and does not allow minting "
                            + "(allow-mint=false); no shares could ever exist.");
        }
    }

    @Override
    public String toString() {
        return "OrderBook[d=" + d + ", initial=" + initial + ", allowMint=" + allowMint + "]";
    }
}
