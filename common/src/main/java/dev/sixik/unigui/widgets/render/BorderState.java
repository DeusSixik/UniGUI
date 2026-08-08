package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

public record BorderState(
        float x,
        float y,
        float width,
        float height,
        ColorView color,
        float thickness,
        float radius
) {
}
