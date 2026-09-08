package com.guessmarket.dto;

/**
 * One line of a user's balance history, formatted for display.
 *
 * @param timestamp    pre-formatted for display
 * @param delta        signed change: negative for a debit, positive for a credit
 * @param balanceAfter the balance immediately after this entry
 * @param type         why the balance moved (e.g. {@code "PURCHASE"}, {@code "PAYOUT"})
 * @param eventId      the event this stems from, or {@code null} for the initial allocation
 */
public record LedgerEntryDTO(
        String timestamp,
        double delta,
        double balanceAfter,
        String type,
        Integer eventId
) {}
