package com.guessmarket.dto;

import java.util.Set;

public record UserDTO(
        String name,
        int initialCash,
        Set<Integer> eventsIdUserIsMM) {}
