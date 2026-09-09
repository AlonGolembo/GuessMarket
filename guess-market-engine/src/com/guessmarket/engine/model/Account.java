package com.guessmarket.engine.model;

import com.guessmarket.engine.exception.InsufficientFundsException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A monetary balance owned by a {@link User} or an {@link Event}.
 *
 * <p>The balance is encapsulated: it can only move through
 * {@link #debit(double, LedgerEntryType, Integer)} and
 * {@link #credit(double, LedgerEntryType, Integer)}, both of which validate their
 * argument; a normal debit that would overdraw the account is refused. The one
 * exception is {@link #debitAllowingOverdraw} - used only to force a standing
 * obligation through and then block the holder. There is deliberately no setter.
 *
 * <p>Every successful move appends a {@link LedgerEntry} to an append-only history,
 * readable via {@link #ledgerEntries()}. Each entry carries the balance it left
 * behind, so a balance-over-time view is a direct read with no replay.
 *
 * <p>Amounts are {@code double} dollars. Migrating to {@code BigDecimal} is
 * tracked as future work; confining money to this one class is what makes that
 * change local when it happens.
 */
public final class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private double balance;

    /** Append-only balance history, oldest first. Non-final so a null from an
     *  older serialized stream can be replaced with an empty list on first use. */
    private List<LedgerEntry> ledger = new ArrayList<>();

    public Account(double initialBalance) {
        if (initialBalance < 0 || Double.isNaN(initialBalance)) {
            throw new IllegalArgumentException("Initial balance must be >= 0, got: " + initialBalance);
        }
        this.balance = initialBalance;
        if (initialBalance > 0) {
            appendEntry(initialBalance, LedgerEntryType.INITIAL, null);
        }
    }

    /** Current balance in dollars. */
    public double balance() {
        return balance;
    }

    /**
     * Removes {@code amount} from the balance and records the reason.
     *
     * @param type    why the money is leaving
     * @param eventId the event this stems from, or {@code null}
     * @throws IllegalArgumentException     if {@code amount} is not strictly positive
     * @throws InsufficientFundsException   if {@code amount} exceeds the balance
     */
    public void debit(double amount, LedgerEntryType type, Integer eventId) {
        requirePositive(amount);
        if (amount > balance) {
            throw new InsufficientFundsException(amount, balance);
        }
        balance -= amount;
        appendEntry(-amount, type, eventId);
    }

    /**
     * Removes {@code amount} even if it takes the balance below zero, and records
     * the reason. Used only to honour a standing obligation the account holder can
     * no longer cover (a resting order-book bid that filled after they spent their
     * money elsewhere) - see the "blocked user" rule. The caller is responsible
     * for blocking the holder afterwards.
     *
     * @throws IllegalArgumentException if {@code amount} is not strictly positive
     */
    public void debitAllowingOverdraw(double amount, LedgerEntryType type, Integer eventId) {
        requirePositive(amount);
        balance -= amount;
        appendEntry(-amount, type, eventId);
    }

    /**
     * Adds {@code amount} to the balance and records the reason.
     *
     * @param type    why the money is arriving
     * @param eventId the event this stems from, or {@code null}
     * @throws IllegalArgumentException if {@code amount} is not strictly positive
     */
    public void credit(double amount, LedgerEntryType type, Integer eventId) {
        requirePositive(amount);
        balance += amount;
        appendEntry(amount, type, eventId);
    }

    /** This account's balance history, oldest first (unmodifiable). */
    public List<LedgerEntry> ledgerEntries() {
        if (ledger == null) {
            ledger = new ArrayList<>();
        }
        return Collections.unmodifiableList(ledger);
    }

    private void appendEntry(double delta, LedgerEntryType type, Integer eventId) {
        if (ledger == null) {
            ledger = new ArrayList<>();
        }
        ledger.add(new LedgerEntry(delta, balance, type, eventId));
    }

    private static void requirePositive(double amount) {
        if (Double.isNaN(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be strictly positive, got: " + amount);
        }
    }

    @Override
    public String toString() {
        return String.format("Account[balance=%.2f]", balance);
    }
}
