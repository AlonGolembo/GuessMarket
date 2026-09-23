package com.guessmarket.server.dto;

/** POST /api/trading/quote and POST /api/trading/buy body. */
public record TradeRequest(String userName, String eventName, int optionIndex1Based, int quantity) {}
