package com.guessmarket.server.dto;

/** POST /api/events/close body. */
public record CloseEventRequest(String eventName, int winningOptionIndex1Based) {}
