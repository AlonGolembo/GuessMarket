package com.guessmarket.engine.exception;

//Base custom exception for all GuessMarket engine domain and operational failures.

public class MarketException extends Exception {

    public MarketException(String message) {
        super(message);
    }

    public MarketException(String message, Throwable cause) {
        super(message, cause);
    }
}