================================================================================
                           GUESS MARKET ENGINE & UI
================================================================================

--------------------------------------------------------------------------------
1. PROJECT STRUCTURE
--------------------------------------------------------------------------------

GuessMarket/
├── guess-market-dto/                  [Data Transfer Objects Module]
│   └── src/com/guessmarket/dto/
│       ├── EventDTO.java              (Summary info for event listing)
│       ├── EventDetailsDTO.java       (Full trading state & history)
│       ├── TradeHistoryDTO.java       (Individual trade audit log record)
│       └── TradeResultDTO.java        (Purchase receipt data)
│
├── guess-market-engine/               [Core Domain & Logic Module]
│   └── src/com/guessmarket/engine/
│       ├── api/                       (MarketEngine & MarketEngineImpl)
│       ├── exception/                 (MarketException, XmlValidationException)
│       ├── lmsr/                      (LmsrCalculator - cost & pricing math)
│       ├── mapper/                    (EventMapper - entity to DTO converters)
│       ├── model/                     (Event, Option, TradeRecord, Commission)
│       ├── serialization/             (StateSerializer - save/load state)
│       └── xml/                       (XmlEventParser - DOM parsing & validation)
│
└── guess-market-ui-console/           [Console Presentation Module]
    └── src/com/guessmarket/ui/console/
        ├── menu/                      (ConsoleApp main loop, InputHandler)
        └── view/                      (ConsolePrinter - text table views)


--------------------------------------------------------------------------------
2. MAIN DESIGN DECISIONS
--------------------------------------------------------------------------------

* Multi-Module Architecture: Decouples domain logic (engine) from data transfer
  contracts (dto) and user interface views (ui-console).

* Read-Only DTOs: Domain models (Event, Option) never leak into the presentation 
  layer. The UI receives only immutable Record DTOs.

* Numerical Protection in LMSR Math: Option pricing using the softmax 
  exponential formula uses a max-shift subtraction trick inside LmsrCalculator 
  to prevent Double.POSITIVE_INFINITY floating-point overflow and NaN errors.

* Centralized Dynamic View Formatting: ConsolePrinter auto-calculates border
  lengths and formats columns based on configurable width constants, preventing
  terminal layout misalignments.

* Robust Input Parsing: InputHandler consumes entire input lines via 
  scanner.nextLine() to prevent standard Scanner bugs and strips Windows double 
  quotes from pasted file paths automatically.


--------------------------------------------------------------------------------
3. COMPLETED COMPONENTS
--------------------------------------------------------------------------------

[x] LMSR Math Engine (cost functions, trade pricing, probability calculations)
[x] XML Parser & Schema Validation (loads and constructs market events)
[x] DTO Mapping Infrastructure (EventMapper)
[x] Interactive Console Menu & Safe Input Processing (InputHandler)
[x] Command 1: Load System XML File
[x] Command 2: Display All Events Summary
[x] Command 3: Display Event Trading Details & Audit Log
[x] Command 4: Buy Option Shares & Output Receipt
[x] Application Exit Sequence (Command 8)
[x] Command 5: Close Event & Declare Winner Logic in MarketEngine
[x] Command 6: Save System State (File Serialization Output)
[x] Command 7: Load System State (File Deserialization Recovery)
[x] Refactor XML Parser to use JAXB instead of DOM


--------------------------------------------------------------------------------
4. REMAINING WORK
--------------------------------------------------------------------------------

[ ] Add verifications for overriding data (load an xml when one is already loaded,
    load saved state when there is an xml loaded, etc).
[ ] Complete remaining work when I get answers to the questions below.
[ ] Test and improve. Check if there should be more printing and if there are any stages
    that aren't clear.
[ ] Fix message printed to user when an xml file has errors in it (com/guessmarket/ui/console/menu/ConsoleApp.java:162)

--------------------------------------------------------------------------------
5. OPEN QUESTIONS & DECISIONS
--------------------------------------------------------------------------------
[X] When there is a big difference between the amount of shares bought of each option,
    the event details print out share price 0.00 (for the share that was bought less)
    since there is a requirement for displaying only 2 digits after the decimal point.
    Should I keep this this way or treat this case differently?
[ ] When closing an event, should I print how much money was paid out? How many winners
    where paid? Maybe I should create another EventResultDTO (similar to TradeResultDTO)
    for closing an event?
================================================================================