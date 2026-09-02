package com.guessmarket.engine.model;

import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.engine.exception.XmlValidationException;

import java.util.List;

/**
 * Order-book trading. The configuration is captured from the XML and validated;
 * pricing and trade execution are not implemented yet and throw
 * {@link UnsupportedOperationException} until they are.
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

    @Override
    public double initialSubsidy() {
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
    }

    @Override
    public String toString() {
        return "OrderBook[d=" + d + ", initial=" + initial + ", allowMint=" + allowMint + "]";
    }
}
