package com.guessmarket.server;

/** One posted chat message. {@code timestampMillis} (not {@code Instant}) so plain Gson needs no custom adapter. */
record ChatMessage(long id, String userName, String text, long timestampMillis) {
}
