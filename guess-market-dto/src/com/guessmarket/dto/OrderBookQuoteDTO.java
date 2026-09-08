package com.guessmarket.dto;

/**
 * The five price indicators an Order Book shows for one option, since it has
 * no single formula price the way LMSR does. Every field is {@code null}
 * until there's enough book activity to define it: {@code lastTrade} until
 * the first fill, {@code bestBid}/{@code bestAsk} while that side is empty,
 * {@code mid}/{@code spread} unless both a bid and an ask are resting.
 */
public record OrderBookQuoteDTO(Double lastTrade, Double bestBid, Double bestAsk, Double mid, Double spread) {}
