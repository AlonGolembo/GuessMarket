package com.guessmarket.engine.model;

public enum CommissionType implements java.io.Serializable {
    ON_PURCHASE,
    ON_CLOSE;

    public static CommissionType fromXmlString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Commission type cannot be null");
        }
        return switch (value.trim().toLowerCase()) {
            case "on-purchase" -> ON_PURCHASE;
            case "on-close" -> ON_CLOSE;
            default -> throw new IllegalArgumentException("Unknown commission type: " + value);
        };
    }

    public String toXmlString() {
        return switch (this) {
            case ON_PURCHASE -> "on-purchase";
            case ON_CLOSE -> "on-close";
        };
    }
}