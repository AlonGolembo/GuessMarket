package com.guessmarket.dto;

import java.util.Map;
import java.util.Set;

/**
 * Snapshot of a user. Events are referenced by id only, so the DTO graph stays
 * flat (an {@link EventDTO} never points back at a {@code UserDTO}).
 *
 * @param balance             current cash balance
 * @param marketMakerEventIds ids of events this user is the market maker for
 * @param holdings            eventId -&gt; (optionName -&gt; shares held) for every event
 *                            the user participates in; {@code holdings.keySet()} is
 *                            the set of participating event ids
 */
public record UserDTO(
        String name,
        double balance,
        Set<Integer> marketMakerEventIds,
        Map<Integer, Map<String, Integer>> holdings
) {}
