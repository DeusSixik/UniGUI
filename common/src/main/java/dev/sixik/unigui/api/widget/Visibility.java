package dev.sixik.unigui.api.widget;

/**
 * Controls how a widget participates in layout, rendering and input.
 */
public enum Visibility {
    /**
     * Widget is measured, arranged, rendered and can receive input/focus.
     */
    VISIBLE,

    /**
     * Widget keeps its layout slot but does not render or receive input/focus.
     */
    HIDDEN,

    /**
     * Widget is removed from layout and does not render or receive input/focus.
     */
    COLLAPSED
}
