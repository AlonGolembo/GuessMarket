package com.guessmarket.ui.common;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.TradingMethodType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventFiltersTest {

    private static EventDTO event(TradingMethodType method, EventStatus status, CommissionType commission) {
        return new EventDTO(1, "E", "d", 10, commission, List.of("A", "B"), method, status);
    }

    private static final EventDTO LMSR_ACTIVE_ONPURCHASE =
            event(TradingMethodType.LMSR, EventStatus.ACTIVE, CommissionType.ON_PURCHASE);
    private static final EventDTO ORDERBOOK_CLOSED_ONCLOSE =
            event(TradingMethodType.ORDERBOOK, EventStatus.CLOSED, CommissionType.ON_CLOSE);

    @Test
    void noFilterSelectionMatchesEverything() {
        var all = EventFilters.predicate(EventFilters.NO_FILTER, EventFilters.NO_FILTER, EventFilters.NO_FILTER);
        assertTrue(all.test(LMSR_ACTIVE_ONPURCHASE));
        assertTrue(all.test(ORDERBOOK_CLOSED_ONCLOSE));
    }

    @Test
    void nullSelectionAlsoMatchesEverything() {
        var all = EventFilters.predicate(null, null, null);
        assertTrue(all.test(LMSR_ACTIVE_ONPURCHASE));
        assertTrue(all.test(ORDERBOOK_CLOSED_ONCLOSE));
    }

    @Test
    void eachFilterNarrowsIndependently() {
        assertTrue(EventFilters.predicate("LMSR", null, null).test(LMSR_ACTIVE_ONPURCHASE));
        assertFalse(EventFilters.predicate("LMSR", null, null).test(ORDERBOOK_CLOSED_ONCLOSE));

        assertTrue(EventFilters.predicate(null, "CLOSED", null).test(ORDERBOOK_CLOSED_ONCLOSE));
        assertFalse(EventFilters.predicate(null, "CLOSED", null).test(LMSR_ACTIVE_ONPURCHASE));

        assertTrue(EventFilters.predicate(null, null, "ON_PURCHASE").test(LMSR_ACTIVE_ONPURCHASE));
        assertFalse(EventFilters.predicate(null, null, "ON_PURCHASE").test(ORDERBOOK_CLOSED_ONCLOSE));
    }

    @Test
    void filtersCombineWithAnd() {
        assertTrue(EventFilters.predicate("LMSR", "ACTIVE", "ON_PURCHASE").test(LMSR_ACTIVE_ONPURCHASE));
        assertFalse(EventFilters.predicate("LMSR", "CLOSED", "ON_PURCHASE").test(LMSR_ACTIVE_ONPURCHASE));
    }

    @Test
    void switchingAFilterBackToAllClearsIt() {
        assertFalse(EventFilters.predicate("ORDERBOOK", null, null).test(LMSR_ACTIVE_ONPURCHASE));
        // user re-selects "All" for the method filter
        assertTrue(EventFilters.predicate(EventFilters.NO_FILTER, null, null).test(LMSR_ACTIVE_ONPURCHASE));
    }
}
