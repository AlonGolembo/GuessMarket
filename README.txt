================================================================================
                         GUESS MARKET ENGINE & UI
           A Prediction Market Platform with LMSR Automated Market Maker
================================================================================


================================================================================
1. PROJECT OVERVIEW
================================================================================

Guess Market is a Java-based prediction market system using the Logarithmic
Market Scoring Rule (LMSR) algorithm to automate option pricing and market
dynamics. The system allows users to trade binary outcome options, with prices
determined by the LMSR cost function and market-driven demand.

Key Features:
  • LMSR-based automated market maker with dynamic price discovery
  • Binary event trading (two mutually exclusive outcomes)
  • Configurable commission models (on-trade or on-close)
  • XML-based event configuration and schema validation
  • Event settlement and winning option payout distribution
  • Market state persistence (save/load)
  • Interactive console UI for market operations


================================================================================
2. ARCHITECTURE & MODULE STRUCTURE
================================================================================

The project follows a clean, layered multi-module architecture:

┌─────────────────────────────────────────────────────────────┐
│                  guess-market-ui-console                    │
│            (Console UI - Menu, Input, Display)              │
└─────────────────────┬───────────────────────────────────────┘
                      │ uses
┌─────────────────────▼───────────────────────────────────────┐
│                  guess-market-engine                        │
│          (Domain Logic, LMSR Math, State Management)        │
└─────────────────────┬───────────────────────────────────────┘
                      │ produces
┌─────────────────────▼───────────────────────────────────────┐
│                   guess-market-dto                          │
│          (Immutable Data Transfer Objects)                  │
└─────────────────────────────────────────────────────────────┘


Module Breakdown:

► guess-market-dto/
  Data Transfer Objects representing the public API contracts:
  
  - EventDTO: Summary snapshot of an event
    (id, name, description, commissionPercentage, commissionType, options, isActive)
  
  - EventDetailsDTO: Complete trading state and history
    (eventInfo, currentOptionPrices, totalSharesBought, eventAccountBalance,
     totalCommissionCollected, tradeHistory, winningOption)
  
  - TradeResultDTO: Trade receipt data
    (eventId, optionName, quantityPurchased, sharePricePerUnit, totalPaid,
     commissionCost, timestamp)
  
  - TradeHistoryDTO: Individual trade audit log entry
    (timestamp, optionName, quantityBought, sharePricePerUnit, totalCost)


► guess-market-engine/
  Core business logic and domain models:
  
  api/
    - MarketEngine (interface): Public API contract
    - MarketEngineImpl: Main orchestrator for market operations
  
  model/
    - Event: Aggregate root representing a prediction market event
      (Contains options, tracks balance, manages trade history, lifecycle)
    - Option: Binary outcome with tracked share quantities
    - TradeRecord: Audit log entry for each transaction
    - CommissionType: Enum (ON_TRADE, ON_CLOSE)
  
  lmsr/
    - LmsrCalculator: All LMSR mathematical operations
      • calculateCost(qYes, qNo, b): Market cost function
      • calculateOptionPrice(qTarget, qOther, b): Implied probability
      • calculateTradeCost(...): Price for a trade
      • calculateInitialSubsidy(b): Market maker subsidy
  
  xml/
    - XmlEventParser: JAXB-based XML unmarshalling and validation
      (Loads and constructs Event objects from XML files)
    - JAXB classes: GuessMarketXml, EventXml, OptionXml, etc.
  
  mapper/
    - EventMapper: Converts domain Event → DTOs (EventDTO, EventDetailsDTO)
  
  serialization/
    - StateSerializer: Binary persistence of market state (save/load)
  
  exception/
    - MarketException: General domain error
    - XmlValidationException: XML parsing/validation error


► guess-market-ui-console/
  Interactive console presentation layer:
  
  menu/
    - ConsoleApp: Main application loop and command dispatcher
    - InputHandler: Safe input parsing (handles Scanner edge cases,
      Windows quote stripping, file path validation)
  
  view/
    - ConsolePrinter: Centralized output formatting
    - EventListPrinter: Formats event summary tables
    - EventDetailsPrinter: Formats detailed event trading view


================================================================================
3. CORE DESIGN DECISIONS
================================================================================

► Immutable DTOs & Domain Isolation
  Domain models (Event, Option) never leak to the UI layer.
  All presentation receives only immutable Record DTOs, preventing
  accidental state modification and enforcing clean boundaries.

► LMSR Mathematical Safety
  The LmsrCalculator uses a max-shift subtraction trick inside Math.exp()
  to prevent Double.POSITIVE_INFINITY overflow when dealing with large
  share quantities. Formula:
    exp((q - max) / b) instead of exp(q / b)
  This maintains numerical stability across extreme market states.

► Event Lifecycle Management
  Events transition: Active → Closed
  The Event class encapsulates this state machine and enforces invariants
  (e.g., cannot buy shares from closed events, cannot close twice).

► Two Commission Models
  - ON_TRADE: Deducted at purchase time
  - ON_CLOSE: Deducted from winning payouts when event settles
  Configured per-event in XML and enforced uniformly during trading.

► Binary Market Constraint
  Events support exactly two outcomes (enforced in Event constructor).
  This simplifies the pricing model and matches typical use cases.

► Input Safety
  InputHandler consumes entire lines via scanner.nextLine() to avoid
  Scanner.next() bugs. Automatically strips Windows double quotes from
  file paths (e.g., "C:\file.xml" → C:\file.xml).

► Pluggable Persistence
  StateSerializer abstracts save/load logic, enabling future migration
  to database or network backends without touching domain code.


================================================================================
4. MarketEngine API (Public Interface)
================================================================================

Interface: com.guessmarket.engine.api.MarketEngine

Core Operations:

  void loadXmlFile(String filePath)
    → Parses XML, validates events, resets market state
    → Throws: MarketException, XmlValidationException

  boolean isFileLoaded()
    → Returns true if a valid Events XML is currently loaded

  List<EventDTO> getAllEvents()
    → Returns summary snapshots of all events (active & closed)

  List<EventDTO> getActiveEvents()
    → Returns summary snapshots of only active events

  EventDetailsDTO getEventDetails(int eventId)
    → Returns complete trading state: prices, shares, balance,
      trade history, winning option (if closed)

  TradeResultDTO buyShares(int eventId, int optionIndex1Based, int quantity)
    → Executes purchase of option shares
    → Returns: receipt with cost breakdown
    → Option index is 1-based for user-friendliness

  void closeEvent(int eventId, int winningOptionIndex1Based)
    → Settles market by declaring winner, applying on-close commission,
      and marking event as inactive

  void saveState(String filePath)
    → Serializes entire market state to file

  void loadState(String filePath)
    → Deserializes and restores previous market state

  int getNumOfLoadedEvents()
    → Returns count of currently loaded events


================================================================================
5. LMSR: Mathematical Foundation
================================================================================

The Logarithmic Market Scoring Rule is an automated market maker algorithm
that:

1. Sets option prices based on total shares bought of each outcome
2. Ensures no arbitrage and incentivizes information revelation
3. Uses an exponential cost function to automatically adjust prices

Key Formulas (for binary outcomes Yes/No with liquidity parameter b):

  Cost Function C(q_yes, q_no) = b * ln(e^(q_yes/b) + e^(q_no/b))
    - Determines total system liability
    - Always non-decreasing

  Option Price p_yes = e^(q_yes/b) / (e^(q_yes/b) + e^(q_no/b))
    - Probability belief implied by market state
    - Ranges [0, 1]

  Trade Cost = C(new_state) - C(old_state)
    - What buyer pays to purchase shares
    - Increases as shares bought increase (slippage)

Numerical Stability:
  Uses max-shift trick: exp((q - max(q)) / b) to prevent overflow
  Maintains precision even with q > 1000


================================================================================
6. XML Event Configuration Format
================================================================================

Events are loaded from XML files structured as:

  <GM-root>
    <GM-event name="Event Name">
      <id>1</id>
      <description>Event description text</description>
      <comision type="ON_TRADE" or "ON_CLOSE">
        <value>15</value>  <!-- 0-90% -->
      </comision>
      <GM-option>Yes</GM-option>
      <GM-option>No</GM-option>
      <GM-method>
        <GM-LMSR>
          <b>100</b>  <!-- Liquidity parameter, must be > 0 -->
        </GM-LMSR>
      </GM-method>
    </GM-event>
    <!-- More events... -->
  </GM-root>

Validation Rules:
  • Event ID must be unique and positive
  • Event must have exactly 2 options
  • Commission must be 0-90%
  • Liquidity parameter 'b' must be > 0
  • All required fields (name, description, id, commission) must be present
  • File must be valid XML with proper structure


================================================================================
7. USER INTERACTION FLOW
================================================================================

Console Menu (Initial State - No File Loaded):
  1. Load System XML File
  2. Load System State (from saved file)
  3. Exit

Console Menu (File/State Loaded):
  1. Load System XML File (with override warning)
  2. Display All Events Summary
  3. Display Event Trading Details & History
  4. Buy Option Shares (with purchase receipt)
  5. Close Event & Declare Winner
  6. Save System State
  7. Load System State (with override warning)
  8. Exit

Buy Shares Flow:
  1. User selects from list of active events
  2. User views event details (prices, history)
  3. User selects option (1-based index)
  4. User enters quantity
  5. System calculates cost + commission
  6. Purchase executes, receipt displays
  7. Updated event state displays

Close Event Flow:
  1. User selects from list of active events
  2. User views current prices and history
  3. User declares winning option (1-based index)
  4. System:
     - Calculates on-close commission (if applicable)
     - Marks event inactive
     - Records winning option
  5. Final event state displays


================================================================================
8. STATE PERSISTENCE
================================================================================

The system can save and restore its entire state:

Save (Command 6):
  → Serializes all Event objects and their trade history
  → Writes to user-specified file path
  → Preserves event lifecycle (active/closed) and all balances

Load (Command 7):
  → Deserializes state from saved file
  → Restores all events, options, prices, and trade history
  → Enables market continuity across sessions


================================================================================
9. NOTABLE IMPLEMENTATION DETAILS
================================================================================

► Option Pricing Edge Cases
  When share distribution is highly unequal (e.g., 1000:1), the lower-
  traded option may display as $0.00 due to 2-decimal rounding. This is
  expected behavior and reflects market reality (extreme probability skew).

► Trade History Ordering
  Trades are stored newest-first for immediate accessibility and UI display.
  getTradeHistory() returns the list in reverse chronological order.

► Commission Deduction Timing
  - ON_TRADE: Deducted during purchase, affects total cost
  - ON_CLOSE: Deducted from winning payouts during event settlement
  Both types contribute to event's commission balance.

► Winning Payout Distribution
  Currently, Event.distributeWinningPayouts() is a placeholder.
  When user/portfolio tracking is implemented, it will iterate through
  winning shareholders and credit ($1.00 - commission) per winning share.

► Input Handling
  InputHandler.readFilePath() automatically strips Windows double quotes,
  allowing users to paste file paths directly from file explorer.


================================================================================
10. EXCEPTION HANDLING
================================================================================

MarketException: Thrown for domain violations
  - Closing already-closed events
  - Buying from closed events
  - Invalid event/option selection
  - Serialization failures

XmlValidationException: Thrown for XML parsing errors
  - Malformed XML
  - Missing required fields
  - Invalid commission/liquidity values
  - Duplicate event IDs
  - File not found


================================================================================
11. FUTURE EXTENSIONS
================================================================================

Identified placeholders and extensibility points:

  • User/Portfolio System: Track individual user balances and holdings
  • Payout Distribution: Implement winning shareholder payouts
  • Advanced Reporting: Trade analytics, probability evolution charts
  • API Layer: REST API for programmatic market interaction
  • Database Persistence: Replace file serialization with DB
  • Multi-Outcome Events: Generalize from binary to k-way outcomes


================================================================================
