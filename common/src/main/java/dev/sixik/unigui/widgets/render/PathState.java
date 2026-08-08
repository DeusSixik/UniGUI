package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.VectorPath;

public record PathState(
        float x,
        float y,
        float width,
        float height,
        VectorPath path,
        ColorView color,
        boolean stroke,
        float strokeWidth
) {
}
