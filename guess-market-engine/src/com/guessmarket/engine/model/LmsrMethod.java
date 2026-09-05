package com.guessmarket.engine.model;

import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.engine.exception.XmlValidationException;
import com.guessmarket.engine.lmsr.LmsrCalculator;

import java.util.List;

/**
 * Logarithmic Market Scoring Rule pricing. All the maths lives in
 * {@link LmsrCalculator}; this class adapts it to the {@link TradingMethod} seam.
 */
public final class LmsrMethod implements TradingMethod {

    /** Liquidity parameter: larger {@code b} = deeper market, smaller price moves. */
    private final int b;
    private final double initialSubsidy;

    public LmsrMethod(int b) {
        this.b = b;
        // An invalid b is reported by validate(); don't let the eager calc throw here.
        this.initialSubsidy = b > 0 ? LmsrCalculator.calculateInitialSubsidy(b) : Double.NaN;
    }

    public int b() {
        return b;
    }

    @Override
    public TradingMethodType type() {
        return TradingMethodType.LMSR;
    }

    @Override
    public double initialSubsidy() {
        return this.initialSubsidy;
    }

    @Override
    public double baseValue() {
        return 1.0;
    }

    @Override
    public int initialShares() {
        return 0;
    }

    @Override
    public double priceOf(int optionIndex, List<Option> options) {
        int qTarget = options.get(optionIndex).getSharesOutstanding();
        int qOther = options.get(1 - optionIndex).getSharesOutstanding();
        return LmsrCalculator.calculateOptionPrice(qTarget, qOther, b);
    }

    @Override
    public double costToBuy(int optionIndex, int quantity, List<Option> options) {
        int qFirst = options.get(0).getSharesOutstanding();
        int qSecond = options.get(1).getSharesOutstanding();
        return LmsrCalculator.calculateTradeCost(qFirst, qSecond, b, optionIndex == 0, quantity);
    }

    @Override
    public void validate() throws XmlValidationException {
        if (b <= 0) {
            throw new XmlValidationException("LMSR parameter 'b' must be strictly positive (> 0), got: " + b);
        }
    }

    @Override
    public String toString() {
        return "LMSR[b=" + b + "]";
    }
}
