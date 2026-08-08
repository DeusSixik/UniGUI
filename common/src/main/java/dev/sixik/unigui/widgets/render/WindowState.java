package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

public record WindowState(
        float x,
        float y,
        float width,
        float height,
        float headerHeight,
        float paddingLeft,
        float paddingRight,
        boolean closeButtonVisible,
        float closeButtonWidth,
        RichText title,
        float titleTextWidth,
        float titleTextHeight,
        ColorView headerColor,
        ColorView headerSeparatorColor,
        ColorView titleColor
) {
}
