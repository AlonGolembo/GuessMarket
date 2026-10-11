package com.guessmarket.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory log of every posted chat message, shared by every client. Like
 * the {@code MarketEngine}, there is no persistence - the chat disappears
 * when the server stops. {@code CopyOnWriteArrayList} since reads (every
 * client polling) vastly outnumber writes (a user sending a message).
 */
final class ChatStore {

    private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    ChatMessage post(String userName, String text) {
        ChatMessage message = new ChatMessage(nextId.getAndIncrement(), userName, text, System.currentTimeMillis());
        messages.add(message);
        return message;
    }

    List<ChatMessage> all() {
        return List.copyOf(messages);
    }
}
