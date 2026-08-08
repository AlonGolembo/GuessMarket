package com.guessmarket.engine.exception;

public class XmlValidationException extends MarketException {

    public XmlValidationException(String message) {
        super(message);
    }

    public XmlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}