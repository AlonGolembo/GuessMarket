package com.guessmarket.dto;

public record TradeResultDTO(
        double sharesCost,
        double commissionCost,
        double totalPaid,
        EventDetailsDTO updatedDetails
) {}