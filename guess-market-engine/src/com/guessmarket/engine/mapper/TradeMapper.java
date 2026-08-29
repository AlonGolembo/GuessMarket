package com.guessmarket.engine.mapper;

import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.TradeRecord;

public class TradeMapper {
    public static TradeHistoryDTO toTradeHistoryDTO(TradeRecord record) throws MarketException {
        return new TradeHistoryDTO(
                record.getBuyer(),
                record.getOptionName(),
                record.getQuantity(),
                record.getPricePaid()
        );
    }
}
