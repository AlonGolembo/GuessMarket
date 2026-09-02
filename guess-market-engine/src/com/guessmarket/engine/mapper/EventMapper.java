package com.guessmarket.engine.mapper;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.HoldingDTO;
import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.Option;
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

        Map<String, Integer> sharesOutstanding = new LinkedHashMap<>();
        for (Option option : options) {
            sharesOutstanding.put(option.getName(), option.getSharesOutstanding());
        }

        Map<String, Double> prices = currentPrices(event.getTradingMethod(), options);

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
                winningOptionName
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
     * Current price per option, from the trading method. Empty if the method
     * cannot price yet (e.g. Order Book), so the caller can render "-".
     */
    private static Map<String, Double> currentPrices(TradingMethod method, List<Option> options) {
        Map<String, Double> prices = new LinkedHashMap<>();
        if (options.size() != 2) {
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
}
