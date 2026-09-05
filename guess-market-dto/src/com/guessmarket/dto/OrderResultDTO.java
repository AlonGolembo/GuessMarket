package com.guessmarket.dto;

/**
 * Result of placing a limit order on an Order Book event.
 *
 * @param filledQuantity filled immediately by matching (direct match + mint)
 * @param restingQuantity left resting in the book under {@code restingOrderId}
 *                       ({@code null} restingOrderId or 0 quantity if none)
 * @param cashMoved      dollars that changed hands for the placer across every
 *                       fill - paid if buying, received if selling
 * @param commission     commission the placer themselves paid within
 *                       {@code cashMoved}
 */
public record OrderResultDTO(
        int filledQuantity,
        int restingQuantity,
        double cashMoved,
        double commission,
        Long restingOrderId,
        EventDetailsDTO updatedDetails
) {}
