package com.guessmarket.dto;

import java.util.List;
import java.util.Map;

public record EventDetailsDTO(
        EventDTO eventInfo,
        Map<String, Double> currentOptionPrices,  // e.g., {"Yes": 0.73, "No": 0.27}
        Map<String, Integer> totalSharesBought,    // e.g., {"Yes": 100, "No": 0}
        double eventAccountBalance,
        double totalCommissionCollected,
        List<TradeHistoryDTO> tradeHistory,       // Latest trade first
        String winningOption                      // null if active, option name if closed
) {}