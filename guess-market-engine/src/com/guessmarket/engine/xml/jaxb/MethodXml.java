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

    /**
     * @return {@code LMSR}/{@code ORDERBOOK} when exactly one method is present,
     *         {@code NOTDEFINED} when both are present (ambiguous),
     *         {@code NONE} when neither is.
     */
    public TradingMethodType getType() {
        boolean hasLmsr = lmsr != null;
        boolean hasOrderBook = orderBook != null;
        if (hasLmsr && hasOrderBook) return TradingMethodType.NOTDEFINED;
        if (hasLmsr) return TradingMethodType.LMSR;
        if (hasOrderBook) return TradingMethodType.ORDERBOOK;
        return TradingMethodType.NONE;
    }
}