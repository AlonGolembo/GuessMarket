package com.guessmarket.engine.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A market participant. A user has a stable {@code name} (their identity), an
 * {@link Account} holding their cash, and a set of events for which they act as
 * the market maker.
 *
 * <p>Cash only moves through {@link #debit} / {@link #credit}, which delegate to
 * the {@link Account}; there is no balance setter. Two users are equal when their
 * names are equal.
 *
 * <p>A user whose balance is forced below zero to honour a standing obligation is
 * {@linkplain #isBlocked() blocked} - from then on the engine refuses every
 * action they attempt.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Account account;
    /** Grows when the user is made market maker of a newly created event. */
    private Set<String> marketMakerEventNames;
    /** {@code true} once the balance went negative - the user can no longer act. */
    private boolean blocked;

    /** Events this user currently participates in, keyed by event name. Populated as trades happen. */
    private final Map<String, Event> participatingEvents = new HashMap<>();

    public User(String name, double initialBalance, Set<String> marketMakerEventNames) {
        this.name = Objects.requireNonNull(name, "name");
        this.account = new Account(initialBalance);
        this.marketMakerEventNames = new LinkedHashSet<>();
        if (marketMakerEventNames != null) {
            this.marketMakerEventNames.addAll(marketMakerEventNames);
        }
    }

    public String getName() {
        return name;
    }

    /** The user's cash account. Mutate it with {@link #debit}/{@link #credit}. */
    public Account getAccount() {
        return account;
    }

    /** Convenience read-through to {@link Account#balance()}. */
    public double getAccountBalance() {
        return account.balance();
    }

    public void debit(double amount, LedgerEntryType type, String eventName) {
        account.debit(amount, type, eventName);
    }

    public void credit(double amount, LedgerEntryType type, String eventName) {
        account.credit(amount, type, eventName);
    }

    /**
     * Forces {@code amount} out of the account even into a negative balance, then
     * blocks the user. Only for honouring a standing obligation they can no longer
     * cover (see the class doc).
     */
    public void forceDebitAndBlock(double amount, LedgerEntryType type, String eventName) {
        account.debitAllowingOverdraw(amount, type, eventName);
        blocked = true;
    }

    /** {@code true} once this user's balance was forced negative - they can no longer act. */
    public boolean isBlocked() {
        return blocked;
    }

    /** Names of the events this user is the market maker for (unmodifiable). */
    public Set<String> getMarketMakerEventNames() {
        return Collections.unmodifiableSet(marketMakerEventNames);
    }

    public boolean isMarketMakerFor(String eventName) {
        return marketMakerEventNames.contains(eventName);
    }

    /** Assigns this user as the market maker of the given (newly created) event. */
    public void addMarketMakerEvent(String eventName) {
        if (!(marketMakerEventNames instanceof LinkedHashSet<String>)) {
            marketMakerEventNames = new LinkedHashSet<>(marketMakerEventNames);
        }
        marketMakerEventNames.add(eventName);
    }

    public Map<String, Event> getParticipatingEvents() {
        return participatingEvents;
    }

    public void addParticipatingEvent(Event event) {
        participatingEvents.put(event.getName(), event);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return name.equals(((User) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "User[" + name + ", " + account + "]";
    }
}
