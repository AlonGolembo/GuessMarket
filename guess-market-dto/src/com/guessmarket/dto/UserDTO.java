package com.guessmarket.dto;

import java.util.Map;
import java.util.Set;

/**
 * Snapshot of a user. Events are referenced by name only, so the DTO graph stays
 * flat (an {@link EventDTO} never points back at a {@code UserDTO}).
 *
 * @param balance               current cash balance (can be negative for a blocked user)
 * @param blocked               {@code true} once the balance went negative - the user
 *                              can no longer perform any action in the market
 * @param marketMakerEventNames names of events this user is the market maker for
 * @param holdings              eventName -&gt; (optionName -&gt; shares held) for every event
 *                              the user participates in; {@code holdings.keySet()} is
 *                              the set of participating event names
 */
public record UserDTO(
        String name,
        double balance,
        boolean blocked,
        Set<String> marketMakerEventNames,
        Map<String, Map<String, Integer>> holdings
) {}
