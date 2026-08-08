package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;

import java.util.List;

public record TextWidgetState(
        float x,
        float y,
        float width,
        float height,
        RichText text,
        ColorView color,
        boolean wrap,
        TextOverflowMode overflowMode,
        boolean hovered,
        Alignment verticalAlignment,
        boolean clipped,
        float clipX,
        float clipY,
        float clipWidth,
        float clipHeight,
        List<TextWidgetSegment> segments
) {
    public TextWidgetState {
        segments = segments == null ? List.of() : List.copyOf(segments);
    }
}
