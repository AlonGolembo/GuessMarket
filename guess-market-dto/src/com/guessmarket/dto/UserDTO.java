package com.guessmarket.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record UserDTO(
        String name,
        Double initialCash,
        Set<Integer> eventsIdUserIsMM,
        Map<Integer, EventDTO> participatingEvents) {}
