package com.guessmarket.engine.xml.jaxb;

import com.guessmarket.engine.model.TradingMethodType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class MethodXml {

    @XmlElement(name = "GM-LMSR")
    private LmsrXml lmsr;

    @XmlElement(name = "GM-order-book")
    private OrderBookXml orderBook;

    public MethodXml() {}

    public LmsrXml getLmsr() {
        return lmsr;
    }
    public OrderBookXml getOrderBook() {
        return orderBook;
    }

    public TradingMethodType getType(){
        TradingMethodType tradingMethodType = TradingMethodType.NONE;
        if(lmsr != null && orderBook == null){
            tradingMethodType = TradingMethodType.LMSR;
        }

        if(lmsr == null && orderBook != null){
            tradingMethodType = TradingMethodType.ORDERBOOK;
        }

        return tradingMethodType;
    }
}