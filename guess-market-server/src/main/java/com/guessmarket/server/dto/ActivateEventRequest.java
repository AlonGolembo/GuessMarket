package com.guessmarket.server.dto;

/** POST /api/events/activate body. */
public record ActivateEventRequest(String eventName, String userName) {}
