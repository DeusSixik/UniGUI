package dev.sixik.unigui.api.layout;

/** Selects in-flow or out-of-flow positioning. */
public enum PositionType {
    /** Participates in the parent's normal layout flow. */
    RELATIVE,
    /** Is positioned from the parent content box using inset/size values. */
    ABSOLUTE
}
