package com.guessmarket.engine.model;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.engine.exception.InsufficientFundsException;
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
import java.util.Optional;

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
 * subsidy and trade proceeds; on close it pays the winners {@link TradingMethod#baseValue()}
 * per share and sweeps whatever is left to the market maker. Commission is not
 * part of the pool - it is credited to the market maker's account as it is collected.
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
    private OrderBook orderBook;                  // non-null iff tradingMethod.usesOrderBook()

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

        int initialShares = tradingMethod.initialShares();
        if (tradingMethod.usesOrderBook()) {
            orderBook = new OrderBook();
            if (initialShares > 0) {
                // The market maker receives `initialShares` of *each* option (the
                // pairs just paid for above) and immediately offers them all for
                // sale, split evenly at half the base value per the spec.
                options.get(0).addShares(initialShares);
                options.get(1).addShares(initialShares);
                holdingsByUser.put(marketMaker.getName(), new int[]{initialShares, initialShares});
                double askPrice = tradingMethod.baseValue() / 2.0;
                orderBook.restNew(marketMaker.getName(), 0, OrderSide.ASK, initialShares, askPrice);
                orderBook.restNew(marketMaker.getName(), 1, OrderSide.ASK, initialShares, askPrice);
            }
        }

        this.marketMaker = marketMaker;
        participants.put(marketMaker.getName(), marketMaker);
        marketMaker.addParticipatingEvent(this);
        status = EventStatus.ACTIVE;
        LOG.info("Event {} opened by market maker '{}'; subsidy {} funded into the pool{}",
                id, marketMaker.getName(), subsidy,
                initialShares > 0 ? "; " + initialShares + " of each option allocated and posted for sale" : "");
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
     * Settles the market from {@code ACTIVE}: takes the on-close commission (paid
     * straight to the market maker) from the winning pot if configured, pays every
     * holder of the winning option {@link TradingMethod#baseValue()} per share
     * (minus that commission) from the pool, sweeps whatever remains in the pool
     * - the unspent subsidy and net trade proceeds - to the market maker, records
     * the winner and closes.
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

        double baseValue = tradingMethod.baseValue();

        if (commissionType == CommissionType.ON_CLOSE) {
            double winningPot = winner.getSharesOutstanding() * baseValue;
            recordCommission(winningPot * commissionFraction);
        }

        // On-close: fee comes out of the payout. On-purchase: it was already taken.
        double payoutPerShare = commissionType == CommissionType.ON_CLOSE
                ? baseValue * (1.0 - commissionFraction)
                : baseValue;

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
        if (orderBook != null) {
            orderBook.clear();      // resting orders don't survive settlement
        }

        LOG.info("Event {} settled: winner '{}', paid {} to {} holder(s), swept {} to market maker '{}'",
                id, winner.getName(), totalPaidToHolders, paidHolders, marketMakerSweep,
                marketMaker == null ? "-" : marketMaker.getName());
    }

    // --- Order Book --------------------------------------------------------

    /**
     * Places a limit order on this event's {@link OrderBook}: it is matched
     * immediately, price-then-time, against the opposite side of the same
     * option, at the resting order's price (price improvement for the
     * incoming order); whatever doesn't fill rests in the book. No escrow -
     * cash and shares only move as fills happen, and a resting order that can
     * no longer be honoured (its owner has since spent the cash or - not
     * possible for asks, see below - the shares elsewhere) is dropped rather
     * than filled short.
     *
     * <p>Placing an ask requires currently holding at least {@code quantity}
     * shares of that option net of the user's own other resting asks on it,
     * so a resting ask can never end up unbacked. Placing a bid requires that
     * its full {@code quantity * price} (plus on-purchase commission) is
     * affordable right now; a bid can still go stale later if the same user
     * has other bids that jointly overcommit their balance; the fill-time
     * affordability check below then trims or drops it gracefully.
     *
     * @param optionIndex 0-based index into {@link #getOptions()}
     */
    public OrderOutcome placeOrder(User user, int optionIndex, OrderSide side, int quantity, double price) {
        if (status != EventStatus.ACTIVE) {
            throw new MarketException("Event ID " + id + " is not open for trading (state: " + status + ").");
        }
        if (orderBook == null) {
            throw new MarketException("Event ID " + id + " does not trade through an order book.");
        }
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new MarketException("Invalid option index: " + optionIndex);
        }
        if (quantity <= 0) {
            throw new MarketException("Quantity must be strictly positive, got: " + quantity);
        }
        double baseValue = tradingMethod.baseValue();
        if (Double.isNaN(price) || price <= 0 || price > baseValue) {
            throw new MarketException("Price must be in (0, " + baseValue + "], got: " + price);
        }

        if (side == OrderSide.ASK) {
            int free = freeSharesOf(user.getName(), optionIndex);
            if (quantity > free) {
                throw new MarketException("User '" + user.getName() + "' only has " + free
                        + " free share(s) of '" + options.get(optionIndex).getName() + "' to sell.");
            }
        } else {
            double perUnit = commissionType == CommissionType.ON_PURCHASE
                    ? price * (1.0 + commissionPercentage / 100.0)
                    : price;
            double totalCost = quantity * perUnit;
            if (totalCost > user.getAccountBalance() + 1e-9) {
                throw new InsufficientFundsException(totalCost, user.getAccountBalance());
            }
        }

        participants.put(user.getName(), user);
        user.addParticipatingEvent(this);

        LimitOrder incoming = orderBook.createOrder(user.getName(), optionIndex, side, quantity, price);
        matchDirect(incoming, user);
        // Minting (Phase 3): when incoming is a bid and its remainder can pair
        // with a resting bid on the other option, new share pairs get created
        // here instead of just resting.

        if (incoming.getRemaining() > 0) {
            orderBook.rest(incoming);
        }

        int filled = quantity - incoming.getRemaining();
        LOG.info("Event {}: '{}' placed {} {} '{}' @ {}; filled {}, resting {}",
                id, user.getName(), side, quantity, options.get(optionIndex).getName(), price,
                filled, incoming.getRemaining());
        return new OrderOutcome(incoming.getId(), filled, incoming.getRemaining());
    }

    /** Cancels a still-resting order; only its own owner may cancel it. */
    public void cancelOrder(User user, long orderId) {
        if (orderBook == null) {
            throw new MarketException("Event ID " + id + " does not trade through an order book.");
        }
        if (status != EventStatus.ACTIVE) {
            throw new MarketException("Event ID " + id + " is not open for trading (state: " + status + ").");
        }
        LimitOrder order = orderBook.findById(orderId);
        if (order == null) {
            throw new MarketException("No resting order with id " + orderId + " in event ID " + id + ".");
        }
        if (!order.getUserName().equals(user.getName())) {
            throw new MarketException("User '" + user.getName() + "' does not own order " + orderId + ".");
        }
        orderBook.remove(order);
        LOG.info("Event {}: '{}' cancelled order {}", id, user.getName(), orderId);
    }

    /** The live order book, or {@code null} for an event not using this trading method. */
    OrderBook getOrderBook() {
        return orderBook;
    }

    /**
     * Walks the opposite side of {@code incoming}'s option, best price first,
     * executing trades while prices cross, until {@code incoming} is filled,
     * the book runs dry, or no more resting order there can be honoured.
     */
    private void matchDirect(LimitOrder incoming, User incomingUser) {
        int optionIndex = incoming.getOptionIndex();
        boolean incomingIsBid = incoming.getSide() == OrderSide.BID;

        while (incoming.getRemaining() > 0) {
            Optional<LimitOrder> counterpart = incomingIsBid
                    ? orderBook.bestAsk(optionIndex)
                    : orderBook.bestBid(optionIndex);
            if (counterpart.isEmpty()) {
                break;
            }
            LimitOrder resting = counterpart.get();

            boolean crosses = incomingIsBid
                    ? incoming.getPrice() >= resting.getPrice()
                    : incoming.getPrice() <= resting.getPrice();
            if (!crosses) {
                break;
            }

            User buyer = incomingIsBid ? incomingUser : getParticipantByName(resting.getUserName());
            User seller = incomingIsBid ? getParticipantByName(resting.getUserName()) : incomingUser;
            double execPrice = resting.getPrice();

            int quantity = Math.min(incoming.getRemaining(), resting.getRemaining());
            quantity = Math.min(quantity, holdingsOf(seller.getName())[optionIndex]);
            quantity = Math.min(quantity, affordableQuantity(buyer, execPrice));

            if (quantity <= 0) {
                // Its owner can no longer honour this resting order (balance or
                // shares have since moved elsewhere) - drop it and try the next.
                LOG.warn("Event {}: dropping stale resting order {} (no longer honourable)", id, resting.getId());
                orderBook.remove(resting);
                continue;
            }

            executeTrade(buyer, seller, optionIndex, quantity, execPrice);
            incoming.reduce(quantity);
            resting.reduce(quantity);
            if (resting.isFilled()) {
                orderBook.remove(resting);
            }
        }
    }

    /** Moves cash and shares for one fill and records it; commission goes straight to the market maker. */
    private void executeTrade(User buyer, User seller, int optionIndex, int quantity, double execPrice) {
        double shareCost = quantity * execPrice;
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? shareCost * (commissionPercentage / 100.0)
                : 0.0;

        buyer.debit(shareCost + commission);
        seller.credit(shareCost);
        recordCommission(commission);

        holdingsByUser.computeIfAbsent(seller.getName(), k -> new int[options.size()])[optionIndex] -= quantity;
        holdingsByUser.computeIfAbsent(buyer.getName(), k -> new int[options.size()])[optionIndex] += quantity;

        participants.put(buyer.getName(), buyer);
        participants.put(seller.getName(), seller);
        buyer.addParticipatingEvent(this);
        seller.addParticipatingEvent(this);

        String optionName = options.get(optionIndex).getName();
        tradeHistory.add(0, new TradeRecord(buyer.getName(), optionName, quantity, shareCost + commission));
        orderBook.recordTrade(optionIndex, execPrice);

        LOG.info("Event {}: order-book trade - '{}' bought {} '{}' from '{}' @ {} (cost {}, commission {})",
                id, buyer.getName(), quantity, optionName, seller.getName(), execPrice, shareCost, commission);
    }

    /** How many more shares of {@code optionIndex} the user could still sell, net of their own open asks. */
    private int freeSharesOf(String userName, int optionIndex) {
        int held = holdingsOf(userName)[optionIndex];
        int committed = 0;
        for (LimitOrder order : orderBook.asks(optionIndex)) {
            if (order.getUserName().equals(userName)) {
                committed += order.getRemaining();
            }
        }
        return held - committed;
    }

    /** Largest quantity {@code buyer} can afford at {@code execPrice} (including on-purchase commission, if any). */
    private int affordableQuantity(User buyer, double execPrice) {
        double perUnit = commissionType == CommissionType.ON_PURCHASE
                ? execPrice * (1.0 + commissionPercentage / 100.0)
                : execPrice;
        if (perUnit <= 0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(buyer.getAccountBalance() / perUnit + 1e-9);
    }

    /**
     * Records a commission charge and pays it straight to the market maker's
     * account - it is the market maker's fee, not part of the event pool that
     * gets divided among winning holders.
     */
    private void recordCommission(double amount) {
        if (amount < 0 || Double.isNaN(amount)) {
            throw new IllegalArgumentException("Commission must be >= 0, got: " + amount);
        }
        if (amount == 0) {
            return;
        }
        totalCommissionCollected += amount;
        marketMaker.credit(amount);
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
