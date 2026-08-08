package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.Orientation;

public record SeparatorState(
        float x,
        float y,
        float width,
        float height,
        Orientation orientation,
        float thickness,
        ColorView color
) {
}
