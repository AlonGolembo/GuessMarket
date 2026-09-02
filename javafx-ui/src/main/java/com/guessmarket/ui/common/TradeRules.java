package com.guessmarket.ui.common;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.UserDTO;

/**
 * Pure decisions the Users tab makes about a (user, event) pair. Kept free of
 * JavaFX so the rules can be unit-tested; the engine still has the final say and
 * rejects anything these let through by mistake.
 */
public final class TradeRules {

    private TradeRules() {}

    public static boolean isMarketMaker(UserDTO user, EventDTO event) {
        return user != null && event != null
                && user.marketMakerEventIds() != null
                && user.marketMakerEventIds().contains(event.id());
    }

    /** The market maker may open their own event, but only while it is not yet open. */
    public static boolean canActivate(UserDTO user, EventDTO event) {
        return isMarketMaker(user, event) && event.status() == EventStatus.NOT_ACTIVE;
    }

    /** Anyone except the event's market maker may trade an open event. */
    public static boolean canTrade(UserDTO user, EventDTO event) {
        return user != null && event != null
                && event.status() == EventStatus.ACTIVE
                && !isMarketMaker(user, event);
    }

    public static String payTimingLabel(CommissionType commissionType) {
        if (commissionType == null) {
            return "";
        }
        return switch (commissionType) {
            case ON_CLOSE -> "(Pay later)";
            case ON_PURCHASE -> "(Pay now)";
        };
    }
}
