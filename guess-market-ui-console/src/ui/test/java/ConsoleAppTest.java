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
    @DisplayName("ConsoleApp should gracefully handle exit command (Option 3)")
    void testExitFlow() {
        String nl = System.lineSeparator();
        // Ensure all prompts are answered and trailing newline is included
        String simulatedInput = "admin" + nl + "3" + nl + "Y" + nl;

        try (ConsoleTestHelper console = new ConsoleTestHelper(simulatedInput)) {
            String[] args = {"--config", "test.json"};
            ConsoleApp.main(args);

            String output = console.getCapturedOutput();
            assertTrue(output.contains("Welcome to Guess Market!"));
        }
    }

    @Test
    @DisplayName("Console should successfully load XML")
    void loadXmlFlow(){
        String filePath = "C:\\Users\\Alon Golembo\\Downloads\\multiple.xml";
        try(ConsoleTestHelper console = new ConsoleTestHelper("admin\n1\n" + filePath + "\n8\n")){
            String[] args = {"--config", "test.json"};
            ConsoleApp.main(args);

            String output = console.getCapturedOutput();
            assertTrue(output.contains("XML Successfully loaded"));
        }
    }
}