package com.guessmarket.dto;

/** When an event's commission is charged. */
public enum CommissionType {
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

    public String toUIDisplay(){
        return switch (this){
            case ON_PURCHASE -> "On Purchase";
            case ON_CLOSE -> "On Close";
        };
    }
}