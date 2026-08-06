package dev.sixik.unigui.api.layout;

/** Controls child clipping and scrolling behavior for each axis. */
public enum Overflow {
    /** Content may render outside the widget bounds. */
    VISIBLE,
    /** Content is clipped without scrolling. */
    HIDDEN,
    /** A ScrollView always exposes scrolling on this axis. */
    SCROLL,
    /** A ScrollView exposes scrolling only when content exceeds its viewport. */
    AUTO
}
