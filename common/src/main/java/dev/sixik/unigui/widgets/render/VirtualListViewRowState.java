package dev.sixik.unigui.widgets.render;

public record VirtualListViewRowState(
        int index,
        float x,
        float y,
        float width,
        float height,
        boolean selected,
        boolean active
) {
}
