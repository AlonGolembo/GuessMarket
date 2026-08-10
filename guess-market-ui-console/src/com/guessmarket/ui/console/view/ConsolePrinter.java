package com.guessmarket.ui.console.view;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;

import java.util.List;

public class ConsolePrinter {

    // Events list columns


    // Event details columns


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

    public static void printEventList(List<EventDTO> events){
        EventListPrinter.printEventList(events);
    }

    public static void printEventDetails(EventDetailsDTO details){
        EventDetailsPrinter.printEventDetails(details);
    }

    static String padOrTruncate(String text, int maxLength) {
        if (text == null) {
            text = "";
        }
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text;
    }


}
