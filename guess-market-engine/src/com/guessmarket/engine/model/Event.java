package com.guessmarket.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Event implements java.io.Serializable {

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercentage; // Integer between 0 and 90
    private final CommissionType commissionType;
    private final List<Option> options;
    private final int b;                     // LMSR liquidity parameter

    private boolean active;
    private double eventAccountBalance;      // Subsidies / trades / mint funds
    private double totalCommissionCollected;
    private final List<TradeRecord> tradeHistory; // Audit log of transactions
    private Option winningOption;             // Set when closed

    public Event(int id, String name, String description, int commissionPercentage,
                 CommissionType commissionType, List<Option> options, int b) {

        validateCommission(commissionPercentage);
        validateOptions(options);

        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercentage = commissionPercentage;
        this.commissionType = commissionType;
        this.options = new ArrayList<>(options);
        this.b = b;
        this.active = true;
        this.eventAccountBalance = 0.0;
        this.totalCommissionCollected = 0.0;
        this.tradeHistory = new ArrayList<>();
        this.winningOption = null;
    }

    private void validateOptions(List<Option> options) {
        if(options == null || options.size() != 2){
            throw new IllegalArgumentException("The event should have exactly two options! Provided: " + (options == null ? 0 : options.size()));
        }
    }

    private static void validateCommission(int commission) {
        if (commission < 0 || commission > 90) {
            throw new IllegalArgumentException("Commission percentage must be between 0 and 90 inclusive. Got: " + commission);
        }
    }

    // --- Domain Operations ---

    public void addTradeRecord(TradeRecord record) {
        // Keeps latest trades accessible
        this.tradeHistory.add(0, record);
    }

    public void addCommission(double amount) {
        this.totalCommissionCollected += amount;
        this.eventAccountBalance += amount;
    }

    public void closeEvent(Option winningOption) {
        if (!this.active) {
            throw new IllegalStateException("Event is already closed.");
        }
        if (!this.options.contains(winningOption)) {
            throw new IllegalArgumentException("Selected winning option does not belong to this event.");
        }
        this.winningOption = winningOption;
        this.active = false;
    }

    // --- Getters ---

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public int getCommissionPercentage() {
        return commissionPercentage;
    }
    public CommissionType getCommissionType() {
        return commissionType;
    }
    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }
    public int getB() {
        return b;
    }
    public boolean isActive() {
        return active;
    }
    public double getEventAccountBalance() {
        return eventAccountBalance;
    }
    public void setEventAccountBalance(double balance) {
        this.eventAccountBalance = balance;
    }
    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }
    public List<TradeRecord> getTradeHistory() {
        return Collections.unmodifiableList(tradeHistory);
    }
    public Option getWinningOption() {
        return winningOption;
    }
}