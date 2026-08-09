package com.guessmarket.ui.console.view;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeHistoryDTO;

import java.util.List;
import java.util.Map;

public class ConsolePrinter {

    // Events list columns
    private static final int COL_ID_WIDTH = 5;
    private static final int COL_NAME_WIDTH = 25;
    private static final int COL_COMM_WIDTH = 12;
    private static final int COL_COMM_TYPE_WIDTH = 14;
    private static final int COL_STATUS_WIDTH = 10;
    private static final int COL_OPTIONS_WIDTH = 20;

    // Event details columns
    private static final int COL_PRICE_OPTION_WIDTH = 20;
    private static final int COL_PRICE_PROB_WIDTH = 20;
    private static final int COL_PRICE_SHARES_WIDTH = 18;
    private static final int COL_TRADE_OPTION_WIDTH = 20;
    private static final int COL_TRADE_QTY_WIDTH = 15;
    private static final int COL_TRADE_PAID_WIDTH = 18;

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

    /**
     * Prints full trading status and audit details for a specific event (Command 3).
     */
    public static void printEventDetails(EventDetailsDTO details) {
        if (details == null) {
            System.out.println("\nNo details available for this event.");
            return;
        }

        EventDTO info = details.eventInfo();

        System.out.println("\n==================================================================");
        System.out.printf("                      EVENT DETAILS (ID: %d)%n", info.id());
        System.out.println("==================================================================");
        System.out.println("Name         : " + info.name());
        System.out.println("Description  : " + info.description());
        System.out.println("Status       : " + (info.isActive() ? "ACTIVE" : "CLOSED"));

        if (!info.isActive()) {
            String winner = (details.winningOption() != null) ? details.winningOption() : "N/A";
            System.out.println("Winner       : " + winner);
        }

        System.out.println("Commission   : " + info.commissionPercentage() + "% (" + info.commissionType() + ")");
        System.out.printf("Acc. Balance : $%.2f%n", details.eventAccountBalance());
        System.out.printf("Total Fees   : $%.2f%n", details.totalCommissionCollected());

        // 1. Render Option Pricing & Shares Summary
        printOptionPricingTable(info.options(), details.currentOptionPrices(), details.totalSharesBought());

        // 2. Render Trade History Audit Log
        printTradeHistoryTable(details.tradeHistory());

        System.out.println("==================================================================\n");
    }

    private static void printOptionPricingTable(List<String> options,
                                                Map<String, Double> prices,
                                                Map<String, Integer> sharesBought) {

        int totalWidth = COL_PRICE_OPTION_WIDTH + COL_PRICE_PROB_WIDTH + COL_PRICE_SHARES_WIDTH + (2 * 3);
        String border = "-".repeat(totalWidth);
        String format = String.format("%%-%ds | %%-%ds | %%-%ds%%n",
                COL_PRICE_OPTION_WIDTH, COL_PRICE_PROB_WIDTH, COL_PRICE_SHARES_WIDTH);

        System.out.println("\n--- Current Option Pricing & Distribution ---");
        System.out.println(border);
        System.out.printf(format, "Option", "Price (Prob %)", "Shares Bought");
        System.out.println(border);

        for (String optionName : options) {
            double price = prices != null ? prices.getOrDefault(optionName, 0.0) : 0.0;
            int totalShares = sharesBought != null ? sharesBought.getOrDefault(optionName, 0) : 0;
            double probabilityPct = price * 100.0;

            String priceFormatted = String.format("$%.2f (%.1f%%)", price, probabilityPct);

            System.out.printf(format,
                    padOrTruncate(optionName, COL_PRICE_OPTION_WIDTH),
                    padOrTruncate(priceFormatted, COL_PRICE_PROB_WIDTH),
                    padOrTruncate(String.valueOf(totalShares), COL_PRICE_SHARES_WIDTH)
            );
        }
        System.out.println(border);
    }

    private static void printTradeHistoryTable(List<TradeHistoryDTO> history) {
        System.out.println("\n--- Trade History Audit Log ---");

        if (history == null || history.isEmpty()) {
            System.out.println("No purchase transactions recorded yet.");
            return;
        }

        int totalWidth = COL_TRADE_OPTION_WIDTH + COL_TRADE_QTY_WIDTH + COL_TRADE_PAID_WIDTH + (2 * 3);
        String border = "-".repeat(totalWidth);
        String format = String.format("%%-%ds | %%-%ds | %%-%ds%%n",
                COL_TRADE_OPTION_WIDTH, COL_TRADE_QTY_WIDTH, COL_TRADE_PAID_WIDTH);

        System.out.println(border);
        System.out.printf(format, "Option Selected", "Quantity", "Total Paid");
        System.out.println(border);

        for (TradeHistoryDTO trade : history) {
            String totalPaidFormatted = String.format("$%.2f", trade.pricePaid());

            System.out.printf(format,
                    padOrTruncate(trade.optionName(), COL_TRADE_OPTION_WIDTH),
                    padOrTruncate(String.valueOf(trade.quantity()), COL_TRADE_QTY_WIDTH),
                    padOrTruncate(totalPaidFormatted, COL_TRADE_PAID_WIDTH)
            );
        }
        System.out.println(border);
    }
}
