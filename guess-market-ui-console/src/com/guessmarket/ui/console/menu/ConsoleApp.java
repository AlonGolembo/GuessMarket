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
            int choice = 0;
            try{
                ConsolePrinter.printMenu(this.engine.isFileLoaded());
                if(this.engine.isFileLoaded()){
                    choice = inputHandler.readIntRange("Select an option (1-8)", 1, 8);
                }
                else{
                    choice = inputHandler.readIntRange("Select an option (1-3)", 1, 3);
                }
                handleMenuChoice(this.engine.isFileLoaded(), choice);
            }catch (Exception e){
                System.out.println("Unexpected Error: " + e.getMessage());
            }
        }

        System.out.println("Thank you for using Guess Market. Goodbye!");
    }

    private void handleMenuChoice(boolean isFileLoaded, int choice) {
        try {
            if(isFileLoaded){
                switch (choice) {
                    case 1 -> handleLoadXml();
                    case 2 -> handleDisplayAllEvents();
                    case 3 -> handleDisplayEventDetails();
                    case 4 -> handleBuyShares();
                    case 5 -> handleCloseEvent();
                    case 6 -> handleSaveState();
                    case 7 -> handleLoadState();
                    case 8 -> isRunning = false;
                }
            }
            else{
                switch (choice) {
                    case 1 -> handleLoadXml();
                    case 2 -> handleLoadState();
                    case 3 -> isRunning = false;
                }
            }
        } catch (Exception e) {
            System.out.println("\nEngine Error: " + e.getMessage());
        }
    }

    private void handleLoadState() {
        try {
            String filePath = inputHandler.readFilePath("Please enter the path to the file you want to load: ");
            this.engine.loadState(filePath);
            System.out.println("System state successfully restored from: " + filePath);
        } catch (MarketException e) {
            System.out.println("\nFailed to load state: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("\nAn unexpected error occurred: " + e.getMessage());
        }
    }

    private void handleSaveState() {
        try{
            String filePath = inputHandler.readFilePath("Please enter the path where you want to save the file: ");
            this.engine.saveState(filePath);
            System.out.println("File saved correctly at: " + filePath);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void handleCloseEvent() {
        try{
            List<EventDTO> activeEventsList = this.engine.getActiveEvents();
            ConsolePrinter.printEventList(activeEventsList);

            int eventId = inputHandler.readPositiveInt("Please insert event ID you wish to close: ");

            EventDetailsDTO selectedEvent = this.engine.getEventDetails(eventId);
            ConsolePrinter.printEventDetails(selectedEvent);

            int optionIndex = inputHandler.readIntRange("Please insert winning option index: ", 1, 2);
            this.engine.closeEvent(eventId, optionIndex);
            System.out.println("Event ID " + eventId + " closed and paid out");

            selectedEvent = this.engine.getEventDetails(eventId);
            ConsolePrinter.printEventDetails(selectedEvent);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void handleBuyShares() {
        try{
            List<EventDTO> activeEventsList = this.engine.getActiveEvents();
            ConsolePrinter.printEventList(activeEventsList);

            int eventId = inputHandler.readPositiveInt("Please insert event ID: ");

            EventDetailsDTO selectedEvent = this.engine.getEventDetails(eventId);
            ConsolePrinter.printEventDetails(selectedEvent);

            int optionIndex = inputHandler.readIntRange("Please insert option index: ", 1, 2);
            int numberOfShares = inputHandler.readPositiveInt("Please insert number of shares to purchase: ");
            TradeResultDTO tradeResult = this.engine.buyShares(eventId, optionIndex, numberOfShares);

            System.out.println("Purchase successful!\n");
            System.out.println("Total amount payed: " + tradeResult.totalPaid());
            if(tradeResult.commissionCost() > 0){
                System.out.println("Commission payed: " +tradeResult.commissionCost());
            }

            selectedEvent = this.engine.getEventDetails(eventId);
            ConsolePrinter.printEventDetails(selectedEvent);

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void handleDisplayEventDetails() {
        try{
            List<EventDTO> eventsList = this.engine.getAllEvents();
            ConsolePrinter.printEventList(eventsList);

            int eventId = inputHandler.readPositiveInt("Please insert event ID: ");
            EventDetailsDTO eventDetails = this.engine.getEventDetails(eventId);
            ConsolePrinter.printEventDetails(eventDetails);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void handleDisplayAllEvents() {
        try{
            List<EventDTO> eventsList = this.engine.getAllEvents();
            ConsolePrinter.printEventList(eventsList);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void handleLoadXml() {
        if (this.engine.isFileLoaded()) {
            String warning = "An XML file is already loaded. This action may override the loaded data.";
            if (!confirmAction(warning)) {
                return; // Abort if user chose No
            }
        }

        // Proceed with loading the file...
        String xmlFilePath = this.inputHandler.readFilePath("Please insert XML file path: ");
        try{
            this.engine.loadXmlFile(xmlFilePath);
            int numberOfLoadedEvents = this.engine.getNumOfLoadedEvents();
            System.out.println("XML Successfully loaded!");
            System.out.println("\nLoaded " + numberOfLoadedEvents + " events in the system.");
        }catch(MarketException e){
            System.out.println("New file wasn't loaded!");
            System.out.println(e.getMessage() + "\nPlease insert a valid file path.");
        }
    }

    private boolean confirmAction(String warningMessage) {
        System.out.println(warningMessage);
        int choice = inputHandler.readIntRange("1. Yes\n2. No", 1, 2);
        return choice == 1;
    }
}
