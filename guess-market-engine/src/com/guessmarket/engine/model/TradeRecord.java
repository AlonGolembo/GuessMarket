package com.guessmarket.engine.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class TradeRecord implements java.io.Serializable {
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final LocalDateTime timestamp;

    public TradeRecord(String optionName, int quantity, double pricePaid) {
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.timestamp = LocalDateTime.now();
    }

    public String getOptionName() { return optionName; }
    public int getQuantity() { return quantity; }
    public double getPricePaid() { return pricePaid; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TradeRecord that = (TradeRecord) o;
        return Objects.equals(optionName, that.optionName) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionName, timestamp);
    }
}