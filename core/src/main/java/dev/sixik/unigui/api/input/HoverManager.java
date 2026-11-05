package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.widget.Widget;

public interface HoverManager {
    HoverManager NONE = new HoverManager() {
    };

    default Widget hoveredWidget() {
        return null;
    }

    default boolean isHovered(Widget widget) {
        return widget != null && hoveredWidget() == widget;
    }

    default void updateHover(Widget widget, float rootX, float rootY, float localX, float localY, int pointerId) {
    }

    default void clearHover() {
    }
}
