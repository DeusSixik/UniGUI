package dev.sixik.unigui.widgets.data;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Column configuration for VirtualTableView defining header text, width, alignments, and overflow mode.",
        purpose = "Configures column geometry, header rendering, and content alignment within a VirtualTableView.",
        useWhen = {
                "Adding columns to VirtualTableView.",
                "Customizing column text alignment or cell clipping behavior."
        },
        criticalRules = {
                "width is clamped to a minimum of 1.0 pixel.",
                "Alignment and overflowMode default to START, CENTER, and CLIP respectively."
        },
        related = {"VirtualTableView", "Alignment", "TextOverflowMode"})
@WikiPage(
        title = "<key:virtual.table.column.title>",
        slug = "virtual-table-column",
        category = "<key:core.widgets.data.category>",
        summary = "<key:virtual.table.column.summary>",
        order = 20)
@WikiSection(
        title = "<key:virtual.table.column.section.overview.title>",
        markdown = "<key:virtual.table.column.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:virtual.table.column.example.basic.title>",
        code = "<key:virtual.table.column.example.basic.code>",
        order = 10)
public record VirtualTableColumn(String header,
                                 RichText richHeader,
                                 float width,
                                 Alignment horizontalAlignment,
                                 Alignment verticalAlignment,
                                 TextOverflowMode overflowMode) {
    public VirtualTableColumn(String header, float width) {
        this(header, RichText.resolve(header), width, Alignment.START, Alignment.CENTER, TextOverflowMode.CLIP);
    }

    public VirtualTableColumn(RichText header, float width) {
        this(header == null ? "" : header.plainText(), header, width, Alignment.START, Alignment.CENTER, TextOverflowMode.CLIP);
    }

    public VirtualTableColumn(String header,
                              float width,
                              Alignment horizontalAlignment,
                              Alignment verticalAlignment,
                              TextOverflowMode overflowMode) {
        this(header, RichText.resolve(header), width, horizontalAlignment, verticalAlignment, overflowMode);
    }

    public VirtualTableColumn {
        RichText normalizedHeader = richHeader == null ? RichText.resolve(header) : richHeader;
        header = normalizedHeader.plainText();
        richHeader = normalizedHeader;
        width = Float.isFinite(width) ? Math.max(1.0f, width) : 80.0f;
        horizontalAlignment = horizontalAlignment == null ? Alignment.START : horizontalAlignment;
        verticalAlignment = verticalAlignment == null ? Alignment.CENTER : verticalAlignment;
        overflowMode = overflowMode == null ? TextOverflowMode.CLIP : overflowMode;
    }

    @WikiMethod(
            title = "<key:virtual.table.column.method.width.title>",
            summary = "<key:virtual.table.column.method.width.summary>")
    public VirtualTableColumn width(float width) {
        return new VirtualTableColumn(header, richHeader, width, horizontalAlignment, verticalAlignment, overflowMode);
    }

    @WikiMethod(
            title = "<key:virtual.table.column.method.align.title>",
            summary = "<key:virtual.table.column.method.align.summary>")
    public VirtualTableColumn align(Alignment horizontal, Alignment vertical) {
        return new VirtualTableColumn(header, richHeader, width, horizontal, vertical, overflowMode);
    }

    @WikiMethod(
            title = "<key:virtual.table.column.method.overflow_mode.title>",
            summary = "<key:virtual.table.column.method.overflow_mode.summary>")
    public VirtualTableColumn overflowMode(TextOverflowMode overflowMode) {
        return new VirtualTableColumn(header, richHeader, width, horizontalAlignment, verticalAlignment, overflowMode);
    }

    @WikiMethod(
            title = "<key:virtual.table.column.method.rich_header.title>",
            summary = "<key:virtual.table.column.method.rich_header.summary>")
    public VirtualTableColumn richHeader(RichText header) {
        RichText normalized = header == null ? RichText.plain("") : header;
        return new VirtualTableColumn(normalized.plainText(), normalized, width, horizontalAlignment, verticalAlignment, overflowMode);
    }
}