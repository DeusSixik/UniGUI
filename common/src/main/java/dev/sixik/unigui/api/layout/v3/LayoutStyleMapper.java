package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;

/** Bridges the current mutable LayoutStyle / legacy LayoutConstraints APIs into V3 snapshots. */
public final class LayoutStyleMapper {
    private LayoutStyleMapper() {
    }

    public static LayoutStyleSnapshot from(LayoutStyle style) {
        return LayoutStyleSnapshot.from(style);
    }

    public static LayoutStyleSnapshot from(LayoutConstraints constraints) {
        return LayoutStyleSnapshot.from(constraints);
    }

    public static LayoutStyleSnapshot from(LayoutStyle style, LayoutConstraints fallbackConstraints) {
        if (style != null) {
            return LayoutStyleSnapshot.from(style);
        }
        return LayoutStyleSnapshot.from(fallbackConstraints);
    }
}
