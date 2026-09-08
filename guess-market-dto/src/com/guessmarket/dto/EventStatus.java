package com.guessmarket.dto;

/** An event's lifecycle state: {@code NOT_ACTIVE -> ACTIVE -> CLOSED}. */
public enum EventStatus {
    NOT_ACTIVE,
    ACTIVE,
    CLOSED;

    public String toUIDisplay() {
        return switch (this){
            case NOT_ACTIVE -> "Not Active";
            case ACTIVE -> "Active";
            case CLOSED -> "Closed";
        };
    }
}
