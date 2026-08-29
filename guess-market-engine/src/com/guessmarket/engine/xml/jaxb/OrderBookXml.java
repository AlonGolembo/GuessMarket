package com.guessmarket.engine.xml.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrderBookXml {
    @XmlAttribute(name="allow-mint")
    private boolean allowMint;

    @XmlAttribute(name="initial")
    private Integer initial;

    @XmlAttribute(name = "d")
    private Integer d;

    public OrderBookXml() {}

    public Integer getD() {
        return d;
    }
    public Integer getInitial() {return initial;}
}