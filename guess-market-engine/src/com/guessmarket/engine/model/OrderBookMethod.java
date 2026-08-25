package com.guessmarket.engine.model;

public final class OrderBookMethod implements  ITradingMethod{
    @Override
    public TradingMethodType getType() {
        return TradingMethodType.ORDERBOOK;
    }

    // FIXME: Need to implement
    public double getInitialSubsidy() {
        return 0;
    }
}
