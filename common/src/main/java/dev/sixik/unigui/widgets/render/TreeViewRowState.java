package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

public record TreeViewRowState(
        float x,
        float y,
        float width,
        float height,
        int depth,
        float indentWidth,
        float textPaddingX,
        RichText text,
        float textX,
        float textY,
        float textWidth,
        float textHeight,
        ColorView textColor,
        boolean selected,
        boolean selectable,
        boolean hasChildren,
        boolean expanded,
        boolean hovered,
        boolean pressed,
        boolean enabled
) {
}
