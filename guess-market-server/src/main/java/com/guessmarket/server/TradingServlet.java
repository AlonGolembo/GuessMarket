package com.guessmarket.server;

import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.server.dto.CancelOrderRequest;
import com.guessmarket.server.dto.PlaceOrderRequest;
import com.guessmarket.server.dto.TradeRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Trading commands: price a trade, buy (LMSR/market order), place/cancel a limit order. */
@WebServlet(urlPatterns = "/api/trading/*")
public class TradingServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path.");
            return;
        }
        switch (pathInfo) {
            case "/quote" -> handle(request, response, () -> {
                TradeRequest body = JsonUtil.readBody(request, TradeRequest.class);
                TradeQuoteDTO quote = engine(request)
                        .quoteTrade(body.userName(), body.eventName(), body.optionIndex1Based(), body.quantity());
                JsonUtil.writeJson(response, quote);
            });
            case "/buy" -> handle(request, response, () -> {
                TradeRequest body = JsonUtil.readBody(request, TradeRequest.class);
                TradeResultDTO result = engine(request)
                        .buyShares(body.userName(), body.eventName(), body.optionIndex1Based(), body.quantity());
                JsonUtil.writeJson(response, result);
            });
            case "/place-order" -> handle(request, response, () -> {
                PlaceOrderRequest body = JsonUtil.readBody(request, PlaceOrderRequest.class);
                OrderResultDTO result = engine(request).placeOrder(body.userName(), body.eventName(),
                        body.optionIndex1Based(), body.side(), body.quantity(), body.price());
                JsonUtil.writeJson(response, result);
            });
            case "/cancel-order" -> handle(request, response, () -> {
                CancelOrderRequest body = JsonUtil.readBody(request, CancelOrderRequest.class);
                engine(request).cancelOrder(body.userName(), body.eventName(), body.orderId());
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            });
            default -> JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + pathInfo);
        }
    }
}
