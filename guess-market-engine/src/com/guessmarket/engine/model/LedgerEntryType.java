package com.guessmarket.engine.model;

/** Why a user's balance moved. One per {@link LedgerEntry}. */
public enum LedgerEntryType {

    /** The user's opening cash allocation. */
    INITIAL,

    /** Bought shares (LMSR buy, order-book fill as the buyer, or a mint). */
    PURCHASE,

    /** Sold shares (order-book fill as the seller). */
    SALE,

    /** Commission collected, credited to the market maker. */
    COMMISSION,

    /** Market-maker subsidy funded into an event pool when it opens. */
    SUBSIDY,

    /** Winnings paid to a holder of the winning option at close. */
    PAYOUT,

    /** Leftover event pool swept to the market maker at close. */
    SETTLEMENT
}
