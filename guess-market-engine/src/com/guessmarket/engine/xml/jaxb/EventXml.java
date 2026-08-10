package com.guessmarket.engine.xml.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class EventXml {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "id")
    private Integer id;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "comision")
    private CommissionXml commission;

    @XmlElementWrapper(name = "GM-options")
    @XmlElement(name = "GM-option")
    private List<String> options = new ArrayList<>();

    @XmlElement(name = "GM-method")
    private MethodXml method;

    public EventXml() {}

    public String getName() { return name; }
    public Integer getId() { return id; }
    public String getDescription() { return description; }
    public CommissionXml getCommission() { return commission; }
    public List<String> getOptions() { return options; }
    public MethodXml getMethod() { return method; }
}