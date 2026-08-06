package dev.sixik.unigui.widgets;

public record VirtualTableColumn(String header, float width) {
    public VirtualTableColumn {
        header = header == null ? "" : header;
        width = Float.isFinite(width) ? Math.max(1.0f, width) : 80.0f;
    }
}
