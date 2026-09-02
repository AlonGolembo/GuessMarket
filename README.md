# Guess Market

A prediction-market platform with an automated market maker. Users trade shares
in binary outcomes; the **Logarithmic Market Scoring Rule (LMSR)** sets prices
from demand. An event's market maker posts a subsidy to open it, traders buy
shares, and on settlement the holders of the winning option are paid.

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
- **`buy(User, optionIndex, quantity)`** — prices via the trading method, charges
  on-purchase commission if configured, debits the buyer *first* (an unaffordable
  trade throws before anything else changes), issues shares, records the holding
  and the trade.
- **`settleAndClose(winningOptionIndex)`** — takes the on-close commission from the
  winning pot, pays each winning holder their per-share payout from the pool,
  records the winner, closes.

`TradingMethod` is the polymorphic seam for method-specific behaviour
(`priceOf`, `costToBuy`, `initialSubsidy`, `validate`). `LmsrMethod` is
implemented; `OrderBookMethod` captures its config and throws
`UnsupportedOperationException` for pricing/trading until that lands.

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
