package dev.sixik.unigui.api.xml;

/**
 * Read-only metadata property-child слота дочернего виджета.
 *
 * <p>Property-child — это именованный XML-элемент вида
 * {@code <ScrollView.Content>...</ScrollView.Content>}. Он нужен, когда child
 * должен попасть не в обычный список детей, а в конкретный слот родителя.</p>
 *
 * @param name имя слота без имени родительского типа, например {@code "Content"}
 * @param displayName подпись слота в inspector UI
 * @param category группа inspector UI; по умолчанию {@code "Children"}
 * @param description краткое описание назначения слота
 */
public record XmlPropertyChildDescriptor(
        String name,
        String displayName,
        String category,
        String description,
        boolean singleChild) {
    public XmlPropertyChildDescriptor(String name,
                                      String displayName,
                                      String category,
                                      String description) {
        this(name, displayName, category, description, false);
    }

    /** Нормализует обязательное имя и optional metadata-поля. */
    public XmlPropertyChildDescriptor {
        name = normalizeRequired(name, "name");
        displayName = normalize(displayName, name);
        category = normalize(category, "Children");
        description = normalize(description, "");
    }

    /**
     * Создаёт descriptor property-child слота с дефолтной metadata.
     *
     * @param name имя слота
     * @return descriptor слота
     */
    public static XmlPropertyChildDescriptor of(String name) {
        return new XmlPropertyChildDescriptor(name, null, null, null, false);
    }

    /**
     * Возвращает копию descriptor-а с новым display name.
     *
     * @param displayName подпись в editor UI
     * @return новая копия descriptor-а
     */
    public XmlPropertyChildDescriptor displayName(String displayName) {
        return new XmlPropertyChildDescriptor(name, displayName, category, description, singleChild);
    }

    /**
     * Возвращает копию descriptor-а с новой category.
     *
     * @param category группа inspector UI
     * @return новая копия descriptor-а
     */
    public XmlPropertyChildDescriptor category(String category) {
        return new XmlPropertyChildDescriptor(name, displayName, category, description, singleChild);
    }

    /**
     * Возвращает копию descriptor-а с новым описанием.
     *
     * @param description подсказка для пользователя редактора
     * @return новая копия descriptor-а
     */
    public XmlPropertyChildDescriptor description(String description) {
        return new XmlPropertyChildDescriptor(name, displayName, category, description, singleChild);
    }

    public XmlPropertyChildDescriptor singleChildOnly() {
        return singleChild(true);
    }

    public XmlPropertyChildDescriptor singleChild(boolean singleChild) {
        return new XmlPropertyChildDescriptor(name, displayName, category, description, singleChild);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("XML property child descriptor " + field + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
