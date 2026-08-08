package com.guessmarket.engine.model;

public class Option {
    private final String name;
    private int sharesBought;

    public Option(String name) {
        this.name = name;
        this.sharesBought = 0;
    }

    public String getName() { return name; }
    public int getSharesBought() { return sharesBought; }

    public void addShares(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Share count must be positive");
        }
        this.sharesBought += count;
    }
}