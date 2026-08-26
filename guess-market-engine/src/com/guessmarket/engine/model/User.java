package com.guessmarket.engine.model;

import java.util.*;

public class User {
    private String name;
    private int initialCash;
    private Set<Integer> eventsIdUserIsMM;

    public User(String name, int initialCash, Set<Integer> eventsId){
        this.name = name;
        this.initialCash = initialCash;
        this.eventsIdUserIsMM = eventsId;
    }

    public String getName() {
        return name;
    }

    public int getInitialCash() {
        return initialCash;
    }

    public Set<Integer> getEventsIdUserIsMM() {
        return eventsIdUserIsMM;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return initialCash == user.initialCash && Objects.equals(name, user.name) && Objects.equals(eventsIdUserIsMM, user.eventsIdUserIsMM);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, initialCash, eventsIdUserIsMM);
    }
}
