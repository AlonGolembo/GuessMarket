package com.guessmarket.engine.xml.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class CommissionXml {

    @XmlAttribute(name = "type")
    private String type;

    @XmlValue
    private Integer value;

    public CommissionXml() {}

    public String getType() {
        return type;
    }
    public Integer getValue() {
        return value;
    }
}