package com.guessmarket.engine.api;

import java.util.ArrayList;
import java.util.List;

/**
 * The listener registry, lifted out of {@link MarketEngineImpl}. Notifies
 * listeners over a snapshot of the list so a listener may add or remove itself
 * from within its own callback.
 */
final class MarketEventPublisher {

    private final List<MarketDataChangeListener> listeners = new ArrayList<>();

    void add(MarketDataChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    void remove(MarketDataChangeListener listener) {
        listeners.remove(listener);
    }

    void publish() {
        for (MarketDataChangeListener listener : List.copyOf(listeners)) {
            listener.onMarketDataChanged();
        }
    }
}
