package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

public record DockingRootState(
        float x,
        float y,
        float width,
        float height,
        boolean empty,
        boolean backgroundVisible,
        ColorView backgroundColor,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth,
        boolean dockDragging,
        boolean dropPreviewVisible
) {
}
