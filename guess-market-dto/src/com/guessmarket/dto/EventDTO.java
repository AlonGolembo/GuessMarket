package com.guessmarket.dto;

import java.util.List;

/** Summary snapshot of an event. Enum-like fields ({@code commissionType},
 *  {@code tradingMethod}, {@code status}) are carried as their enum name. */
public record EventDTO(
        int id,
        String name,
        String description,
        int commissionPercentage,
        String commissionType,
        List<String> options,
        String tradingMethod,
        String status
) {}
