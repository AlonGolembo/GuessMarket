package com.guessmarket.engine.mapper;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.Option;

import java.util.List;


public class EventMapper {

    public static EventDTO toEventDTO(Event event) {
        List<String> optionNames = event.getOptions().stream()
                .map(Option::getName)
                .toList();

        return new EventDTO(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPercentage(),
                event.getCommissionType().toXmlString(),
                optionNames,
                event.isActive()
        );
    }

    public static EventDetailsDTO toEventDetailsDTO(Event event) {
        // ... build details DTO
    }
}