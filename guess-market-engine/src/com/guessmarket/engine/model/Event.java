package com.guessmarket.engine.model;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.engine.exception.MarketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A prediction-market event: the aggregate root that owns its options, its cash
 * pool, its commission terms, its trade history, its participants, their holdings
 * and its lifecycle. Every rule about those things is enforced here; nothing
 * outside this class mutates them, and there are no plain setters.
 *
 * <p><b>Lifecycle</b>
 * <pre>
 *   NOT_ACTIVE --open(marketMaker)--> ACTIVE --settleAndClose(winner)--> CLOSED
 * </pre>
 * Each transition is one-way and guarded. A trade ({@link #buy}) is only accepted
 * while {@code ACTIVE}.
 *
 * <p><b>Money</b> is {@code double} dollars, held in a {@code double} pool for
 * now (a follow-up moves it to {@link Account}). The pool holds the market-maker
 * subsidy, trade proceeds and collected commission; on close it pays the winners
 * and then sweeps whatever is left to the market maker.
 */
public class Event implements Serializable {

    private static final Logger LOG = LogManager.getLogger(Event.class);

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercentage;      // 0..90
    private final CommissionType commissionType;
    private final List<Option> options;
    private final TradingMethod tradingMethod;

    private final Map<String, User> participants = new HashMap<>();
    /** buyer name -> shares held per option index. */
    private final Map<String, int[]> holdingsByUser = new HashMap<>();
    private final List<TradeRecord> tradeHistory = new ArrayList<>();  // newest first

    private EventStatus status = EventStatus.NOT_ACTIVE;
    private double pool;                          // subsidy + trade proceeds + collected commission - payouts
    private double totalCommissionCollected;
    private User marketMaker;                     // set on open()
    private Option winningOption;                 // set on close

    public Event(int id, String name, String description, int commissionPercentage,
                 CommissionType commissionType, List<Option> options, TradingMethod tradingMethod) {

        validateCommission(commissionPercentage);
        validateOptions(options);

        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercentage = commissionPercentage;
        this.commissionType = commissionType;
        this.options = new ArrayList<>(options);
        this.tradingMethod = tradingMethod;
        this.pool = 0.0;   // funded by the market maker in open()
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

    // --- Lifecycle ----------------------------------------------------------

    /**
     * Opens the event for trading. The given user must be this event's market
     * maker and must be able to fund the trading-method subsidy, which is debited
     * from their account into the pool. Atomic: if the debit fails the event
     * stays {@code NOT_ACTIVE}.
     */
    public void open(User marketMaker) {
        if (status != EventStatus.NOT_ACTIVE) {
            throw new MarketException("Event ID " + id + " cannot be opened from state " + status + ".");
        }
        if (!marketMaker.isMarketMakerFor(id)) {
            throw new MarketException(
                    "User '" + marketMaker.getName() + "' is not the market maker for event ID " + id + ".");
        }

        double subsidy = tradingMethod.initialSubsidy();
        if (Double.isNaN(subsidy) || subsidy < 0) {
            subsidy = 0.0;
        }
        if (subsidy > 0) {
            marketMaker.debit(subsidy);      // throws InsufficientFundsException -> event stays NOT_ACTIVE
            pool += subsidy;
        }

        this.marketMaker = marketMaker;
        participants.put(marketMaker.getName(), marketMaker);
        marketMaker.addParticipatingEvent(this);
        status = EventStatus.ACTIVE;
        LOG.info("Event {} opened by market maker '{}'; subsidy {} funded into the pool",
                id, marketMaker.getName(), subsidy);
    }

    /**
     * Executes a purchase: prices the shares, charges the on-purchase commission
     * if configured, debits the buyer, credits the pool, issues the shares and
     * records the holding and the trade. Atomic: the buyer is debited first, so
     * an unaffordable trade throws before anything else changes.
     *
     * @param optionIndex 0-based index into {@link #getOptions()}
     */
    public TradeReceipt buy(User buyer, int optionIndex, int quantity) {
        if (status != EventStatus.ACTIVE) {
            throw new MarketException("Event ID " + id + " is not open for trading (state: " + status + ").");
        }

        TradeReceipt receipt = quote(optionIndex, quantity);   // also validates optionIndex / quantity

        buyer.debit(receipt.totalPaid());        // InsufficientFundsException -> nothing below runs

        Option option = options.get(optionIndex);
        pool += receipt.sharesCost();
        recordCommission(receipt.commission());
        option.addShares(quantity);
        holdingsByUser.computeIfAbsent(buyer.getName(), k -> new int[options.size()])[optionIndex] += quantity;
        participants.put(buyer.getName(), buyer);
        buyer.addParticipatingEvent(this);
        tradeHistory.add(0, new TradeRecord(buyer.getName(), option.getName(), quantity, receipt.totalPaid()));

        LOG.info("Event {}: '{}' bought {} '{}' for {} (shares {}, commission {}); pool now {}",
                id, buyer.getName(), quantity, option.getName(),
                receipt.totalPaid(), receipt.sharesCost(), receipt.commission(), pool);
        return receipt;
    }

    /**
     * Prices a prospective trade without changing anything. {@link #buy} charges
     * exactly this breakdown.
     */
    public TradeReceipt quote(int optionIndex, int quantity) {
        if (quantity <= 0) {
            throw new MarketException("Quantity to buy must be strictly positive (> 0), got: " + quantity);
        }
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new MarketException("Invalid option index: " + optionIndex);
        }
        double sharesCost = tradingMethod.costToBuy(optionIndex, quantity, options);
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? sharesCost * (commissionPercentage / 100.0)
                : 0.0;
        return new TradeReceipt(sharesCost, commission, sharesCost + commission);
    }

    /**
     * Settles the market from {@code ACTIVE}: takes the on-close commission from
     * the winning pot if configured, pays every holder of the winning option
     * their per-share payout from the pool, sweeps whatever remains in the pool
     * (collected commission + unspent subsidy + net trade proceeds) to the market
     * maker, records the winner and closes.
     *
     * @param winningOptionIndex 0-based index into {@link #getOptions()}
     */
    public void settleAndClose(int winningOptionIndex) {
        if (status != EventStatus.ACTIVE) {
            throw new MarketException("Event ID " + id + " is not open for settlement (state: " + status + ").");
        }
        if (winningOptionIndex < 0 || winningOptionIndex >= options.size()) {
            throw new MarketException("Invalid winning option index: " + winningOptionIndex);
        }

        Option winner = options.get(winningOptionIndex);
        double commissionFraction = commissionPercentage / 100.0;

        if (commissionType == CommissionType.ON_CLOSE) {
            double winningPot = winner.getSharesOutstanding() * 1.0;   // $1.00 per winning share
            recordCommission(winningPot * commissionFraction);
        }

        // On-close: fee comes out of the payout. On-purchase: it was already taken.
        double payoutPerShare = commissionType == CommissionType.ON_CLOSE
                ? (1.0 - commissionFraction)
                : 1.0;

        double totalPaidToHolders = 0.0;
        int paidHolders = 0;
        for (Map.Entry<String, int[]> entry : holdingsByUser.entrySet()) {
            int heldWinningShares = entry.getValue()[winningOptionIndex];
            if (heldWinningShares <= 0) {
                continue;
            }
            User holder = participants.get(entry.getKey());
            double payout = heldWinningShares * payoutPerShare;
            if (holder != null && payout > 0) {
                holder.credit(payout);
                pool -= payout;
                totalPaidToHolders += payout;
                paidHolders++;
            }
        }

        // Whatever is left in the pool - the collected commission, the unspent
        // subsidy and the net trade proceeds - belongs to the market maker.
        double marketMakerSweep = 0.0;
        if (marketMaker != null && pool > 1e-9) {
            marketMakerSweep = pool;
            marketMaker.credit(pool);
            pool = 0.0;
        }

        this.winningOption = winner;
        this.status = EventStatus.CLOSED;

        LOG.info("Event {} settled: winner '{}', paid {} to {} holder(s), swept {} to market maker '{}'",
                id, winner.getName(), totalPaidToHolders, paidHolders, marketMakerSweep,
                marketMaker == null ? "-" : marketMaker.getName());
    }

    private void recordCommission(double amount) {
        if (amount < 0 || Double.isNaN(amount)) {
            throw new IllegalArgumentException("Commission must be >= 0, got: " + amount);
        }
        totalCommissionCollected += amount;
        pool += amount;
    }

    // --- Accessors --------------------------------------------------------

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

    public TradingMethod getTradingMethod() {
        return tradingMethod;
    }

    public EventStatus getStatus() {
        return status;
    }

    public double getEventAccountBalance() {
        return pool;
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

    /** The user who opened this event and backs its pool; {@code null} until {@link #open}. */
    public User getMarketMaker() {
        return marketMaker;
    }

    public Map<String, User> getParticipants() {
        return Collections.unmodifiableMap(participants);
    }

    public User getParticipantByName(String name) {
        return participants.get(name);
    }

    /** Shares the named user holds of each option (by index), or all zeros. */
    public int[] holdingsOf(String userName) {
        int[] held = holdingsByUser.get(userName);
        return held == null ? new int[options.size()] : held.clone();
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
