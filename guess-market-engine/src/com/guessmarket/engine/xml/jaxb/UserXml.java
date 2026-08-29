package com.guessmarket.engine.xml.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

import java.util.*;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.FIELD)
public class UserXml {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "initial-cash")
    private Double initialCash;

    @XmlElementWrapper(name = "GM-market-maker")
    @XmlElement(name = "event")
    private List<EventRefXml> marketMakerEvents = new ArrayList<>();

    public UserXml() {
    }

    public String getName() {
        return name;
    }

    public Double getInitialCash() {
        return initialCash;
    }

    public Set<Integer> getMarketMakerEvents() {
        return marketMakerEvents.stream().map(EventRefXml::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
