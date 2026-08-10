package com.guessmarket.engine.xml.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class MethodXml {

    @XmlElement(name = "GM-LMSR")
    private LmsrXml lmsr;

    public MethodXml() {}

    public LmsrXml getLmsr() {
        return lmsr;
    }
}