package com.guessmarket.engine.model;

import com.guessmarket.engine.lmsr.LmsrCalculator;

public final class LmsrMethod implements ITradingMethod{
    private final Integer b;
    private final Double initialSubsidy;

    public LmsrMethod(Integer b){
        this.b = b;
        this.initialSubsidy = LmsrCalculator.calculateInitialSubsidy(b);
    }

    @Override
    public TradingMethodType getType() {
        return TradingMethodType.LMSR;
    }

    public int getB(){
        return b;
    }

    public Double getInitialSubsidy() {
        return initialSubsidy;
    }
}
