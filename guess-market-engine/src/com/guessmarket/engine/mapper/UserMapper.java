package com.guessmarket.engine.mapper;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.model.User;

import java.util.Map;
import java.util.stream.Collectors;

/** Converts a domain {@link User} to its DTO. One direction only. */
public final class UserMapper {

    private UserMapper() {}

    public static UserDTO toUserDTO(User user) {
        Map<Integer, EventDTO> participatingEvents = user.getParticipatingEvents().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> EventMapper.toEventDTO(entry.getValue())
                ));

        return new UserDTO(
                user.getName(),
                user.getAccountBalance(),
                user.getMarketMakerEventIds(),
                participatingEvents
        );
    }
}
