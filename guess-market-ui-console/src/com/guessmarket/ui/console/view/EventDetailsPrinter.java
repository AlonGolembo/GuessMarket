package com.guessmarket.ui.console.view;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeHistoryDTO;

import java.util.List;
import java.util.Map;

class EventDetailsPrinter {
    private static final int COL_PRICE_OPTION_WIDTH = 20;
    private static final int COL_PRICE_PROB_WIDTH = 20;
    private static final int COL_PRICE_SHARES_WIDTH = 18;
    private static final int COL_TRADE_OPTION_WIDTH = 20;
    private static final int COL_TRADE_QTY_WIDTH = 15;
    private static final int COL_TRADE_PAID_WIDTH = 18;

    /**
     * Prints full trading status and audit details for a specific event (Command 3).
     */
    static void printEventDetails(EventDetailsDTO details) {
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
                    ConsolePrinter.padOrTruncate(optionName, COL_PRICE_OPTION_WIDTH),
                    ConsolePrinter.padOrTruncate(priceFormatted, COL_PRICE_PROB_WIDTH),
                    ConsolePrinter.padOrTruncate(String.valueOf(totalShares), COL_PRICE_SHARES_WIDTH)
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
                    ConsolePrinter.padOrTruncate(trade.optionName(), COL_TRADE_OPTION_WIDTH),
                    ConsolePrinter.padOrTruncate(String.valueOf(trade.quantity()), COL_TRADE_QTY_WIDTH),
                    ConsolePrinter.padOrTruncate(totalPaidFormatted, COL_TRADE_PAID_WIDTH)
            );
        }
        System.out.println(border);
    }
}
