package com.guessmarket.engine.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One line in an {@link Account}'s balance history: a single debit or credit,
 * the balance it left behind, why it happened and (when it stems from an event)
 * which one. A pure data holder - it knows nothing about DTOs or mappers.
 *
 * <p>{@code delta} is signed: negative for a debit, positive for a credit.
 * {@code eventId} is {@code null} for {@link LedgerEntryType#INITIAL}.
 */
public final class LedgerEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LocalDateTime at;
    private final double delta;
    private final double balanceAfter;
    private final LedgerEntryType type;
    private final Integer eventId;

    public LedgerEntry(double delta, double balanceAfter, LedgerEntryType type, Integer eventId) {
        this.at = LocalDateTime.now();
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.type = Objects.requireNonNull(type, "type");
        this.eventId = eventId;
    }

    public LocalDateTime getTimestamp() {
        return at;
    }

    /** Signed change to the balance: negative for a debit, positive for a credit. */
    public double getDelta() {
        return delta;
    }

    /** The account balance immediately after this entry was applied. */
    public double getBalanceAfter() {
        return balanceAfter;
    }

    public LedgerEntryType getType() {
        return type;
    }

    /** The event this entry stems from, or {@code null} for the initial allocation. */
    public Integer getEventId() {
        return eventId;
    }

    @Override
    public String toString() {
        return String.format("LedgerEntry[%s %+.2f -> %.2f%s]",
                type, delta, balanceAfter, eventId == null ? "" : ", event " + eventId);
    }
}
