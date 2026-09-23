package com.guessmarket.client.http;

import com.guessmarket.dto.OrderSide;

/**
 * Request bodies {@link HttpMarketEngine} sends. Mirror the shapes
 * {@code guess-market-server}'s servlets expect (see its own
 * {@code com.guessmarket.server.dto} package) - kept separate rather than
 * shared, since the server module isn't something a desktop client depends on.
 */
final class WireRequests {

    private WireRequests() {}

    record LoginRequest(String name) {}

    record DepositRequest(String userName, double amount) {}

    record ActivateEventRequest(String eventName, String userName) {}

    record CloseEventRequest(String eventName, int winningOptionIndex1Based) {}

    record TradeRequest(String userName, String eventName, int optionIndex1Based, int quantity) {}

    record PlaceOrderRequest(String userName, String eventName, int optionIndex1Based, OrderSide side,
                              int quantity, double price) {}

    record CancelOrderRequest(String userName, String eventName, long orderId) {}
}
