package com.guessmarket.dto;

/**
 * Which market-making method an event uses. {@link #LMSR} and {@link #ORDERBOOK}
 * are the real methods; {@link #NONE} and {@link #NOTDEFINED} are only produced
 * while parsing XML (no method, or an ambiguous one) and never reach a DTO.
 */
public enum TradingMethodType {
    LMSR,
    ORDERBOOK,
    NONE,
    NOTDEFINED
}
