package com.guessmarket.engine.model;

import java.util.Collection;

public class User {
    private String name;
    private int initialCash;
    private Collection<String> eventsIdUserIsMM;

    public User(String name, int initialCash, Collection<String> eventsId){
        this.name = name;
        this.initialCash = initialCash;
        this.eventsIdUserIsMM = eventsId;
    }
}
