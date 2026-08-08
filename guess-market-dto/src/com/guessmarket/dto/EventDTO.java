package com.guessmarket.dto;

import java.util.List;
import java.util.Map;

public record EventDTO(
        int id,
        String name,
        String description,
        int commissionPercentage,
        String commissionType,   // "on-purchase" or "on-close"[cite: 1]
        List<String> options,
        boolean isActive) {}