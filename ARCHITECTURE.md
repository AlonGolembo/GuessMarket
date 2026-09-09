# Guess Market — architecture

Mid/high-level design: the three modules, and the classes and relationships
inside each one. Rendered images are in `docs/diagrams/` (`*.png`, `*.svg`); the
Mermaid sources (`*.mmd`) are inlined below.

The build is one Maven reactor with a strict one-way dependency
`javafx-ui → guess-market-engine → guess-market-dto`, enforced by `module-info`.

---

## 1. Modules

```mermaid
flowchart TB
    UI["<b>javafx-ui</b><br/>JavaFX desktop client"]
    ENG["<b>guess-market-engine</b><br/>domain model + all trading rules<br/>LMSR · Order Book · XML · persistence"]
    DTO["<b>guess-market-dto</b><br/>flat immutable records + enums<br/>no behaviour, no dependencies"]

    UI ==>|"calls MarketEngine, exchanges DTOs,<br/>listens for changes"| ENG
    ENG ==>|"maps its domain to DTOs"| DTO
    UI -.->|"reads DTO fields"| DTO
```

The UI never sees a domain object: it calls `MarketEngine` with DTOs, renders the
DTOs it gets back, and re-pulls everything when the engine fires a change event.

---

## 2. `guess-market-dto`

```mermaid
flowchart TB
    subgraph summary["Summary DTOs"]
        EventDTO["EventDTO"]
        UserDTO["UserDTO"]
    end
    subgraph detail["Detail DTOs (summary + lists)"]
        EventDetailsDTO["EventDetailsDTO<br/>prices, pool, winner, history,<br/>holdings, book depth"]
        UserDetailsDTO["UserDetailsDTO<br/>UserDTO + balance history"]
    end
    subgraph rows["Row + result DTOs"]
        TradeHistoryDTO["TradeHistoryDTO"]
        LedgerEntryDTO["LedgerEntryDTO"]
        HoldingDTO["HoldingDTO"]
        OrderBookLevelDTO["OrderBookLevelDTO"]
        OrderBookQuoteDTO["OrderBookQuoteDTO"]
        LimitOrderDTO["LimitOrderDTO"]
        Results["TradeQuoteDTO<br/>TradeResultDTO<br/>OrderResultDTO"]
        NewEventDTO["NewEventDTO<br/>(create request)"]
    end
    subgraph enums["Enums"]
        E["CommissionType · EventStatus<br/>OrderSide · TradingMethodType"]
    end

    EventDetailsDTO -->|embeds| EventDTO
    EventDetailsDTO --> TradeHistoryDTO
    EventDetailsDTO --> HoldingDTO
    EventDetailsDTO --> OrderBookLevelDTO
    EventDetailsDTO --> OrderBookQuoteDTO
    EventDetailsDTO --> LimitOrderDTO
    UserDetailsDTO -->|embeds| UserDTO
    UserDetailsDTO --> LedgerEntryDTO
```

Every DTO is a flat, immutable `record`. A summary DTO (`EventDTO`, `UserDTO`) is
what tables show and what the UI passes back as a command argument; a detail DTO
embeds its summary DTO plus the heavy lists. `EventDTO` and `UserDTO` never
reference each other — they cross-link by id / name.

---

## 3. `guess-market-engine`

```mermaid
flowchart TB
    subgraph api["engine.api  (exported)"]
        MarketEngine["MarketEngine<br/>(interface)"]
        MarketEngineImpl["MarketEngineImpl<br/>facade: resolve · delegate · publish"]
        MarketCatalog["MarketCatalog<br/>loaded events + users · lookups"]
        Publisher["MarketEventPublisher<br/>+ MarketDataChangeListener"]
    end

    subgraph model["engine.model  (domain)"]
        Event["Event — aggregate root<br/>options, pool, holdings,<br/>lifecycle, all trading rules"]
        Option["Option"]
        User["User<br/>MM roles · blocked flag"]
        Account["Account<br/>balance + ledger history"]
        TradingMethod["TradingMethod (sealed)<br/>LmsrMethod · OrderBookMethod"]
        OrderBook["OrderBook + LimitOrder"]
        Records["TradeRecord · LedgerEntry<br/>OrderOutcome · TradeReceipt"]
    end

    subgraph support["support packages"]
        Lmsr["engine.lmsr<br/>LmsrCalculator"]
        Xml["engine.xml<br/>parser + validators<br/>&rarr; ParsedMarket"]
        Mappers["engine.mapper<br/>Event/User/Trade/Ledger<br/>Mapper"]
        Ser["engine.serialization<br/>StateSerializer + MarketSnapshot"]
        Exc["engine.exception<br/>MarketException + subtypes"]
    end

    MarketEngineImpl -.->|implements| MarketEngine
    MarketEngineImpl -->|owns| MarketCatalog
    MarketEngineImpl -->|owns| Publisher
    MarketEngineImpl -->|load| Xml
    MarketEngineImpl -->|save / restore| Ser
    MarketEngineImpl -->|commands| Event
    MarketEngineImpl -->|maps results| Mappers
    Xml -->|builds| Event
    Xml -->|builds| User
    MarketCatalog -->|holds| Event
    MarketCatalog -->|holds| User
    Ser <-->|serialize| Event
    Event -->|two| Option
    Event -->|prices via| TradingMethod
    Event -->|matches in| OrderBook
    Event -->|debit / credit| User
    Event -->|writes| Records
    User -->|money via| Account
    Account -->|appends| Records
    TradingMethod -->|maths| Lmsr
    Mappers -->|read| Event
    Mappers -->|read| User
```

`MarketEngineImpl` owns no rules: it looks objects up in `MarketCatalog`, tells
the **`Event` aggregate** to do the work (every trading and lifecycle rule lives
there), runs the result through a `*Mapper`, and fires `MarketEventPublisher`.
`TradingMethod` is the one polymorphic seam for LMSR-vs-Order-Book behaviour.
Money moves only through `Account`, which keeps the balance-over-time ledger. XML
load and Java-serialization restore are the only two ways a whole market graph
enters the catalog.

---

## 4. `javafx-ui`

```mermaid
flowchart TB
    App["GuessMarketApp<br/>entry: build engine · load FXML · inject"]

    subgraph controllers["ui.controllers"]
        Main["MainController<br/>shell: file load/save · menu<br/>engine &rarr; tabs"]
        Events["EventsController<br/>Events tab: list · filters<br/>book depth · charts · closed summary"]
        Users["UsersController<br/>Users tab: balances · holdings<br/>trading · blocked state · chart"]
    end

    subgraph common["ui.common"]
        Dialogs["Dialogs"]
        NewEventDialog["NewEventDialog<br/>&rarr; NewEventDTO"]
        ThemeManager["ThemeManager<br/>+ 5 skins"]
        Anim["AnimationSettings<br/>+ AnimationSettingsDialog"]
        Charts["Charts<br/>(LineCharts)"]
        Helpers["TradeRules · EventFilters<br/>FileLoadStatus"]
    end

    FXML["views (FXML)<br/>main-view · events-tab · users-tab"]

    App -->|loads| FXML
    App -->|setEngine| Main
    Main -->|engine| Events
    Main -->|engine| Users
    Main -->|opens| NewEventDialog
    Main -->|opens| ThemeManager
    Main -->|opens| Anim
    Main -->|errors| Dialogs
    Events -->|charts| Charts
    Users -->|charts| Charts
    Events -->|rules| Helpers
    Users -->|rules| Helpers
    Events -.->|check| Anim
    Users -.->|check| Anim
    FXML -.controller.-> Main
    FXML -.controller.-> Events
    FXML -.controller.-> Users
```

`GuessMarketApp` boots one `MarketEngineImpl` and the main window;
`MainController` injects that engine into the two tab controllers, which register
as `MarketDataChangeListener`s and re-render on every change. All bonuses live in
`ui.common`: `ThemeManager` (skins), `AnimationSettings` (toggles), `Charts`
(graphs), `NewEventDialog` (create-event).
