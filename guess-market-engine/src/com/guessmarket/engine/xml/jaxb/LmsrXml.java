package com.guessmarket.engine.xml.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class LmsrXml {

    @XmlElement(name = "b")
    private Integer b;

    public LmsrXml() {}

    public Integer getB() {
        return b;
    }
}