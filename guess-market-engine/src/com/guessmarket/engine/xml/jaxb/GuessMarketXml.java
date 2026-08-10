package com.guessmarket.engine.xml.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuessMarketXml {

    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<EventXml> events = new ArrayList<>();

    public GuessMarketXml() {}

    public List<EventXml> getEvents() {
        return events;
    }

    public void setEvents(List<EventXml> events) {
        this.events = events;
    }
}