package dev.sixik.unigui.widgets.editor;

/** Immutable payload passed between editor drag sources and drop targets. */
public record DragPayload(String id, String type, String preview) {
    public DragPayload {
        id = normalize(id, "");
        type = normalize(type, "generic");
        preview = normalize(preview, id);
    }

    public static DragPayload of(String id, String type, String preview) {
        return new DragPayload(id, type, preview);
    }

    public boolean matchesType(String expectedType) {
        String normalized = normalize(expectedType, "");
        return normalized.equals("*") || type.equals(normalized);
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
