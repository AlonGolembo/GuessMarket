# Guess Market

A prediction-market platform. Users trade shares in binary outcomes, priced by
one of two trading methods per event: the **Logarithmic Market Scoring Rule
(LMSR)**, an automated market maker that sets prices from demand, or **Order
Book**, a peer-to-peer public book of limit orders (Polymarket-style). An
event's market maker posts a subsidy (and, for Order Book, an initial share
allocation) to open it, traders buy shares, and on settlement the holders of
the winning option are paid.

## Modules

The build is a single Maven reactor.

| Module | Contents |
|---|---|
| `guess-market-dto` | Immutable record DTOs crossing the engine's API boundary. No dependencies. |
| `guess-market-engine` | Domain model, LMSR pricing, XML loading, state persistence. The only exported package is `com.guessmarket.engine.api`. |
| `javafx-ui` | JavaFX desktop front-end. Talks only to `engine.api` + the DTOs. |

Dependency direction is one-way: `javafx-ui → engine.api → dto`, with the engine's
internals (`model`, `lmsr`, `xml`, `mapper`, `serialization`) hidden behind the
interface.

## Build & run

```bash
mvn clean verify              # compile + test all modules
mvn -pl javafx-ui javafx:run  # launch the desktop app
```

Requires JDK 25. IntelliJ: open the root `pom.xml` as a project.

> If your machine runs antivirus HTTPS/TLS scanning (e.g. AVG), Maven on the
> command line may fail to validate `repo.maven.apache.org`. Point it at a
> truststore that includes the scanner's root CA, or use the IDE's Maven, which
> is usually already configured.

## Domain model

`Event` is the aggregate root. It owns its options, cash pool, commission terms,
trade history, participants and their holdings, and enforces every rule about
them — there are no plain setters.

```
NOT_ACTIVE --open(marketMaker)--> ACTIVE --settleAndClose(winner)--> CLOSED
```

- **`open(User marketMaker)`** — verifies the user is the event's market maker and
  can fund the trading-method subsidy, debits it into the pool, activates.
- **`buy(User, optionIndex, quantity)`** — for LMSR: prices via the trading
  method, charges on-purchase commission if configured, debits the buyer
  *first* (an unaffordable trade throws before anything else changes), issues
  shares, records the holding and the trade. For Order Book this is a market
  order (see below): it may fill for less than requested.
- **`settleAndClose(winningOptionIndex)`** — takes the on-close commission from the
  winning pot, pays each winning holder their per-share payout from the pool,
  records the winner, closes (and discards any resting Order Book orders).

`TradingMethod` is the polymorphic seam for method-specific behaviour
(`baseValue`, `initialShares`, `usesOrderBook`, `allowsMinting`, `priceOf`,
`costToBuy`, `initialSubsidy`, `validate`) — callers ask the method, never
switch on its concrete type. `priceOf`/`costToBuy` are LMSR-only; for an Order
Book event `Event` prices and trades directly against the live book instead
(see below), so `OrderBookMethod`'s versions of those two are never called in
practice.

Money is `double` dollars, confined to the `Account` value object on `User`
(migrating to `BigDecimal` is future work, kept local by that class).

## XML format

```xml
<Guess-Market>
  <GM-events>
    <GM-event name="Coin flip">
      <id>1</id>
      <description>Will it land heads?</description>
      <commission type="on-purchase">10</commission>   <!-- or on-close; 0..90 -->
      <GM-options>
        <GM-option>Heads</GM-option>
        <GM-option>Tails</GM-option>                     <!-- exactly two -->
      </GM-options>
      <GM-method>
        <GM-LMSR><b>100</b></GM-LMSR>                    <!-- b > 0 -->
        <!-- or, instead of GM-LMSR: -->
        <!-- <GM-order-book d="1" initial="100" allow-mint="true"/> -->
      </GM-method>
    </GM-event>
  </GM-events>
  <GM-users>
    <GM-user name="alice">
      <initial-cash>1000</initial-cash>
      <GM-market-maker><event id="1"/></GM-market-maker>
    </GM-user>
    <GM-user name="bob"><initial-cash>500</initial-cash></GM-user>
  </GM-users>
</Guess-Market>
```

Validation (in `engine.xml`): unique event ids, exactly two non-empty options,
commission 0–90, a single valid trading method, unique user names, non-negative
cash, and exactly one market maker per event. Reading is split across
`XmlMarketReader` (file + JAXB) and `MarketAssembler` + focused validators.
XSD schema validation is not yet wired (no schema ships with the project).

## LMSR

For a binary market with liquidity parameter `b`:

```
Cost      C(q0, q1) = b · ln( e^(q0/b) + e^(q1/b) )
Price     p_i       = e^(q_i/b) / Σ e^(q_j/b)          # implied probability
Trade     cost      = C(after) - C(before)
Subsidy   C(0, 0)   = b · ln 2
```

`LmsrCalculator` uses a max-shift inside `exp()` to stay numerically stable for
large share counts.

## Order Book

A peer-to-peer limit-order market instead of a formula: every option has its
own independent book of resting bids/asks (`OrderBook`, one per option index,
each side kept in strict price-then-time priority). Every YES+NO pair is
always worth exactly the configured base value `d`; there's no single implied
price, so the UI is shown five indicators per option instead - last trade,
best bid, best ask, mid, and spread - each absent until there's enough book
activity to define it.

```xml
<GM-order-book d="1" initial="100" allow-mint="true"/>
```

- **`d`** — the base value: what a winning share pays out, and what a YES+NO
  pair is worth together (naturally `$1`, as on Polymarket, but configurable).
- **`initial`** — pairs the market maker buys at open, `initial * d` cash, in
  exchange for `initial` shares of *each* option, which are immediately posted
  for sale as two resting asks at `d / 2`.
- **`allow-mint`** — whether new share pairs may be minted from matching
  demand (see below). `initial=0` requires `allow-mint=true`, or no shares
  could ever exist.

**No escrow.** Cash and shares move only when an order actually fills; nothing
is reserved when an order is placed. A placed order is validated against the
placer's *current* balance/holdings (a seller needs the shares net of their
own other resting asks; a buyer needs the full cost, including commission, up
front), but a resting order can still go stale later (e.g. the same user has
other orders that jointly overcommit their balance) - matching then clamps
the fill to whatever the counterparty can currently deliver/afford, dropping
a resting order that can't be honoured at all rather than filling it short.

**Two ways shares come into existence:**
1. **Initial allocation** (above) - the market maker's opening purchase.
2. **Minting** - `Event.placeOrder` direct-matches a bid against the opposite
   side of the *same* option first; whatever remains, if minting is allowed,
   is offered against the best resting bid on the *other* option whenever
   their two prices sum to at least `d`. Both sides get new shares: the
   incoming bid pays the complement of the resting bid's price, the resting
   bid pays its own price in full, and `quantity * d` is poured into the
   event's pool to back the new pairs.

**`buyShares` on an Order Book event is a market order**: it sweeps the
cheapest asks up to the requested quantity, clamped fill-by-fill the same way
matching is, and never mints or rests a remainder - so it may fill for less
than requested (`TradeReceipt.filledQuantity()`/`TradeResultDTO.filledQuantity()`
report how much actually bought). `quoteTrade` walks the ask book the same
way, read-only.

Commission (on-purchase or on-close, same as LMSR) is never pooled - it's
credited straight to the market maker's account as it's collected, for
*both* trading methods.
