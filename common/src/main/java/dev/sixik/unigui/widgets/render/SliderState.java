package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

public record SliderState(
        float x,
        float y,
        float width,
        float height,
        float min,
        float max,
        float value,
        float step,
        float normalizedValue,
        float knobWidth,
        boolean dragging,
        ColorView trackColor,
        ColorView fillColor,
        ColorView knobColor
) {
}
