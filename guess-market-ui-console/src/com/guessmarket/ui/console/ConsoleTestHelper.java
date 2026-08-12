package com.guessmarket.ui.console;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

public class ConsoleTestHelper implements AutoCloseable {
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public ConsoleTestHelper(String userInputs) {
        // Feed user inputs separated by newlines
        System.setIn(new ByteArrayInputStream(userInputs.getBytes()));
        // Redirect System.out to capture console output
        System.setOut(new PrintStream(outputStream));
    }

    public String getCapturedOutput() {
        return outputStream.toString();
    }

    @Override
    public void close() {
        // Restore original system streams
        System.setIn(originalIn);
        System.setOut(originalOut);
    }
}