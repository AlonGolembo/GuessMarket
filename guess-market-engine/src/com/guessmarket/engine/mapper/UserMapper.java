package com.guessmarket.engine.mapper;

import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.Option;
import com.guessmarket.engine.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Converts a domain {@link User} to its DTOs. One direction only. */
public final class UserMapper {

    private UserMapper() {}

    public static UserDTO toUserDTO(User user) {
        return new UserDTO(
                user.getName(),
                user.getAccountBalance(),
                Set.copyOf(user.getMarketMakerEventIds()),
                holdings(user)
        );
    }

    /** The user's summary plus their full balance history, oldest entry first. */
    public static UserDetailsDTO toUserDetailsDTO(User user) {
        return new UserDetailsDTO(
                toUserDTO(user),
                user.getAccount().ledgerEntries().stream()
                        .map(LedgerMapper::toLedgerEntryDTO)
                        .toList()
        );
    }

    /** eventId -&gt; (optionName -&gt; shares held), covering every event the user participates in. */
    private static Map<Integer, Map<String, Integer>> holdings(User user) {
        Map<Integer, Map<String, Integer>> byEvent = new LinkedHashMap<>();
        for (Event event : user.getParticipatingEvents().values()) {
            int[] held = event.holdingsOf(user.getName());
            Map<String, Integer> byOption = new LinkedHashMap<>();
            List<Option> options = event.getOptions();
            for (int i = 0; i < options.size(); i++) {
                byOption.put(options.get(i).getName(), held[i]);
            }
            byEvent.put(event.getId(), Map.copyOf(byOption));
        }
        return byEvent;
    }
}
