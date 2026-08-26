package com.guessmarket.engine.mapper;

import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.User;

public class UserMapper {
    public static UserDTO toUserDTO(User user) throws MarketException {
        return new UserDTO(user.getName(), user.getInitialCash(), user.getEventsIdUserIsMM());
    }
}
