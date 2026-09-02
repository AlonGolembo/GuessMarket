package com.guessmarket.dto;

/**
 * One participant's holding of one option in an event: a flat
 * {@code (user, option, shares)} row.
 */
public record HoldingDTO(String userName, String optionName, int shares) {}