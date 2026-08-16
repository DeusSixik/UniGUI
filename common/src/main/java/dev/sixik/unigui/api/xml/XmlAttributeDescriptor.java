package dev.sixik.unigui.api.xml;

/**
 * Read-only metadata одного XML-атрибута для редактора и инспектора.
 *
 * <p>Descriptor не участвует в вычислении значения атрибута. Он нужен для
 * user-facing UI: как подписать поле, в какую группу положить, какое значение
 * показать как дефолтное и какую подсказку отдать пользователю.</p>
 *
 * <p>Если часть полей не задана, constructor подставляет безопасные fallback'и:
 * display name строится из имени атрибута, category определяется эвристикой,
 * default/description становятся пустыми строками.</p>
 *
 * @param name точное XML-имя атрибута
 * @param displayName человекочитаемая подпись в UI
 * @param category группа инспектора, например {@code "Layout"} или {@code "Assets"}
 * @param defaultValue строковое значение по умолчанию в XML-синтаксисе
 * @param description краткое описание назначения атрибута
 */
public record XmlAttributeDescriptor(
        String name,
        String displayName,
        String category,
        String defaultValue,
        String description) {
    /** Нормализует обязательное имя и optional metadata-поля. */
    public XmlAttributeDescriptor {
        name = normalizeRequired(name, "name");
        displayName = normalize(displayName, displayNameFor(name));
        category = normalize(category, categoryFor(name));
        defaultValue = normalize(defaultValue, "");
        description = normalize(description, "");
    }

    /**
     * Создаёт descriptor с metadata по эвристикам имени.
     *
     * @param name XML-имя атрибута
     * @return descriptor с auto display/category и пустыми default/description
     */
    public static XmlAttributeDescriptor of(String name) {
        return new XmlAttributeDescriptor(name, null, null, null, null);
    }

    /**
     * Возвращает копию descriptor-а с новым display name.
     *
     * @param displayName человекочитаемая подпись
     * @return новая копия descriptor-а
     */
    public XmlAttributeDescriptor displayName(String displayName) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description);
    }

    /**
     * Возвращает копию descriptor-а с новой category.
     *
     * @param category группа инспектора
     * @return новая копия descriptor-а
     */
    public XmlAttributeDescriptor category(String category) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description);
    }

    /**
     * Возвращает копию descriptor-а с новым default value.
     *
     * @param defaultValue строковое значение по умолчанию
     * @return новая копия descriptor-а
     */
    public XmlAttributeDescriptor defaultValue(String defaultValue) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description);
    }

    /**
     * Возвращает копию descriptor-а с новым описанием.
     *
     * @param description подсказка для пользователя редактора
     * @return новая копия descriptor-а
     */
    public XmlAttributeDescriptor description(String description) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("XML attribute descriptor " + field + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String categoryFor(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("texture") || lower.contains("background") || lower.contains("border")
                || lower.contains("color") || lower.equals("radius") || lower.equals("tint")) {
            return "Appearance";
        }
        if (lower.contains("width") || lower.contains("height") || lower.contains("padding")
                || lower.contains("margin") || lower.contains("flex") || lower.contains("align")
                || lower.contains("justify") || lower.contains("overflow") || lower.equals("x")
                || lower.equals("y") || lower.equals("left") || lower.equals("top")
                || lower.equals("right") || lower.equals("bottom") || lower.contains("gap")
                || lower.equals("position")) {
            return "Layout";
        }
        if (lower.contains("text") || lower.equals("wrap") || lower.contains("marquee")) {
            return "Content";
        }
        if (lower.equals("value") || lower.equals("min") || lower.equals("max") || lower.equals("step")
                || lower.contains("checked") || lower.equals("state") || lower.contains("enabled")) {
            return "Behavior";
        }
        return "Common";
    }

    private static String displayNameFor(String name) {
        StringBuilder builder = new StringBuilder();
        char previous = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(previous)) {
                builder.append(' ');
            }
            builder.append(i == 0 ? Character.toUpperCase(c) : c);
            previous = c;
        }
        return builder.toString().replace('-', ' ').replace('_', ' ');
    }
}
