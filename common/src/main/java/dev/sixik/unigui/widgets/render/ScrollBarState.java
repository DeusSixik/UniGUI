package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.core.Orientation;

public record ScrollBarState(
        float x,
        float y,
        float width,
        float height,
        Orientation orientation,
        float min,
        float max,
        float value,
        float pageSize,
        float step,
        float normalizedValue,
        boolean dragging,
        ColorView trackColor,
        ColorView thumbColor
) {
    public float trackLength() {
        return orientation == Orientation.VERTICAL ? height : width;
    }

    public float thumbLength(float trackLength) {
        float contentExtent = Math.max(pageSize, pageSize + Math.max(0.0f, max - min));
        return Math.max(8.0f, trackLength * (pageSize / contentExtent));
    }
}
