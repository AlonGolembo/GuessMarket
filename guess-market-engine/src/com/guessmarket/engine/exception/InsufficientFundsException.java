package com.guessmarket.engine.exception;

/**
 * Thrown when a debit would take an {@code Account} below zero. Carries the
 * shortfall so the UI can tell the user exactly how much more they need.
 */
public class InsufficientFundsException extends MarketException {

    private final double required;
    private final double available;

    public InsufficientFundsException(double required, double available) {
        super(String.format(
                "Insufficient funds: %.2f required but only %.2f available (short by %.2f).",
                required, available, required - available));
        this.required = required;
        this.available = available;
    }

    public double getRequired() {
        return required;
    }

    public double getAvailable() {
        return available;
    }
}
