/**
 * The domain model. {@link com.guessmarket.engine.model.Event} is the aggregate
 * root: it owns options, the cash pool, commission terms, trade history,
 * participants and their holdings, and enforces every rule about them. Money is
 * confined to {@link com.guessmarket.engine.model.Account}. Pricing lives behind
 * {@link com.guessmarket.engine.model.TradingMethod}.
 *
 * <p>Not exported from the module.
 */
package com.guessmarket.engine.model;
