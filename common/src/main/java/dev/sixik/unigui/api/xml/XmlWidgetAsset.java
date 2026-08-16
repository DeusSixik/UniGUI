package dev.sixik.unigui.api.xml;

/**
 * Запись ассета без привязки к backend-у, которую используют XML-выборщики ассетов редактора.
 *
 * <p>Asset описывает id, подпись и optional metadata, но не хранит native handle или загруженный ресурс.
 * Это позволяет строить palette/picker UI независимо от конкретного render backend-а.</p>
 *
 * @param kind категория ассета
 * @param id стабильный id, который записывается в XML-атрибут
 * @param displayName человекочитаемое имя; blank значение заменяется id
 * @param width ширина texture asset-а или 0, если неизвестна/неприменима
 * @param height высота texture asset-а или 0, если неизвестна/неприменима
 * @param description описание для tooltip/help UI
 */
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

    /**
     * Создаёт texture asset с размерами.
     *
     * @param id texture id для XML
     * @param width ширина текстуры
     * @param height высота текстуры
     * @return texture asset
     */
    public static XmlWidgetAsset texture(String id, int width, int height) {
        return new XmlWidgetAsset(XmlWidgetAssetKind.TEXTURE, id, null, width, height, null);
    }

    /**
     * Создаёт font asset.
     *
     * @param id font id для XML
     * @return font asset
     */
    public static XmlWidgetAsset font(String id) {
        return new XmlWidgetAsset(XmlWidgetAssetKind.FONT, id, null, 0, 0, null);
    }

    /**
     * Создаёт shader asset.
     *
     * @param id shader id для XML
     * @return shader asset
     */
    public static XmlWidgetAsset shader(String id) {
        return new XmlWidgetAsset(XmlWidgetAssetKind.SHADER, id, null, 0, 0, null);
    }

    /**
     * Проверяет, есть ли у asset-а валидные размеры.
     *
     * @return {@code true}, если width и height больше 0
     */
    public boolean hasDimensions() {
        return width > 0 && height > 0;
    }

    /**
     * Возвращает копию asset-а с новым display name.
     *
     * @param displayName новое имя для UI
     * @return новый asset instance
     */
    public XmlWidgetAsset displayName(String displayName) {
        return new XmlWidgetAsset(kind, id, displayName, width, height, description);
    }

    /**
     * Возвращает копию asset-а с новым описанием.
     *
     * @param description новое описание
     * @return новый asset instance
     */
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
