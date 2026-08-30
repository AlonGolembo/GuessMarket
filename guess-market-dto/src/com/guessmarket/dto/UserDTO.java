package com.guessmarket.dto;

import java.util.Set;

/**
 * Snapshot of a user. Events are referenced by id only, so the DTO graph stays
 * flat (an {@link EventDTO} never points back at a {@code UserDTO}).
 *
 * @param balance             current cash balance
 * @param marketMakerEventIds ids of events this user is the market maker for
 * @param participatingEventIds ids of events this user has traded in
 */
public record UserDTO(
        String name,
        double balance,
        Set<Integer> marketMakerEventIds,
        Set<Integer> participatingEventIds
) {}
