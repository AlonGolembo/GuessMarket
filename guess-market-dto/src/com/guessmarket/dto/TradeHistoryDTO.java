package com.guessmarket.dto;

public record TradeHistoryDTO(
        String optionName,
        int quantity,
        double pricePaid
) {}