package com.guessmarket.server.dto;

import com.guessmarket.dto.OrderSide;

/** POST /api/trading/place-order body. */
public record PlaceOrderRequest(String userName, String eventName, int optionIndex1Based, OrderSide side,
                                 int quantity, double price) {}
