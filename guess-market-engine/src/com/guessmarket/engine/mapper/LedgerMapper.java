package com.guessmarket.engine.mapper;

import com.guessmarket.dto.LedgerEntryDTO;
import com.guessmarket.engine.model.LedgerEntry;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Converts a domain {@link LedgerEntry} to its display DTO. One direction only. */
public final class LedgerMapper {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

    private LedgerMapper() {}

    public static LedgerEntryDTO toLedgerEntryDTO(LedgerEntry entry) {
        return new LedgerEntryDTO(
                entry.getTimestamp().format(TIMESTAMP),
                entry.getDelta(),
                entry.getBalanceAfter(),
                entry.getType().name(),
                entry.getEventId()
        );
    }
}
