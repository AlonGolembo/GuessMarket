package com.guessmarket.engine.model;

import com.guessmarket.engine.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    @Test
    void debitAndCreditMoveTheBalance() {
        Account account = new Account(100.0);
        account.debit(30.0, LedgerEntryType.PURCHASE, 1);
        account.credit(5.0, LedgerEntryType.SALE, 1);
        assertEquals(75.0, account.balance(), 1e-9);
    }

    @Test
    void debitBeyondBalanceIsRefusedAndBalanceUnchanged() {
        Account account = new Account(50.0);
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> account.debit(50.01, LedgerEntryType.PURCHASE, 1));
        assertEquals(50.0, account.balance(), 1e-9);
        assertEquals(50.01, ex.getRequired(), 1e-9);
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        Account account = new Account(10.0);
        assertThrows(IllegalArgumentException.class, () -> account.debit(0, LedgerEntryType.PURCHASE, 1));
        assertThrows(IllegalArgumentException.class, () -> account.credit(-1, LedgerEntryType.SALE, 1));
    }

    @Test
    void negativeInitialBalanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Account(-0.01));
    }

    @Test
    void openingBalanceIsRecordedAsTheFirstLedgerEntry() {
        Account account = new Account(100.0);

        List<LedgerEntry> ledger = account.ledgerEntries();
        assertEquals(1, ledger.size());
        LedgerEntry initial = ledger.get(0);
        assertEquals(LedgerEntryType.INITIAL, initial.getType());
        assertEquals(100.0, initial.getDelta(), 1e-9);
        assertEquals(100.0, initial.getBalanceAfter(), 1e-9);
        assertNull(initial.getEventId());
    }

    @Test
    void zeroOpeningBalanceRecordsNothing() {
        assertTrue(new Account(0.0).ledgerEntries().isEmpty());
    }

    @Test
    void everyMoveAppendsASignedEntryCarryingTheResultingBalance() {
        Account account = new Account(100.0);
        account.debit(30.0, LedgerEntryType.PURCHASE, 7);
        account.credit(12.0, LedgerEntryType.PAYOUT, 7);

        List<LedgerEntry> ledger = account.ledgerEntries();
        assertEquals(3, ledger.size());

        LedgerEntry debit = ledger.get(1);
        assertEquals(LedgerEntryType.PURCHASE, debit.getType());
        assertEquals(-30.0, debit.getDelta(), 1e-9);
        assertEquals(70.0, debit.getBalanceAfter(), 1e-9);
        assertEquals(7, debit.getEventId());

        LedgerEntry credit = ledger.get(2);
        assertEquals(LedgerEntryType.PAYOUT, credit.getType());
        assertEquals(12.0, credit.getDelta(), 1e-9);
        assertEquals(82.0, credit.getBalanceAfter(), 1e-9);
    }

    @Test
    void aRefusedDebitAppendsNoEntry() {
        Account account = new Account(50.0);
        assertThrows(InsufficientFundsException.class,
                () -> account.debit(999.0, LedgerEntryType.PURCHASE, 1));
        assertEquals(1, account.ledgerEntries().size());   // just the opening entry
    }

    @Test
    void ledgerViewIsUnmodifiable() {
        Account account = new Account(10.0);
        List<LedgerEntry> ledger = account.ledgerEntries();
        assertThrows(UnsupportedOperationException.class,
                () -> ledger.add(new LedgerEntry(1.0, 11.0, LedgerEntryType.SALE, 1)));
    }
}
