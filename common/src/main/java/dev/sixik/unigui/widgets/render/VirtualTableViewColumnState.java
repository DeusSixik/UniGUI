package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;

import java.util.List;

public record VirtualTableViewColumnState(
        int column,
        float x,
        float y,
        float width,
        float height,
        RichText header,
        Alignment horizontalAlignment,
        Alignment verticalAlignment,
        TextOverflowMode overflowMode,
        boolean resizing,
        boolean clipped,
        float clipX,
        float clipY,
        float clipWidth,
        float clipHeight,
        List<VirtualTableViewTextSegment> textSegments
) {
    public VirtualTableViewColumnState {
        textSegments = textSegments == null ? List.of() : List.copyOf(textSegments);
    }
}
