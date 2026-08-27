package com.guessmarket.engine.mapper;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UserMapper {
    public static UserDTO toUserDTO(User user) throws MarketException {
        Map<Integer, EventDTO> participatingEvents = user.getParticipatingEvents().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> EventMapper.toEventDTO(entry.getValue())
                ));
        return new UserDTO(user.getName(), user.getInitialCash(), user.getEventsIdUserIsMM(), participatingEvents);
    }
}
