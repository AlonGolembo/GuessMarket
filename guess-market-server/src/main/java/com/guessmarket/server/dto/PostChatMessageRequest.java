package com.guessmarket.server.dto;

/** POST /api/chat body. */
public record PostChatMessageRequest(String userName, String text) {}
