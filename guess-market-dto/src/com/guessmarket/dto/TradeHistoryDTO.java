package com.guessmarket.dto;

public record TradeHistoryDTO(
        String timestamp,
        UserDTO buyer,
        String optionName,
        int quantity,
        double pricePaid
) {}