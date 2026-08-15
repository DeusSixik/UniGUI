package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.display.Shape;

public record ShapeState(
        float x,
        float y,
        float width,
        float height,
        Shape.Type type,
        ColorView color,
        boolean stroke,
        float strokeWidth,
        float radius
) {
}
