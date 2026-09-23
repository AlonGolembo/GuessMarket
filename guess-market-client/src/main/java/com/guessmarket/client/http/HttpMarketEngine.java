package com.guessmarket.client.http;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.NewEventDTO;
import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.exception.MarketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A {@link MarketEngine} that talks to a running {@code guess-market-server}
 * over HTTP/JSON instead of holding the domain model in-process. Every method
 * is a request/response round trip; there is no server-pushed notification, so
 * this class polls instead - see {@link #startPolling()} - and re-fires the
 * same {@link MarketDataChangeListener} callback the controllers already use,
 * so they need no changes to work against either engine.
 */
public final class HttpMarketEngine implements MarketEngine {

    public static final String DEFAULT_BASE_URL = "http://localhost:8080/guess-market-server/api";

    /** Recommended by the exercise spec: at most 2s between polls, ~0.5s is typical. */
    private static final long POLL_INTERVAL_MILLIS = 500;

    private static final Logger LOG = LogManager.getLogger(HttpMarketEngine.class);
    private static final Type EVENT_LIST_TYPE = new TypeToken<List<EventDTO>>() {}.getType();
    private static final Type USER_MAP_TYPE = new TypeToken<Map<String, UserDTO>>() {}.getType();

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final List<MarketDataChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "guessmarket-poll");
        thread.setDaemon(true);
        return thread;
    });

    public HttpMarketEngine() {
        this(DEFAULT_BASE_URL);
    }

    public HttpMarketEngine(String baseUrl) {
        this.baseUrl = baseUrl;
        startPolling();
    }

    private void startPolling() {
        poller.scheduleWithFixedDelay(() -> {
            try {
                notifyListeners();
            } catch (RuntimeException e) {
                // A transient failure (server briefly down, etc.) must not kill the poll loop.
                LOG.warn("Poll tick failed: {}", e.getMessage());
            }
        }, POLL_INTERVAL_MILLIS, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void notifyListeners() {
        for (MarketDataChangeListener listener : listeners) {
            listener.onMarketDataChanged();
        }
    }

    @Override
    public void addListener(MarketDataChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(MarketDataChangeListener listener) {
        listeners.remove(listener);
    }

    // --- Loading & uploads --------------------------------------------------

    @Override
    public void loadXmlFile(String filePath, String uploaderName) {
        String content;
        try {
            content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MarketException("Could not read file: " + filePath + " (" + e.getMessage() + ")", e);
        }
        uploadEventsXml(uploaderName, content);
    }

    @Override
    public List<String> uploadEventsXml(String uploaderName, String xmlContent) {
        String boundary = "----GuessMarketBoundary" + System.nanoTime();
        byte[] body = multipartBody(boundary, uploaderName, xmlContent);
        HttpRequest request = HttpRequest.newBuilder(uri("/events/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        WireResponses.UploadResult result = gson.fromJson(execute(request).body(), WireResponses.UploadResult.class);
        return result.addedEventNames();
    }

    private static byte[] multipartBody(String boundary, String uploaderName, String xmlContent) {
        String crlf = "\r\n";
        String body = "--" + boundary + crlf
                + "Content-Disposition: form-data; name=\"uploaderName\"" + crlf + crlf
                + uploaderName + crlf
                + "--" + boundary + crlf
                + "Content-Disposition: form-data; name=\"file\"; filename=\"upload.xml\"" + crlf
                + "Content-Type: text/xml; charset=UTF-8" + crlf + crlf
                + xmlContent + crlf
                + "--" + boundary + "--" + crlf;
        return body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isFileLoaded() {
        return !getAllEvents().isEmpty();
    }

    // --- Users ---------------------------------------------------------------

    @Override
    public UserDTO login(String name) {
        return postForJson("/users/login", new WireRequests.LoginRequest(name), UserDTO.class);
    }

    @Override
    public void deposit(String userName, double amount) {
        postNoContent("/users/deposit", new WireRequests.DepositRequest(userName, amount));
    }

    @Override
    public Map<String, UserDTO> getAllUsers() {
        return getJson("/users", USER_MAP_TYPE);
    }

    @Override
    public UserDetailsDTO getUserDetails(String name) {
        return getJson("/users/details?name=" + encode(name), UserDetailsDTO.class);
    }

    // --- Events ----------------------------------------------------------------

    @Override
    public List<EventDTO> getAllEvents() {
        return getJson("/events", EVENT_LIST_TYPE);
    }

    @Override
    public List<EventDTO> getActiveEvents() {
        return getJson("/events/active", EVENT_LIST_TYPE);
    }

    @Override
    public EventDetailsDTO getEventDetails(String eventName) {
        return getJson("/events/details?name=" + encode(eventName), EventDetailsDTO.class);
    }

    @Override
    public EventDTO createEvent(NewEventDTO spec) {
        return postForJson("/events", spec, EventDTO.class);
    }

    @Override
    public void activateEvent(String eventName, String userName) {
        postNoContent("/events/activate", new WireRequests.ActivateEventRequest(eventName, userName));
    }

    @Override
    public void closeEvent(String eventName, int winningOptionIndex1Based) {
        postNoContent("/events/close", new WireRequests.CloseEventRequest(eventName, winningOptionIndex1Based));
    }

    // --- Trading -----------------------------------------------------------

    @Override
    public TradeQuoteDTO quoteTrade(String userName, String eventName, int optionIndex1Based, int quantity) {
        return postForJson("/trading/quote",
                new WireRequests.TradeRequest(userName, eventName, optionIndex1Based, quantity), TradeQuoteDTO.class);
    }

    @Override
    public TradeResultDTO buyShares(String userName, String eventName, int optionIndex1Based, int quantity) {
        return postForJson("/trading/buy",
                new WireRequests.TradeRequest(userName, eventName, optionIndex1Based, quantity), TradeResultDTO.class);
    }

    @Override
    public OrderResultDTO placeOrder(String userName, String eventName, int optionIndex1Based, OrderSide side,
                                      int quantity, double price) {
        return postForJson("/trading/place-order",
                new WireRequests.PlaceOrderRequest(userName, eventName, optionIndex1Based, side, quantity, price),
                OrderResultDTO.class);
    }

    @Override
    public void cancelOrder(String userName, String eventName, long orderId) {
        postNoContent("/trading/cancel-order", new WireRequests.CancelOrderRequest(userName, eventName, orderId));
    }

    // --- Not supported over HTTP: the server holds no persistence -----------

    @Override
    public void saveState(String filePath) {
        throw new MarketException("Save/load state isn't available in the client-server build.");
    }

    @Override
    public void loadState(String filePath) {
        throw new MarketException("Save/load state isn't available in the client-server build.");
    }

    @Override
    public int getNumOfLoadedEvents() {
        return getAllEvents().size();
    }

    // --- HTTP plumbing -------------------------------------------------------

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private <T> T getJson(String path, Type type) {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return gson.fromJson(execute(request).body(), type);
    }

    private <T> T postForJson(String path, Object requestBody, Type responseType) {
        HttpResponse<String> response = post(path, requestBody);
        return gson.fromJson(response.body(), responseType);
    }

    private void postNoContent(String path, Object requestBody) {
        post(path, requestBody);
    }

    private HttpResponse<String> post(String path, Object requestBody) {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody), StandardCharsets.UTF_8))
                .build();
        return execute(request);
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
