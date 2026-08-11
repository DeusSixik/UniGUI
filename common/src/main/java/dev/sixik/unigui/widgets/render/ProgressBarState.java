package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

public record ProgressBarState(
        float x,
        float y,
        float width,
        float height,
        float min,
        float max,
        float value,
        float progress,
        boolean indeterminate,
        float indeterminateOffset,
        ColorView trackColor,
        ColorView fillColor
) {
}
