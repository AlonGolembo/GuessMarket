package com.guessmarket.engine.model;

import java.io.Serializable;

/**
 * One outcome of an {@link Event} (e.g. "Yes" / "No"), together with the running
 * count of shares the market has sold for it.
 *
 * <p>Identity is the option {@code name}: two options are the same option when
 * they are named the same, regardless of how many shares have been bought. The
 * share count only ever increases, and only {@link Event} may change it.
 */
public class Option implements Serializable {

    private final String name;
    private int sharesOutstanding;

    public Option(String name) {
        this.name = name;
        this.sharesOutstanding = 0;
    }

    public String getName() {
        return name;
    }

    /** Total shares the market has sold for this outcome. */
    public int getSharesOutstanding() {
        return sharesOutstanding;
    }

    /** Package-private: share issuance is driven by {@link Event#buy}. */
    void addShares(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Share count must be positive, got: " + count);
        }
        this.sharesOutstanding += count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return name.equals(((Option) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Option[" + name + ", shares=" + sharesOutstanding + "]";
    }
}
