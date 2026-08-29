package com.guessmarket.dto;

import java.util.List;
import java.util.Map;

public record EventDTO(
        int id,
        String name,
        String description,
        int commissionPercentage,
        String commissionType,
        List<String> options,
        String tradingMethod,
        boolean isActive,
        List<UserDTO> users) {}