package com.guessmarket.engine.model;

import java.io.Serializable;

/**
 * How an event prices its options and what it costs the market maker to open it.
 * Implementations are value objects and part of the serialized market state.
 *
 * <p>Phase 2 widens this into the full pricing seam
 * ({@code priceOf}, {@code costToBuy}, {@code validate}).
 */
public sealed interface ITradingMethod extends Serializable permits LmsrMethod, OrderBookMethod {

    TradingMethodType getType();

    /** Cash the market maker must post to open an event using this method. */
    Double getInitialSubsidy();
}
