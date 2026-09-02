package com.guessmarket.engine.model;

import java.util.*;

public class User {
    private String name;
    private Double accountBalance;
    private Set<Integer> eventsIdUserIsMM;
    private Map<Integer, Event> participatingEvents;

    public User(String name, Double accountBalance, Set<Integer> eventsId){
        this.name = name;
        this.accountBalance = accountBalance;
        this.eventsIdUserIsMM = eventsId;
        this.participatingEvents = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Double getAccountBalance() {
        return accountBalance;
    }

    public Set<Integer> getEventsIdUserIsMM() {
        return eventsIdUserIsMM;
    }

    public Map<Integer, Event> getParticipatingEvents() {
        return participatingEvents;
    }

    public void addParticipatingEvent(Event participatingEvent){
        participatingEvents.put(participatingEvent.getId(), participatingEvent);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return accountBalance == user.accountBalance && Objects.equals(name, user.name) && Objects.equals(eventsIdUserIsMM, user.eventsIdUserIsMM);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, accountBalance, eventsIdUserIsMM);
    }

    public void setBalance(double v) {
        this.accountBalance = v;
    }
}
