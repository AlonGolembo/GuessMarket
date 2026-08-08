Project layout and Design:

GuessMarket/
├── guess-market-engine/                    # Module 1: Core Engine JAR
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── guessmarket/
│                       └── engine/
│                           ├── api/         # MarketEngine interface & EngineImpl
│                           ├── exception/   # Custom domain exceptions (e.g., InvalidXmlException)
│                           ├── model/       # Event, Option, Account, MarketMaker, Order
│                           ├── lmsr/        # LMSR Math & Calculator engine
│                           ├── serialization/# Bonus 1: Save/Load state mechanics
│                           └── xml/         # JAXB / DOM Parsers & Schema validation
│
├── guess-market-dto/                       # Module 2: Shared DTOs JAR
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── guessmarket/
│                       └── dto/             # EventDTO, AccountDTO, TradeResultDTO, etc.
│
└── guess-market-ui-console/                # Module 3: Console UI Application (Main)
    └── src/
        └── main/
            └── java/
                └── com/
                    └── guessmarket/
                        └── ui/
                            └── console/
                                ├── Main.java         # Entry point (main method)
                                ├── ConsoleApp.java   # Main loop & state coordinator
                                ├── menu/             # Menu options (Command pattern or Enum)
                                └── view/             # Formatting & System.out printers