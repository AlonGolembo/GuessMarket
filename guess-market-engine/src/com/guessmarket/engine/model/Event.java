package com.guessmarket.engine.model;

import com.guessmarket.engine.exception.MarketException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A prediction-market event: the aggregate root that owns its options, its cash
 * pool, its commission terms, its trade history, its participants and its
 * lifecycle. Every rule about those things is enforced here; callers never reach
 * past this class to mutate them, and there are no plain setters.
 *
 * <p><b>Lifecycle:</b> {@code NOT_ACTIVE -> ACTIVE -> CLOSED}. A new event starts
 * {@code NOT_ACTIVE}; {@link #activate()} opens it for trading; {@link #settleAndClose}
 * declares a winner and closes it. Each transition is one-way and guarded.
 *
 * <p>Money is {@code double} dollars for now (see {@link Account}).
 */
public class Event implements Serializable {

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercentage;      // 0..90
    private final CommissionType commissionType;
    private final List<Option> options;
    private final ITradingMethod tradingMethod;
    private final Map<String, User> participants = new HashMap<>();
    private final List<TradeRecord> tradeHistory = new ArrayList<>();  // newest first

    private EventStatus status = EventStatus.NOT_ACTIVE;
    private double eventAccountBalance;           // market-maker subsidy + trade proceeds + collected commission
    private double totalCommissionCollected;
    private Option winningOption;                 // set on close

    public Event(int id, String name, String description, int commissionPercentage,
                 CommissionType commissionType, List<Option> options, ITradingMethod tradingMethod) {

        validateCommission(commissionPercentage);
        validateOptions(options);

        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercentage = commissionPercentage;
        this.commissionType = commissionType;
        this.options = new ArrayList<>(options);
        this.tradingMethod = tradingMethod;
        this.eventAccountBalance = seedSubsidy(tradingMethod);
    }

    private static double seedSubsidy(ITradingMethod method) {
        Double subsidy = method == null ? null : method.getInitialSubsidy();
        return (subsidy == null || subsidy.isNaN() || subsidy < 0) ? 0.0 : subsidy;
    }

    private static void validateOptions(List<Option> options) {
        if (options == null || options.size() != 2) {
            throw new IllegalArgumentException(
                    "An event must have exactly two options; provided: " + (options == null ? 0 : options.size()));
        }
    }

    private static void validateCommission(int commission) {
        if (commission < 0 || commission > 90) {
            throw new IllegalArgumentException(
                    "Commission percentage must be between 0 and 90 inclusive, got: " + commission);
        }
    }

    // --- Lifecycle ------------------------------------------------------------

    /** Opens the event for trading. Only valid from {@code NOT_ACTIVE}. */
    public void activate() {
        if (status != EventStatus.NOT_ACTIVE) {
            throw new MarketException("Event ID " + id + " cannot be activated from state " + status + ".");
        }
        status = EventStatus.ACTIVE;
    }

    /**
     * Settles the market: applies the on-close commission if configured, pays the
     * winning shareholders, records the winner and closes the event. Only valid
     * from {@code ACTIVE}.
     */
    public void settleAndClose(Option winningOption) {
        if (status != EventStatus.ACTIVE) {
            throw new MarketException("Event ID " + id + " is not open for settlement (state: " + status + ").");
        }
        if (winningOption == null || !options.contains(winningOption)) {
            throw new MarketException("Winning option does not belong to event: " + name);
        }

        int winningShares = winningOption.getSharesOutstanding();

        if (commissionType == CommissionType.ON_CLOSE) {
            double payoutBeforeFee = winningShares * 1.0;           // $1.00 base payout per winning share
            addCommission(payoutBeforeFee * (commissionPercentage / 100.0));
        }

        distributeWinningPayouts(winningOption, winningShares);

        this.winningOption = winningOption;
        this.status = EventStatus.CLOSED;
    }

    /**
     * Credits ($1.00 - commission fraction) per winning share to each holder.
     * Real per-user holdings arrive with the trade-into-aggregate work; until
     * then this is a no-op.
     */
    private void distributeWinningPayouts(Option winningOption, int winningShares) {
        // TODO(phase-3): iterate per-user holdings of winningOption and credit their accounts.
    }

    // --- Trading-side mutations (called by the trade orchestration) ----------

    /** Adds shares of one of this event's options. */
    public void issueShares(Option option, int quantity) {
        if (!options.contains(option)) {
            throw new MarketException("Option " + option + " does not belong to event " + id + ".");
        }
        option.addShares(quantity);
    }

    /** Records cash received from a trade into the event pool. */
    public void recordTradeProceeds(double amount) {
        if (amount < 0 || Double.isNaN(amount)) {
            throw new IllegalArgumentException("Trade proceeds must be >= 0, got: " + amount);
        }
        eventAccountBalance += amount;
    }

    /** Records collected commission (also held in the event pool). */
    public void addCommission(double amount) {
        if (amount < 0 || Double.isNaN(amount)) {
            throw new IllegalArgumentException("Commission must be >= 0, got: " + amount);
        }
        totalCommissionCollected += amount;
        eventAccountBalance += amount;
    }

    /** Appends a trade to the audit log (kept newest-first). */
    public void addTradeRecord(TradeRecord record) {
        tradeHistory.add(0, record);
    }

    public void addParticipant(User user) {
        participants.put(user.getName(), user);
    }

    // --- Accessors ----------------------------------------------------------

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercentage() {
        return commissionPercentage;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public ITradingMethod getTradingMethod() {
        return tradingMethod;
    }

    public EventStatus getStatus() {
        return status;
    }

    public double getEventAccountBalance() {
        return eventAccountBalance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    /** Trade history, newest first, unmodifiable. */
    public List<TradeRecord> getTradeHistory() {
        return Collections.unmodifiableList(tradeHistory);
    }

    public Option getWinningOption() {
        return winningOption;
    }

    public Map<String, User> getParticipants() {
        return Collections.unmodifiableMap(participants);
    }

    public User getParticipantByName(String name) {
        return participants.get(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Event) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Event[" + id + " '" + name + "', " + status + "]";
    }
}
