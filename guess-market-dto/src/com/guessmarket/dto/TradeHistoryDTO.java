package com.guessmarket.dto;

public record TradeHistoryDTO(
        UserDTO buyer,
        String optionName,
        int quantity,
        double pricePaid
) {}