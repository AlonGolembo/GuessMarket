package com.guessmarket.dto;

import java.util.List;
import java.util.Map;

/**
 * Full trading state of one event.
 *
 * @param currentOptionPrices  option name -&gt; implied price (empty if the method can't price yet)
 * @param totalSharesBought     option name -&gt; total shares the market has sold
 * @param tradeHistory          newest trade first
 * @param participantHoldings   one row per (user, option) the user actually holds (shares &gt; 0)
 * @param winningOption         option name once closed, otherwise {@code null}
 */
public record EventDetailsDTO(
        EventDTO eventInfo,
        Map<String, Double> currentOptionPrices,
        Map<String, Integer> totalSharesBought,
        double eventAccountBalance,
        double totalCommissionCollected,
        List<TradeHistoryDTO> tradeHistory,
        List<HoldingDTO> participantHoldings,
        String winningOption
) {}
