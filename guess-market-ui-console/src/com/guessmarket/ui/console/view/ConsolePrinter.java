package com.guessmarket.ui.console.view;

import com.guessmarket.dto.EventDTO;

import java.util.List;

public class ConsolePrinter {

    // Define column widths in one central place
    private static final int COL_ID_WIDTH = 5;
    private static final int COL_NAME_WIDTH = 25;
    private static final int COL_COMM_WIDTH = 12;
    private static final int COL_COMM_TYPE_WIDTH = 14;
    private static final int COL_STATUS_WIDTH = 10;
    private static final int COL_OPTIONS_WIDTH = 20;

    public static void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("           GUESS MARKET MENU            ");
        System.out.println("========================================");
        System.out.println("1. Load System XML File");
        System.out.println("2. Display All Events Summary");
        System.out.println("3. Display Event Trading Details");
        System.out.println("4. Buy Option Shares");
        System.out.println("5. Close Event & Declare Winner");
        System.out.println("6. Save System State");
        System.out.println("7. Load System State");
        System.out.println("8. Exit");
        System.out.println("========================================");
    }

    /**
     * Prints a summary table of all loaded events (Command 2).
     */
    public static void printEventList(List<EventDTO> events) {
        if (events == null || events.isEmpty()) {
            System.out.println("\nNo events found in the system.");
            return;
        }

        // Calculate total row width: sum of column widths + separator lengths (" | ")
        // 6 columns separated by 5 " | " bars (each 3 chars long)
        int totalWidth = COL_ID_WIDTH + COL_NAME_WIDTH + COL_COMM_WIDTH
                + COL_COMM_TYPE_WIDTH + COL_STATUS_WIDTH + COL_OPTIONS_WIDTH + (5 * 3);

        String borderRow = "-".repeat(totalWidth);

        // Build dynamic format string for header and rows: e.g. "%-5s | %-25s | %-12s | ..."
        String rowFormat = String.format("%%-%ds | %%-%ds | %%-%ds | %%-%ds | %%-%ds | %%-%ds%%n",
                COL_ID_WIDTH, COL_NAME_WIDTH, COL_COMM_WIDTH,
                COL_COMM_TYPE_WIDTH, COL_STATUS_WIDTH, COL_OPTIONS_WIDTH);

        System.out.println("\n" + borderRow);
        System.out.printf(rowFormat, "ID", "Name", "Commission", "Comm. Type", "Status", "Options");
        System.out.println(borderRow);

        for (EventDTO e : events) {
            String status = e.isActive() ? "ACTIVE" : "CLOSED";
            String optionsStr = String.join(" / ", e.options());

            System.out.printf(rowFormat,
                    e.id(),
                    padOrTruncate(e.name(), COL_NAME_WIDTH),
                    e.commissionPercentage() + "%",
                    e.commissionType(),
                    status,
                    padOrTruncate(optionsStr, COL_OPTIONS_WIDTH)
            );
        }

        System.out.println(borderRow);
    }

    private static String padOrTruncate(String text, int maxLength) {
        if (text == null) {
            text = "";
        }
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text;
    }
}
