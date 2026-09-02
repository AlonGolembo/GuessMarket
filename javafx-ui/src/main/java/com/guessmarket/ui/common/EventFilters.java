package com.guessmarket.ui.common;

import com.guessmarket.dto.EventDTO;

import java.util.function.Predicate;

/**
 * The Events-tab filter logic, kept free of JavaFX so it can be unit-tested.
 * Each filter is a selected string from a combo; {@link #NO_FILTER} (or
 * {@code null}, before the combo is populated) means "match everything".
 */
public final class EventFilters {

    /** The "match everything" choice shown at the top of each filter combo. */
    public static final String NO_FILTER = "All";

    private EventFilters() {}

    /**
     * A predicate combining the three filters. Each argument is a combo's current
     * selection ({@code null} / {@link #NO_FILTER} disables that filter);
     * otherwise it is compared to the matching {@link EventDTO} enum name.
     */
    public static Predicate<EventDTO> predicate(String method, String status, String commission) {
        return event -> matches(method, event.tradingMethod().name())
                && matches(status, event.status().name())
                && matches(commission, event.commissionType().name());
    }

    private static boolean matches(String selected, String value) {
        return selected == null || NO_FILTER.equals(selected) || selected.equals(value);
    }
}
