package com.guessmarket.server;

import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.exception.MarketException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Common plumbing every servlet needs: the shared engine, and one place that
 * turns a rejected request into a 400 and anything unexpected into a 500,
 * so individual servlets read as just the request/response mapping.
 */
abstract class BaseServlet extends HttpServlet {

    private static final Logger LOG = LogManager.getLogger(BaseServlet.class);

    MarketEngine engine(HttpServletRequest request) {
        return (MarketEngine) request.getServletContext().getAttribute(GuessMarketContextListener.ENGINE_ATTRIBUTE);
    }

    @FunctionalInterface
    interface Action {
        void run() throws IOException;
    }

    /** Requires a non-blank value (e.g. a required query parameter); rejects like any other bad request. */
    static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new MarketException("'" + fieldName + "' is required.");
        }
        return value;
    }

    void handle(HttpServletRequest request, HttpServletResponse response, Action action) throws IOException {
        try {
            action.run();
        } catch (MarketException e) {
            LOG.warn("Rejected {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), e);
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
}
