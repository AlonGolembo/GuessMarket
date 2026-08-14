package com.guessmarket.engine.model;

import java.util.Objects;

public class Option implements java.io.Serializable {
    private final String name;
    private int sharesBought;

    public Option(String name) {
        this.name = name;
        this.sharesBought = 0;
    }

    public String getName() {
        return name;
    }
    public int getSharesBought() {
        return sharesBought;
    }

    public void addShares(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Share count must be positive");
        }
        this.sharesBought += count;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Option option = (Option) o;
        return sharesBought == option.sharesBought && Objects.equals(name, option.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sharesBought);
    }
}