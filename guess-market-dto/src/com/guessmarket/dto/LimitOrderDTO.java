package com.guessmarket.dto;

/**
 * One resting order in an Order Book event, as shown to a user (e.g. their own
 * open-orders list).
 *
 * @param remaining how much of {@code quantity} is still unfilled
 */
public record LimitOrderDTO(
        long id,
        String userName,
        String optionName,
        OrderSide side,
        int quantity,
        int remaining,
        double price
) {}
