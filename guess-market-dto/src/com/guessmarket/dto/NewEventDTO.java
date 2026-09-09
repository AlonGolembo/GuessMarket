package com.guessmarket.dto;

import java.util.List;

/**
 * A request to create a new event, as filled in by the user.
 *
 * <p>Method-specific parameters are nullable and only one set is read:
 * {@code lmsrB} for {@link TradingMethodType#LMSR}; {@code orderBookD},
 * {@code orderBookInitial} and {@code orderBookAllowMint} for
 * {@link TradingMethodType#ORDERBOOK}.
 *
 * @param marketMakerName    name of the participant who will be the market maker
 * @param optionNames        exactly two option names
 */
public record NewEventDTO(
        String marketMakerName,
        String name,
        String description,
        int commissionPercentage,
        CommissionType commissionType,
        List<String> optionNames,
        TradingMethodType tradingMethod,
        Integer lmsrB,
        Integer orderBookD,
        Integer orderBookInitial,
        boolean orderBookAllowMint
) {}
