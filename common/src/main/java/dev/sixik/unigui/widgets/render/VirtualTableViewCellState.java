package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;

import java.util.List;

public record VirtualTableViewCellState(
        int visualRow,
        int sourceRow,
        int column,
        float x,
        float y,
        float width,
        float height,
        RichText text,
        Alignment horizontalAlignment,
        Alignment verticalAlignment,
        TextOverflowMode overflowMode,
        boolean active,
        boolean editing,
        boolean clipped,
        float clipX,
        float clipY,
        float clipWidth,
        float clipHeight,
        List<VirtualTableViewTextSegment> textSegments
) {
    public VirtualTableViewCellState {
        textSegments = textSegments == null ? List.of() : List.copyOf(textSegments);
    }
}
