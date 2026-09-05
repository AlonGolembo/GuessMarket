package com.guessmarket.engine.model;

import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.engine.exception.XmlValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderBookMethodTest {

    @Test
    void initialSubsidyIsInitialPairsTimesBaseValue() {
        OrderBookMethod method = new OrderBookMethod(5, 100, true);
        assertEquals(TradingMethodType.ORDERBOOK, method.type());
        assertEquals(500.0, method.initialSubsidy());   // 100 pairs * d=5
        assertEquals(5.0, method.baseValue());
        assertEquals(100, method.initialShares());
    }

    @Test
    void validateRejectsNonPositiveBaseValue() {
        assertThrows(XmlValidationException.class, () -> new OrderBookMethod(0, 100, true).validate());
        assertThrows(XmlValidationException.class, () -> new OrderBookMethod(-1, 100, true).validate());
    }

    @Test
    void validateRejectsNegativeInitialAllocation() {
        assertThrows(XmlValidationException.class, () -> new OrderBookMethod(1, -1, true).validate());
    }

    @Test
    void validateRejectsNoInitialAllocationWithMintingDisabled() {
        assertThrows(XmlValidationException.class, () -> new OrderBookMethod(1, 0, false).validate());
    }

    @Test
    void zeroInitialAllocationIsFineWhenMintingIsAllowed() {
        assertDoesNotThrow(() -> new OrderBookMethod(1, 0, true).validate());
    }

    @Test
    void positiveInitialAllocationIsFineWithoutMinting() {
        assertDoesNotThrow(() -> new OrderBookMethod(1, 50, false).validate());
    }
}
