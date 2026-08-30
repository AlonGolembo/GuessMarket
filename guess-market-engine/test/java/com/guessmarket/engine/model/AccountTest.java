package com.guessmarket.engine.model;

import com.guessmarket.engine.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {

    @Test
    void debitAndCreditMoveTheBalance() {
        Account account = new Account(100.0);
        account.debit(30.0);
        account.credit(5.0);
        assertEquals(75.0, account.balance(), 1e-9);
    }

    @Test
    void debitBeyondBalanceIsRefusedAndBalanceUnchanged() {
        Account account = new Account(50.0);
        InsufficientFundsException ex =
                assertThrows(InsufficientFundsException.class, () -> account.debit(50.01));
        assertEquals(50.0, account.balance(), 1e-9);
        assertEquals(50.01, ex.getRequired(), 1e-9);
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        Account account = new Account(10.0);
        assertThrows(IllegalArgumentException.class, () -> account.debit(0));
        assertThrows(IllegalArgumentException.class, () -> account.credit(-1));
    }

    @Test
    void negativeInitialBalanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Account(-0.01));
    }
}
