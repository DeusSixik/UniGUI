package dev.sixik.unigui.api.xml;

/** Метаданные редактора/инспектора только для чтения для одного XML-атрибута. */
public record XmlAttributeDescriptor(
        String name,
        String displayName,
        String category,
        String defaultValue,
        String description) {
    public XmlAttributeDescriptor {
        name = normalizeRequired(name, "name");
        displayName = normalize(displayName, displayNameFor(name));
        category = normalize(category, categoryFor(name));
        defaultValue = normalize(defaultValue, "");
        description = normalize(description, "");
    }

    public static XmlAttributeDescriptor of(String name) {
        return new XmlAttributeDescriptor(name, null, null, null, null);
    }

    public XmlAttributeDescriptor displayName(String displayName) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description);
    }

    public XmlAttributeDescriptor category(String category) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description);
    }

    public XmlAttributeDescriptor defaultValue(String defaultValue) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description);
    }

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
