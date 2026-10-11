package com.guessmarket.client.controllers;

import com.guessmarket.client.http.ChatClient;
import com.guessmarket.engine.exception.MarketException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Backs the chat window: a scrolling history and a compose field. Polls
 * {@code GET /api/chat} on the same cadence {@code HttpMarketEngine} already
 * uses for market data, but only while this window is open - unlike that
 * engine-wide poller, which lives for the whole app run, this one starts
 * when the window shows and stops when it closes.
 */
public class ChatController {

    private static final Logger LOG = LogManager.getLogger(ChatController.class);
    private static final long POLL_INTERVAL_MILLIS = 500;

    @FXML private ListView<String> messagesList;
    @FXML private TextField messageField;

    private final ObservableList<String> items = FXCollections.observableArrayList();
    private ChatClient chatClient;
    private String currentUserName;
    private long lastRenderedId = 0;
    private ScheduledExecutorService poller;

    @FXML
    private void initialize() {
        messagesList.setItems(items);
    }

    public void setContext(ChatClient chatClient, String currentUserName) {
        this.chatClient = chatClient;
        this.currentUserName = currentUserName;
    }

    public void startPolling() {
        if (poller != null) {
            return;
        }
        poller = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "guessmarket-chat-poll");
            thread.setDaemon(true);
            return thread;
        });
        poller.scheduleWithFixedDelay(this::pollOnce, 0, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void stopPolling() {
        if (poller != null) {
            poller.shutdownNow();
            poller = null;
        }
    }

    private void pollOnce() {
        try {
            List<ChatClient.ChatMessage> messages = chatClient.fetchMessages();
            Platform.runLater(() -> render(messages));
        } catch (RuntimeException e) {
            // A transient failure (server briefly down, etc.) must not kill the poll loop.
            LOG.warn("Chat poll tick failed: {}", e.getMessage());
        }
    }

    private void render(List<ChatClient.ChatMessage> messages) {
        long newestId = messages.isEmpty() ? 0 : messages.get(messages.size() - 1).id();
        // Skip stale polls that raced behind an optimistic append from handleSend().
        if (newestId <= lastRenderedId) {
            return;
        }
        items.setAll(messages.stream().map(this::format).toList());
        lastRenderedId = newestId;
        messagesList.scrollTo(items.size() - 1);
    }

    private String format(ChatClient.ChatMessage message) {
        return message.userName() + ": " + message.text();
    }

    @FXML
    private void handleSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            ChatClient.ChatMessage posted = chatClient.postMessage(currentUserName, text);
            messageField.clear();
            // Render immediately rather than waiting for the next poll tick.
            items.add(format(posted));
            lastRenderedId = posted.id();
            messagesList.scrollTo(items.size() - 1);
        } catch (MarketException e) {
            LOG.warn("Chat message rejected: {}", e.getMessage());
        }
    }
}
