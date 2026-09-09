package com.guessmarket.ui.common;

/**
 * In-memory on/off switches for the app's UI animations, toggled from the
 * <em>System &rarr; Animations</em> dialog. Both animations are enabled by
 * default. Settings live for the current run only - they are not persisted.
 */
public final class AnimationSettings {

    private static boolean userDetailsRevealEnabled = true;
    private static boolean eventPanelRevealEnabled = true;

    private AnimationSettings() {}

    /** Users tab: the User Details panel fades and slides in when a different user is selected. */
    public static boolean isUserDetailsRevealEnabled() {
        return userDetailsRevealEnabled;
    }

    /** Events tab: the Trade panel fades and slides in when a new event is selected. */
    public static boolean isEventPanelRevealEnabled() {
        return eventPanelRevealEnabled;
    }

    public static void setUserDetailsRevealEnabled(boolean enabled) {
        userDetailsRevealEnabled = enabled;
    }

    public static void setEventPanelRevealEnabled(boolean enabled) {
        eventPanelRevealEnabled = enabled;
    }
}
