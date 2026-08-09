package com.guessmarket.ui.console.menu;

import java.util.Scanner;

public class InputHandler {
    private final Scanner scanner;
    public InputHandler(){
        this.scanner = new Scanner(System.in);
    }

    public int readIntRange(String prompt, int min, int max){
        while(true){
            System.out.println(prompt);
            String input = scanner.nextLine().trim();

            try{
                int choice = Integer.parseInt(input);
                if(choice >= min && choice <= max){
                    return choice;
                }

                System.out.println("Choice out of range! Please enter a number between " + min + " and " + max + ".");
            } catch(NumberFormatException e){
                System.out.println("Invalid input! Please enter a valid integer.");
            }
        }
    }

    public int readPositiveInt(String prompt){
        while(true){
            System.out.println(prompt);
            String input = scanner.nextLine().trim();

            try{
                int value = Integer.parseInt(input);
                if(value >= 0){
                    return value;
                }

                System.out.println("Value must be positive! Please enter a number larger or equal to 0.");
            } catch(NumberFormatException e){
                System.out.println("Invalid input! Please enter a valid positive integer.");
            }
        }
    }

    public String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Input cannot be empty! Please try again.");
        }
    }

    public String readFilePath(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            // Strip leading and trailing double quotes if present
            if (input.startsWith("\"") && input.endsWith("\"")) {
                input = input.substring(1, input.length() - 1).trim();
            } else {
                // Remove any leftover standalone quotes just in case
                input = input.replace("\"", "").trim();
            }

            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("File path cannot be empty. Please try again.");
        }
    }
}
