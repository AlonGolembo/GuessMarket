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
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Account account;
    /** Grows when the user is made market maker of a newly created event. */
    private Set<Integer> marketMakerEventIds;

    /** Events this user currently participates in, keyed by event id. Populated as trades happen. */
    private final Map<Integer, Event> participatingEvents = new HashMap<>();

    public User(String name, double initialBalance, Set<Integer> marketMakerEventIds) {
        this.name = Objects.requireNonNull(name, "name");
        this.account = new Account(initialBalance);
        this.marketMakerEventIds = new LinkedHashSet<>();
        if (marketMakerEventIds != null) {
            this.marketMakerEventIds.addAll(marketMakerEventIds);
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

    public void debit(double amount, LedgerEntryType type, Integer eventId) {
        account.debit(amount, type, eventId);
    }

    public void credit(double amount, LedgerEntryType type, Integer eventId) {
        account.credit(amount, type, eventId);
    }

    /** Ids of the events this user is the market maker for (unmodifiable). */
    public Set<Integer> getMarketMakerEventIds() {
        return Collections.unmodifiableSet(marketMakerEventIds);
    }

    public boolean isMarketMakerFor(int eventId) {
        return marketMakerEventIds.contains(eventId);
    }

    /** Assigns this user as the market maker of the given (newly created) event. */
    public void addMarketMakerEvent(int eventId) {
        if (!(marketMakerEventIds instanceof LinkedHashSet<Integer>)) {
            marketMakerEventIds = new LinkedHashSet<>(marketMakerEventIds);
        }
        marketMakerEventIds.add(eventId);
    }

    public Map<Integer, Event> getParticipatingEvents() {
        return participatingEvents;
    }

    public void addParticipatingEvent(Event event) {
        participatingEvents.put(event.getId(), event);
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
