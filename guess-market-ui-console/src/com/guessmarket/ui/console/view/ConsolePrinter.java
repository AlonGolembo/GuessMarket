package com.guessmarket.ui.console.view;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.dto.TradeResultDTO;

import java.util.List;
import java.util.Map;

import static com.sun.tools.javac.jvm.Code.truncate;

public class ConsolePrinter {

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
        System.out.println("6. Save System State (Bonus)");
        System.out.println("7. Load System State (Bonus)");
        System.out.println("8. Exit");
        System.out.println("========================================");
    }

    public static void printEventList(List<EventDTO> events) {
        if (events == null || events.isEmpty()) {
            System.out.println("\nNo events found in the system.");
            return;
        }

        System.out.println("\n----------------------------------------------------------------------------------");
        System.out.printf("%-5s | %-20s | %-12s | %-12s | %-8s | %-15s%n",
                "ID", "Name", "Commission", "Comm. Type", "Status", "Options");
        System.out.println("----------------------------------------------------------------------------------");

        for (EventDTO e : events) {
            String status = e.isActive() ? "ACTIVE" : "CLOSED";
            String optionsStr = String.join(" / ", e.options());

            System.out.printf("%-5d | %-20s | %-12s | %-12s | %-8s | %-15s%n",
                    e.id(),
                    truncate(e.name(), 20),
                    e.commissionPercentage() + "%",
                    e.commissionType(),
                    status,
                    truncate(optionsStr, 15)
            );
        }
        System.out.println("----------------------------------------------------------------------------------");
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
