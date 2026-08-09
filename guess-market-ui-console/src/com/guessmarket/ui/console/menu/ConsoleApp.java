package com.guessmarket.ui.console.menu;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeResultDTO;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.api.MarketEngineImpl;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.ui.console.view.ConsolePrinter;

import java.util.List;

public class ConsoleApp {
    private final MarketEngine engine;
    private final InputHandler inputHandler;
    private boolean isRunning;

    public ConsoleApp() {
        this.engine = new MarketEngineImpl();
        this.inputHandler = new InputHandler();
        this.isRunning = true;
    }

    public static void main(String[] args){
        ConsoleApp app = new ConsoleApp();
        app.run();
    }

    private void run() {
        System.out.println("Welcome to Guess Market!");
        while(isRunning){
            try{
                ConsolePrinter.printMenu();
                int choice = inputHandler.readIntRange("Select an option (1-8)", 1, 8);
                handleMenuChoice(choice);
            }catch (Exception e){
                System.out.println("Unexpected Error: " + e.getMessage());
            }
        }

        System.out.println("Thank you for using Guess Market. Goodbye!");
    }

    private void handleMenuChoice(int choice) {
        try {
            switch (choice) {
                case 1 -> handleLoadXml(); // Done
                case 2 -> handleDisplayAllEvents();
                case 3 -> handleDisplayEventDetails();
                case 4 -> handleBuyShares();
                case 5 -> handleCloseEvent();
                case 6 -> handleSaveState();
                case 7 -> handleLoadState();
                case 8 -> isRunning = false;
            }
        } catch (Exception e) {
            System.out.println("\nEngine Error: " + e.getMessage());
        }
    }

    private void handleLoadState() {
    }

    private void handleSaveState() {
    }

    private void handleCloseEvent() {
    }

    private void handleBuyShares() {
    }

    private void handleDisplayEventDetails() {
    }

    private void handleDisplayAllEvents() {
        try{
            List<EventDTO> eventsList = engine.getAllEvents();
            ConsolePrinter.printEventList(eventsList);
        }catch (MarketException e){
            System.out.println(e.getMessage());
        }
    }

    private void handleLoadXml() {
        String xmlFilePath = this.inputHandler.readFilePath("Please insert XML file path: ");

        try{
            this.engine.loadXmlFile(xmlFilePath);
        }catch(MarketException e){
            System.out.println(e.getMessage() + "\nPlease insert a valid file path.");
        }
    }
}
