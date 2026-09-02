package com.guessmarket.engine.model;

import com.guessmarket.engine.exception.InsufficientFundsException;

import java.io.Serializable;

/**
 * A monetary balance owned by a {@link User} or an {@link Event}.
 *
 * <p>The balance is encapsulated: it can only move through {@link #debit(double)}
 * and {@link #credit(double)}, both of which validate their argument, and a debit
 * that would overdraw the account is refused. There is deliberately no setter, so
 * no caller can leave an account in an impossible state.
 *
 * <p>Amounts are {@code double} dollars. Migrating to {@code BigDecimal} is
 * tracked as future work; confining money to this one class is what makes that
 * change local when it happens.
 */
public final class Account implements Serializable {

    private double balance;

    public Account(double initialBalance) {
        if (initialBalance < 0 || Double.isNaN(initialBalance)) {
            throw new IllegalArgumentException("Initial balance must be >= 0, got: " + initialBalance);
        }
        this.balance = initialBalance;
    }

    /** Current balance in dollars. */
    public double balance() {
        return balance;
    }

    /**
     * Removes {@code amount} from the balance.
     *
     * @throws IllegalArgumentException     if {@code amount} is not strictly positive
     * @throws InsufficientFundsException   if {@code amount} exceeds the balance
     */
    public void debit(double amount) {
        requirePositive(amount);
        if (amount > balance) {
            throw new InsufficientFundsException(amount, balance);
        }
        balance -= amount;
    }

    /**
     * Adds {@code amount} to the balance.
     *
     * @throws IllegalArgumentException if {@code amount} is not strictly positive
     */
    public void credit(double amount) {
        requirePositive(amount);
        balance += amount;
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
