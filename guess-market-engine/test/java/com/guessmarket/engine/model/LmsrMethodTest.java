package com.guessmarket.engine.model;

import com.guessmarket.dto.TradingMethodType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LmsrMethodTest {

    @Test
    void payoutIsOneDollarPerShareAndNoInitialShares() {
        LmsrMethod method = new LmsrMethod(100);
        assertEquals(TradingMethodType.LMSR, method.type());
        assertEquals(1.0, method.baseValue());
        assertEquals(0, method.initialShares());
    }

    @Test
    void invalidBIsReportedByValidateNotTheConstructor() {
        LmsrMethod method = new LmsrMethod(0);   // must not throw
        assertThrows(com.guessmarket.engine.exception.XmlValidationException.class, method::validate);
    }
}
