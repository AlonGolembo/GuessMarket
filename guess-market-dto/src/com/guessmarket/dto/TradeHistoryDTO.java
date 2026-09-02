package com.guessmarket.dto;

/** One row of an event's trade history, formatted for display. */
public record TradeHistoryDTO(
        String timestamp,
        String buyerName,
        String optionName,
        int quantity,
        double pricePaid
) {}
