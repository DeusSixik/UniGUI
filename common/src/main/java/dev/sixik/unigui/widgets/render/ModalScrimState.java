package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

public record ModalScrimState(
        float x,
        float y,
        float width,
        float height,
        boolean visible,
        int stackDepth,
        ColorView color
) {
}

