package ui.test.java;

import com.guessmarket.ui.console.ConsoleTestHelper;
import com.guessmarket.ui.console.menu.ConsoleApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleAppTest {

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("ConsoleApp should gracefully handle exit command (Option 8)")
    void testExitFlow() {
        // User selects option '8' to exit immediately
        String simulatedInput = "admin\n8\nY\n";

        try (ConsoleTestHelper console = new ConsoleTestHelper(simulatedInput)) {
            String[] args = {"--config", "test.json"};
            ConsoleApp.main(args);

            String output = console.getCapturedOutput();
            assertTrue(output.contains("Welcome to Guess Market!"));
        }
    }
}