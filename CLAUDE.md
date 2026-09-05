# CLAUDE.md

Guidance for working in this repository.

## Build

- `mvn clean verify` — compile + run all tests (JUnit 5, Surefire). Single Maven
  reactor: `guess-market-dto` → `guess-market-engine` → `javafx-ui`.
- `mvn -pl javafx-ui javafx:run` — launch the JavaFX app.
- Requires JDK 25 (`maven.compiler.release=25` in the parent POM).
- The engine and dto modules keep sources under `src/` (not `src/main/java`);
  each POM points Maven at that layout with `<sourceDirectory>`. `javafx-ui`
  uses the standard layout.
- IntelliJ builds via the Maven import; `*.iml`, `.idea/modules.xml` and
  `.idea/libraries/` are generated and gitignored.
- If command-line Maven can't validate TLS to Maven Central, the machine has
  antivirus HTTPS scanning — use a truststore with the scanner's root CA, or the
  IDE's Maven.

## Architecture

- **One-way layering**, enforced by `module-info`: `javafx-ui` sees only
  `com.guessmarket.engine.api` + `com.guessmarket.dto`. Engine internals
  (`model`, `lmsr`, `xml`, `mapper`, `serialization`) are not exported.
- **`Event` is the aggregate root.** All trading/lifecycle rules live in it;
  it has no plain setters. `MarketEngineImpl` is a thin facade that resolves
  DTOs to domain objects (via `MarketCatalog`), calls the aggregate, and
  publishes a change notification (`MarketEventPublisher`).
- **`TradingMethod`** is the only place method-specific behaviour (pricing,
  trade cost, subsidy, config validation) belongs — never switch on its
  concrete type. `Event.open()`'s one deliberate exception is asking
  `usesOrderBook()`/`allowsMinting()` rather than reaching into `Event`
  through the method.
- **Order Book** (`OrderBookMethod`) trades through `OrderBook`/`LimitOrder`
  (per-option bid/ask books, price-then-time priority) instead of a pricing
  formula — `Event.placeOrder`/`cancelOrder`/`marketBuy` own the matching,
  minting and no-escrow fill-time clamping; `OrderBookMethod.priceOf`/
  `costToBuy` are unreachable in practice. `OrderBook`/`LimitOrder` are
  package-private to mutate but expose public read accessors so
  `engine.mapper` can build DTOs from a live book.
- **DTOs are flat**: an `EventDTO` never references a `UserDTO` and vice versa
  (events/users cross-reference by id/name).
- **Mappers convert domain → DTO only**, one direction, no recursion.
- **Persistence** is `MarketSnapshot(events, users)` via Java serialization;
  every model type on that graph is `Serializable`.
- `quoteTrade` and `buyShares` price a trade the same way `buy()` charges it —
  for LMSR literally the same `Event.quote()` call; for Order Book,
  `quoteOrderBook()`/`marketBuy()` walk the ask book identically (read-only vs.
  mutating), covered by a same-numbers test rather than one shared method.

## Conventions

- Money is `double` dollars, confined to `Account`. New money handling goes
  through `Account.debit()/credit()`, never a setter.
- Domain exceptions extend `MarketException` (unchecked); `throws` clauses on
  the API are informational. `InsufficientFundsException` and
  `XmlValidationException` are the precise subtypes.
- Tests live in `guess-market-engine/test/java`. Prefer building fixtures from
  inline XML (`@TempDir`) or directly from the model constructors.
