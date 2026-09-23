package com.guessmarket.server;

import com.google.gson.Gson;
import com.guessmarket.engine.exception.MarketException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Reader;

/** Gson (de)serialization for request/response bodies, shared by every servlet. */
final class JsonUtil {

    private static final Gson GSON = new Gson();

    private JsonUtil() {}

    static <T> T readBody(HttpServletRequest request, Class<T> type) throws IOException {
        T value;
        try (Reader reader = request.getReader()) {
            value = GSON.fromJson(reader, type);
        }
        if (value == null) {
            throw new MarketException("A request body is required.");
        }
        return value;
    }

    static void writeJson(HttpServletResponse response, Object value) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(value));
    }

    static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        writeJson(response, new ErrorResponse(message));
    }

    private record ErrorResponse(String error) {}
}
