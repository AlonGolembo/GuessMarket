package com.guessmarket.engine.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One entry in an {@link Event}'s trade audit log. A pure data holder: it records
 * who bought, what, how much and when, and knows nothing about DTOs or mappers.
 */
public class TradeRecord implements Serializable {

    private final String buyerName;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final LocalDateTime timestamp;

    public TradeRecord(String buyerName, String optionName, int quantity, double pricePaid) {
        this.buyerName = buyerName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.timestamp = LocalDateTime.now();
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPricePaid() {
        return pricePaid;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeRecord that = (TradeRecord) o;
        return Objects.equals(optionName, that.optionName)
                && Objects.equals(buyerName, that.buyerName)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buyerName, optionName, timestamp);
    }
}
