package com.guessmarket.dto;

/**
 * @param filledQuantity shares actually bought; always the requested quantity
 *                       for LMSR, but may be less for Order Book if the ask
 *                       book ran out (a market buy never mints or rests)
 */
public record TradeResultDTO(
        double sharesCost,
        double commissionCost,
        double totalPaid,
        int filledQuantity,
        EventDetailsDTO updatedDetails
) {}
