package com.guessmarket.engine.mapper;

import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.model.User;

import java.util.Set;

/** Converts a domain {@link User} to its flat DTO. One direction only. */
public final class UserMapper {

    private UserMapper() {}

    public static UserDTO toUserDTO(User user) {
        return new UserDTO(
                user.getName(),
                user.getAccountBalance(),
                user.getMarketMakerEventIds(),
                Set.copyOf(user.getParticipatingEvents().keySet())
        );
    }
}
