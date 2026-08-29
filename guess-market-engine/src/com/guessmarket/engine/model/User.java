package com.guessmarket.engine.model;

import javax.management.ImmutableDescriptor;
import java.util.*;

public class User {
    private String name;
    private Double initialCash;
    private Set<Integer> eventsIdUserIsMM;
    private Map<Integer, Event> participatingEvents;

    public User(String name, Double initialCash, Set<Integer> eventsId){
        this.name = name;
        this.initialCash = initialCash;
        this.eventsIdUserIsMM = eventsId;
        this.participatingEvents = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Double getInitialCash() {
        return initialCash;
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
        return initialCash == user.initialCash && Objects.equals(name, user.name) && Objects.equals(eventsIdUserIsMM, user.eventsIdUserIsMM);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, initialCash, eventsIdUserIsMM);
    }
}
