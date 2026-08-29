package com.guessmarket.engine.model;

import com.guessmarket.engine.xml.jaxb.OrderBookXml;

public sealed interface ITradingMethod permits LmsrMethod, OrderBookMethod {
    TradingMethodType getType();
}
