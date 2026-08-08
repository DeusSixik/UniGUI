package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.text.RichText;

public record VirtualTableViewTextSegment(
        RichText text,
        float x,
        float y,
        float width,
        float height,
        Transform transform
) {
}
