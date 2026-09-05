package com.guessmarket.dto;

/**
 * One row of an Order Book's depth display: every resting order on one
 * option/side at the same {@code price}, aggregated into a single quantity.
 */
public record OrderBookLevelDTO(String optionName, OrderSide side, double price, int quantity) {}
