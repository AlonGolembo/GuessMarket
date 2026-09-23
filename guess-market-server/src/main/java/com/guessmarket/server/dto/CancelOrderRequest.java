package com.guessmarket.server.dto;

/** POST /api/trading/cancel-order body. */
public record CancelOrderRequest(String userName, String eventName, long orderId) {}
