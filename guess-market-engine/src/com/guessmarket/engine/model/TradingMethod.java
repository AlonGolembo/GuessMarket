package com.guessmarket.engine.model;

import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.engine.exception.XmlValidationException;

import java.io.Serializable;
import java.util.List;

/**
 * How an event prices its options, what a trade costs, and what the market maker
 * must post to open it. This is the single polymorphic seam for method-specific
 * behaviour: callers ask the method, they never switch on its concrete type.
 *
 * <p>Implementations are immutable value objects and part of the serialized
 * market state. Prices and costs are computed from the live option list, which
 * the caller passes in; the method stores none of that state itself.
 *
 * <p>The option list is always exactly two entries (binary market), ordered as
 * declared in the XML. {@code optionIndex} is 0-based.
 */
public sealed interface TradingMethod extends Serializable permits LmsrMethod, OrderBookMethod {

    TradingMethodType type();

    /** Cash the market maker posts to open an event priced by this method. */
    double initialSubsidy();

    /**
     * The payout per share of the winning option once the event settles; every
     * pair of options (one of each) is worth exactly this much. {@code 1.0} for
     * LMSR; the configured base value ({@code d}) for Order Book.
     */
    double baseValue();

    /**
     * Shares of each option the market maker receives when opening the event
     * (beyond the cash subsidy). {@code 0} for LMSR; the configured initial
     * allocation for Order Book, where the market maker receives that many of
     * *each* option.
     */
    int initialShares();

    /**
     * Current implied price of the option at {@code optionIndex}: a probability
     * in {@code [0, 1]} for scoring-rule methods.
     *
     * @throws UnsupportedOperationException if this method has no pricing yet
     */
    double priceOf(int optionIndex, List<Option> options);

    /**
     * Cost to buy {@code quantity} shares of the option at {@code optionIndex},
     * given the current option list.
     *
     * @throws UnsupportedOperationException if this method has no trading yet
     */
    double costToBuy(int optionIndex, int quantity, List<Option> options);

    /**
     * Validates this method's configuration.
     *
     * @throws XmlValidationException if the configuration is unusable
     */
    void validate() throws XmlValidationException;
}
