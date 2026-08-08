package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;

public record VirtualTableColumn(String header,
                                 RichText richHeader,
                                 float width,
                                 Alignment horizontalAlignment,
                                 Alignment verticalAlignment,
                                 TextOverflowMode overflowMode) {
    public VirtualTableColumn(String header, float width) {
        this(header, RichText.plain(header), width, Alignment.START, Alignment.CENTER, TextOverflowMode.CLIP);
    }

    public VirtualTableColumn(RichText header, float width) {
        this(header == null ? "" : header.plainText(), header, width, Alignment.START, Alignment.CENTER, TextOverflowMode.CLIP);
    }

    public VirtualTableColumn(String header,
                              float width,
                              Alignment horizontalAlignment,
                              Alignment verticalAlignment,
                              TextOverflowMode overflowMode) {
        this(header, RichText.plain(header), width, horizontalAlignment, verticalAlignment, overflowMode);
    }

    public VirtualTableColumn {
        RichText normalizedHeader = richHeader == null ? RichText.plain(header) : richHeader;
        header = normalizedHeader.plainText();
        richHeader = normalizedHeader;
        width = Float.isFinite(width) ? Math.max(1.0f, width) : 80.0f;
        horizontalAlignment = horizontalAlignment == null ? Alignment.START : horizontalAlignment;
        verticalAlignment = verticalAlignment == null ? Alignment.CENTER : verticalAlignment;
        overflowMode = overflowMode == null ? TextOverflowMode.CLIP : overflowMode;
    }

    public VirtualTableColumn width(float width) {
        return new VirtualTableColumn(header, richHeader, width, horizontalAlignment, verticalAlignment, overflowMode);
    }

    public VirtualTableColumn align(Alignment horizontal, Alignment vertical) {
        return new VirtualTableColumn(header, richHeader, width, horizontal, vertical, overflowMode);
    }

    public VirtualTableColumn overflowMode(TextOverflowMode overflowMode) {
        return new VirtualTableColumn(header, richHeader, width, horizontalAlignment, verticalAlignment, overflowMode);
    }

    public VirtualTableColumn richHeader(RichText header) {
        RichText normalized = header == null ? RichText.plain("") : header;
        return new VirtualTableColumn(normalized.plainText(), normalized, width, horizontalAlignment, verticalAlignment, overflowMode);
    }
}
