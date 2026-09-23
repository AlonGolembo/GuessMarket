package com.guessmarket.server.dto;

/** POST /api/users/deposit body. */
public record DepositRequest(String userName, double amount) {}
