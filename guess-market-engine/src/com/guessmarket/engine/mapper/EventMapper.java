package com.guessmarket.engine.mapper;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.lmsr.LmsrCalculator;
import com.guessmarket.engine.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class EventMapper {

    public static EventDTO toEventDTO(Event event) {
        List<String> optionNames = event.getOptions().stream()
                .map(Option::getName)
                .toList();
        List<UserDTO> users = event.getUsers()
                .values().stream()
                .map(UserMapper::toUserDTO)
                .toList();

        return new EventDTO(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPercentage(),
                event.getCommissionType().toXmlString(),
                optionNames,
                event.getTradingMethod().getType().toString(),
                event.getStatus(),
                users
        );
    }

    public static EventDetailsDTO toEventDetailsDTO(Event event) {
        EventDTO baseInfo = toEventDTO(event);

        // Extract shares bought per option
        Map<String, Integer> sharesBoughtMap = new LinkedHashMap<>();
        for (Option option : event.getOptions()) {
            sharesBoughtMap.put(option.getName(), option.getSharesBought());
        }

        // Calculate current LMSR probability prices per option
        Map<String, Double> pricesMap = new LinkedHashMap<>();
        List<Option> options = event.getOptions();

        if (options.size() >= 2) {
            int qYes = options.get(0).getSharesBought();
            int qNo = options.get(1).getSharesBought();

            // HACK: Temporary use this switch case to continue only with LMSR method
            // FIXME: Refactor when implement Order-Book method
            int b = switch (event.getTradingMethod()) {
                case LmsrMethod lmsr -> lmsr.getB();
                case OrderBookMethod ob -> 0; // Order books don't have b
            };

            double pYes = LmsrCalculator.calculateOptionPrice(qYes, qNo, b);
            double pNo = LmsrCalculator.calculateOptionPrice(qNo, qYes, b);

            pricesMap.put(options.get(0).getName(), pYes);
            pricesMap.put(options.get(1).getName(), pNo);
        }

         // Map trade history records
        List<TradeHistoryDTO> tradeHistory = new ArrayList<>();
        if (event.getTradeHistory() != null) {
            for (TradeRecord record : event.getTradeHistory()) {
                tradeHistory.add(TradeMapper.toTradeHistoryDTO(record));
            }
        }

        // Winning option name (if event is closed)
        String winningOptionName = (event.getWinningOption() != null)
                ? event.getWinningOption().getName()
                : null;

        return new EventDetailsDTO(
                baseInfo,
                pricesMap,
                sharesBoughtMap,
                event.getEventAccountBalance(),
                event.getTotalCommissionCollected(),
                tradeHistory,
                winningOptionName
        );
    }
}