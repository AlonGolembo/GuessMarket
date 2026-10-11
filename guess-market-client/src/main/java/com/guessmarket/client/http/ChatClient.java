package com.guessmarket.client.http;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.guessmarket.engine.exception.MarketException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Talks to the global chat endpoints ({@code /api/chat}) over HTTP/JSON.
 * Deliberately self-contained rather than sharing plumbing with
 * {@link HttpMarketEngine} - same reasoning {@link WireRequests} documents
 * for keeping client/server wire shapes separate rather than coupled.
 */
public final class ChatClient {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private static final Type MESSAGE_LIST_TYPE = new TypeToken<List<ChatMessage>>() {}.getType();

    public ChatClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** One posted chat message. */
    public record ChatMessage(long id, String userName, String text, long timestampMillis) {}

    /** Full history, oldest first. */
    public List<ChatMessage> fetchMessages() {
        HttpRequest request = HttpRequest.newBuilder(uri("/chat")).GET().build();
        return gson.fromJson(execute(request).body(), MESSAGE_LIST_TYPE);
    }

    /** Posts a message as {@code userName}; returns it as the server recorded it (id + timestamp assigned). */
    public ChatMessage postMessage(String userName, String text) {
        HttpRequest request = HttpRequest.newBuilder(uri("/chat"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        gson.toJson(new WireRequests.PostChatMessageRequest(userName, text)), StandardCharsets.UTF_8))
                .build();
        return gson.fromJson(execute(request).body(), ChatMessage.class);
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    /** Sends the request; a non-2xx response becomes a {@link MarketException} carrying the server's error message. */
    private HttpResponse<String> execute(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new MarketException("Could not reach the server: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketException("Request interrupted.", e);
        }
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response;
        }
        throw new MarketException(extractErrorMessage(response.body()));
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Request failed.";
        }
        try {
            WireResponses.ErrorResponse error = gson.fromJson(JsonParser.parseString(body), WireResponses.ErrorResponse.class);
            if (error != null && error.error() != null) {
                return error.error();
            }
        } catch (RuntimeException ignored) {
            // Not the JSON error shape we expected - fall through and surface the raw body.
        }
        return body;
    }
}
