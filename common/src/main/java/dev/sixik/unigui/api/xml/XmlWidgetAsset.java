package dev.sixik.unigui.api.xml;

/** Запись ассета без привязки к backend-у, которую используют XML-выборщики ассетов редактора. */
public record XmlWidgetAsset(
        XmlWidgetAssetKind kind,
        String id,
        String displayName,
        int width,
        int height,
        String description) {
    public XmlWidgetAsset {
        if (kind == null) throw new IllegalArgumentException("XML asset kind must not be null");
        id = normalizeRequired(id, "id");
        displayName = normalize(displayName, id);
        width = Math.max(0, width);
        height = Math.max(0, height);
        description = normalize(description, "");
    }

    public static XmlWidgetAsset texture(String id, int width, int height) {
        return new XmlWidgetAsset(XmlWidgetAssetKind.TEXTURE, id, null, width, height, null);
    }

    public static XmlWidgetAsset font(String id) {
        return new XmlWidgetAsset(XmlWidgetAssetKind.FONT, id, null, 0, 0, null);
    }

    public static XmlWidgetAsset shader(String id) {
        return new XmlWidgetAsset(XmlWidgetAssetKind.SHADER, id, null, 0, 0, null);
    }

    public boolean hasDimensions() {
        return width > 0 && height > 0;
    }

    public XmlWidgetAsset displayName(String displayName) {
        return new XmlWidgetAsset(kind, id, displayName, width, height, description);
    }

    public XmlWidgetAsset description(String description) {
        return new XmlWidgetAsset(kind, id, displayName, width, height, description);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML asset " + field + " must not be blank");
        return normalized;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
