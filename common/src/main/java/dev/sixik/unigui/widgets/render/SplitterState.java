package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.core.Orientation;

public record SplitterState(
        float x,
        float y,
        float width,
        float height,
        Orientation orientation,
        boolean dragging,
        ColorView handleColor
) {
}
