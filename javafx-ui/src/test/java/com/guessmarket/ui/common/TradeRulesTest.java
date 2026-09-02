package com.guessmarket.ui.common;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.dto.UserDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeRulesTest {

    private static EventDTO event(EventStatus status) {
        return new EventDTO(1, "E", "d", 10, CommissionType.ON_PURCHASE,
                List.of("A", "B"), TradingMethodType.LMSR, status);
    }

    private static UserDTO user(Set<Integer> mmEvents) {
        return new UserDTO("u", 100.0, mmEvents, Map.of());
    }

    @Test
    void marketMakerCanActivateOnlyWhilePending() {
        assertTrue(TradeRules.canActivate(user(Set.of(1)), event(EventStatus.NOT_ACTIVE)));
        assertFalse(TradeRules.canActivate(user(Set.of(1)), event(EventStatus.ACTIVE)));
        assertFalse(TradeRules.canActivate(user(Set.of()), event(EventStatus.NOT_ACTIVE)));
    }

    @Test
    void nonMarketMakerCanTradeAnOpenEvent() {
        assertTrue(TradeRules.canTrade(user(Set.of()), event(EventStatus.ACTIVE)));
        assertFalse(TradeRules.canTrade(user(Set.of(1)), event(EventStatus.ACTIVE)));   // MM
        assertFalse(TradeRules.canTrade(user(Set.of()), event(EventStatus.NOT_ACTIVE)));
        assertFalse(TradeRules.canTrade(user(Set.of()), event(EventStatus.CLOSED)));
    }

    @Test
    void nullSelectionsAreSafe() {
        assertFalse(TradeRules.canTrade(null, event(EventStatus.ACTIVE)));
        assertFalse(TradeRules.canActivate(user(Set.of(1)), null));
        assertFalse(TradeRules.isMarketMaker(null, null));
    }

    @Test
    void payTimingLabel() {
        assertEquals("(Pay now)", TradeRules.payTimingLabel(CommissionType.ON_PURCHASE));
        assertEquals("(Pay later)", TradeRules.payTimingLabel(CommissionType.ON_CLOSE));
        assertEquals("", TradeRules.payTimingLabel(null));
    }
}
