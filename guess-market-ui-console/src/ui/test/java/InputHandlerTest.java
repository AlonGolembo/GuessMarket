package ui.test.java;

import com.guessmarket.ui.console.ConsoleTestHelper;
import com.guessmarket.ui.console.menu.InputHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputHandlerTest {

    @Test
    @DisplayName("readIntRange should return valid choice on first attempt")
    void testReadIntRangeValidFirstTry() {
        String simulatedInput = "2\n";

        try (ConsoleTestHelper console = new ConsoleTestHelper(simulatedInput)) {
            InputHandler inputHandler = new InputHandler();
            int choice = inputHandler.readIntRange("Select option (1-3)", 1, 3);

            assertEquals(2, choice);
        }
    }

    @Test
    @DisplayName("readIntRange should reject out-of-bounds input and retry until valid")
    void testReadIntRangeRetryOnInvalid() {
        // Simulates entering 5 (out of range), then 'abc' (invalid type), then 1 (valid)
        String simulatedInput = "5\nabc\n1\n";

        try (ConsoleTestHelper console = new ConsoleTestHelper(simulatedInput)) {
            InputHandler inputHandler = new InputHandler();
            int choice = inputHandler.readIntRange("Select option (1-3)", 1, 3);

            assertEquals(1, choice);
            String output = console.getCapturedOutput();
            assertTrue(output.contains("Select option (1-3)"));
        }
    }
}
