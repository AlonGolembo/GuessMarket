package ui.test.java;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.ui.console.ConsoleTestHelper;
import com.guessmarket.ui.console.view.ConsolePrinter;
import com.guessmarket.ui.console.view.EventDetailsPrinter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsolePrinterTest {

    @Test
    @DisplayName("ConsolePrinter should render the main menu header")
    void testPrintMenu() {
        try (ConsoleTestHelper console = new ConsoleTestHelper("")) {
            ConsolePrinter.printMenu(true);
            String output = console.getCapturedOutput();

            assertTrue(output.contains("Welcome to Guess Market!"));
            assertTrue(output.contains("Select an option"));
        }
    }

    @Test
    @DisplayName("EventDetailsPrinter should correctly output event details table")
    void testPrintEventDetails() {
        // 1. Create the base EventDTO record
        EventDTO eventInfo = new EventDTO(
                1,
                "Mujtaba is Dead",
                "This event gambles if Mujtaba is alive",
                5,
                "on-purchase",
                List.of("Hell Yea !", "No way !"),
                true
        );

        // 2. Create the EventDetailsDTO record matching your constructor parameters
        EventDetailsDTO details = new EventDetailsDTO(
                eventInfo,                                                // eventInfo
                Map.of("Hell Yea !", 0.6225, "No way !", 0.3775),         // currentOptionPrices (Map<String, Double>)
                Map.of("Hell Yea !", 50, "No way !", 0),                  // totalSharesBought (Map<String, Integer>)
                98.81,                                                    // eventAccountBalance
                1.40,                                                     // totalCommissionCollected
                List.of(),                                                // tradeHistory
                null                                                      // winningOption
        );

        try (ConsoleTestHelper console = new ConsoleTestHelper("")) {
            EventDetailsPrinter.printEventDetails(details);
            String output = console.getCapturedOutput();

            assertTrue(output.contains("EVENT DETAILS (ID: 1)"));
            assertTrue(output.contains("Mujtaba is Dead"));
            assertTrue(output.contains("98.81"));
        }
    }
}
