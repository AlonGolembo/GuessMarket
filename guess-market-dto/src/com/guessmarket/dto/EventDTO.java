package com.guessmarket.dto;

import java.util.List;

/** Summary snapshot of an event. */
public record EventDTO(
        int id,
        String name,
        String description,
        int commissionPercentage,
        CommissionType commissionType,
        List<String> options,
        TradingMethodType tradingMethod,
        EventStatus status
) {}
