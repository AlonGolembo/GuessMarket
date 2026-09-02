package com.guessmarket.engine.mapper;

import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.engine.model.TradeRecord;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Converts a domain {@link TradeRecord} to its display DTO. One direction only. */
public final class TradeMapper {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

    private TradeMapper() {}

    public static TradeHistoryDTO toTradeHistoryDTO(TradeRecord record) {
        return new TradeHistoryDTO(
                record.getTimestamp().format(TIMESTAMP),
                record.getBuyerName(),
                record.getOptionName(),
                record.getQuantity(),
                record.getPricePaid()
        );
    }
}
