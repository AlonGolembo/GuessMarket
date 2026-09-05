package com.guessmarket.engine.mapper;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.HoldingDTO;
import com.guessmarket.dto.LimitOrderDTO;
import com.guessmarket.dto.OrderBookLevelDTO;
import com.guessmarket.dto.OrderBookQuoteDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.LimitOrder;
import com.guessmarket.engine.model.Option;
import com.guessmarket.engine.model.OrderBook;
import com.guessmarket.engine.model.TradeRecord;
import com.guessmarket.engine.model.TradingMethod;
import com.guessmarket.engine.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts a domain {@link Event} to its DTOs. One direction only. */
public final class EventMapper {

    private EventMapper() {}

    public static EventDTO toEventDTO(Event event) {
        List<String> optionNames = event.getOptions().stream()
                .map(Option::getName)
                .toList();

        return new EventDTO(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPercentage(),
                event.getCommissionType(),
                optionNames,
                event.getTradingMethod().type(),
                event.getStatus()
        );
    }

    public static EventDetailsDTO toEventDetailsDTO(Event event) {
        EventDTO baseInfo = toEventDTO(event);
        List<Option> options = event.getOptions();
        OrderBook orderBook = event.getOrderBook();

        Map<String, Integer> sharesOutstanding = new LinkedHashMap<>();
        for (Option option : options) {
            sharesOutstanding.put(option.getName(), option.getSharesOutstanding());
        }

        Map<String, Double> prices = currentPrices(event.getTradingMethod(), options, orderBook);

        List<TradeHistoryDTO> tradeHistory = new ArrayList<>();
        for (TradeRecord record : event.getTradeHistory()) {
            tradeHistory.add(TradeMapper.toTradeHistoryDTO(record));
        }

        String winningOptionName = event.getWinningOption() != null
                ? event.getWinningOption().getName()
                : null;

        return new EventDetailsDTO(
                baseInfo,
                prices,
                sharesOutstanding,
                event.getEventAccountBalance(),
                event.getTotalCommissionCollected(),
                tradeHistory,
                participantHoldings(event, options),
                winningOptionName,
                orderBookQuotes(orderBook, options),
                orderBookLevels(orderBook, options),
                restingOrders(orderBook, options)
        );
    }

    /** One {@link HoldingDTO} per (participant, option) the participant actually holds. */
    private static List<HoldingDTO> participantHoldings(Event event, List<Option> options) {
        List<HoldingDTO> holdings = new ArrayList<>();
        for (User participant : event.getParticipants().values()) {
            int[] held = event.holdingsOf(participant.getName());
            for (int i = 0; i < options.size(); i++) {
                if (held[i] > 0) {
                    holdings.add(new HoldingDTO(participant.getName(), options.get(i).getName(), held[i]));
                }
            }
        }
        return holdings;
    }

    /**
     * Current price per option. For Order Book this is the mid price (falling
     * back to the last trade, then half the base value); for a scoring-rule
     * method it's the method's own formula, or empty if it can't price yet.
     */
    private static Map<String, Double> currentPrices(TradingMethod method, List<Option> options, OrderBook orderBook) {
        Map<String, Double> prices = new LinkedHashMap<>();
        if (options.size() != 2) {
            return prices;
        }
        if (orderBook != null) {
            for (int i = 0; i < options.size(); i++) {
                prices.put(options.get(i).getName(), impliedPrice(orderBook, i, method.baseValue()));
            }
            return prices;
        }
        try {
            prices.put(options.get(0).getName(), method.priceOf(0, options));
            prices.put(options.get(1).getName(), method.priceOf(1, options));
        } catch (UnsupportedOperationException pricingNotImplemented) {
            prices.clear();
        }
        return prices;
    }

    private static double impliedPrice(OrderBook orderBook, int optionIndex, double baseValue) {
        Double bid = orderBook.bestBid(optionIndex).map(LimitOrder::getPrice).orElse(null);
        Double ask = orderBook.bestAsk(optionIndex).map(LimitOrder::getPrice).orElse(null);
        if (bid != null && ask != null) {
            return (bid + ask) / 2.0;
        }
        double lastTrade = orderBook.lastTradePrice(optionIndex);
        if (!Double.isNaN(lastTrade)) {
            return lastTrade;
        }
        return baseValue / 2.0;
    }

    /** The five order-book indicators per option; empty for a non-Order-Book event. */
    private static Map<String, OrderBookQuoteDTO> orderBookQuotes(OrderBook orderBook, List<Option> options) {
        Map<String, OrderBookQuoteDTO> quotes = new LinkedHashMap<>();
        if (orderBook == null) {
            return quotes;
        }
        for (int i = 0; i < options.size(); i++) {
            Double lastTrade = nanToNull(orderBook.lastTradePrice(i));
            Double bestBid = orderBook.bestBid(i).map(LimitOrder::getPrice).orElse(null);
            Double bestAsk = orderBook.bestAsk(i).map(LimitOrder::getPrice).orElse(null);
            Double mid = bestBid != null && bestAsk != null ? (bestBid + bestAsk) / 2.0 : null;
            Double spread = bestBid != null && bestAsk != null ? bestAsk - bestBid : null;
            quotes.put(options.get(i).getName(), new OrderBookQuoteDTO(lastTrade, bestBid, bestAsk, mid, spread));
        }
        return quotes;
    }

    /** The book's resting orders aggregated by (option, side, price) into depth rows; empty for LMSR. */
    private static List<OrderBookLevelDTO> orderBookLevels(OrderBook orderBook, List<Option> options) {
        List<OrderBookLevelDTO> levels = new ArrayList<>();
        if (orderBook == null) {
            return levels;
        }
        for (int i = 0; i < options.size(); i++) {
            String optionName = options.get(i).getName();
            levels.addAll(aggregateByPrice(optionName, OrderSide.BID, orderBook.bids(i)));
            levels.addAll(aggregateByPrice(optionName, OrderSide.ASK, orderBook.asks(i)));
        }
        return levels;
    }

    private static List<OrderBookLevelDTO> aggregateByPrice(String optionName, OrderSide side, List<LimitOrder> orders) {
        Map<Double, Integer> quantityByPrice = new LinkedHashMap<>();   // orders are already price-then-time ordered
        for (LimitOrder order : orders) {
            quantityByPrice.merge(order.getPrice(), order.getRemaining(), Integer::sum);
        }
        List<OrderBookLevelDTO> levels = new ArrayList<>();
        for (Map.Entry<Double, Integer> level : quantityByPrice.entrySet()) {
            levels.add(new OrderBookLevelDTO(optionName, side, level.getKey(), level.getValue()));
        }
        return levels;
    }

    /** Every individual resting order (unaggregated), e.g. for a user's own open-orders list; empty for LMSR. */
    private static List<LimitOrderDTO> restingOrders(OrderBook orderBook, List<Option> options) {
        List<LimitOrderDTO> resting = new ArrayList<>();
        if (orderBook == null) {
            return resting;
        }
        for (int i = 0; i < options.size(); i++) {
            String optionName = options.get(i).getName();
            for (LimitOrder order : orderBook.bids(i)) {
                resting.add(toLimitOrderDTO(order, optionName));
            }
            for (LimitOrder order : orderBook.asks(i)) {
                resting.add(toLimitOrderDTO(order, optionName));
            }
        }
        return resting;
    }

    private static LimitOrderDTO toLimitOrderDTO(LimitOrder order, String optionName) {
        return new LimitOrderDTO(order.getId(), order.getUserName(), optionName, order.getSide(),
                order.getQuantity(), order.getRemaining(), order.getPrice());
    }

    private static Double nanToNull(double value) {
        return Double.isNaN(value) ? null : value;
    }
}
