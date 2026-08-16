package dev.sixik.unigui.api.xml;

/** Метаданные редактора/инспектора только для чтения для property-element слота дочернего виджета. */
public record XmlPropertyChildDescriptor(
        String name,
        String displayName,
        String category,
        String description) {
    public XmlPropertyChildDescriptor {
        name = normalizeRequired(name, "name");
        displayName = normalize(displayName, name);
        category = normalize(category, "Children");
        description = normalize(description, "");
    }

    public static XmlPropertyChildDescriptor of(String name) {
        return new XmlPropertyChildDescriptor(name, null, null, null);
    }

    public XmlPropertyChildDescriptor displayName(String displayName) {
        return new XmlPropertyChildDescriptor(name, displayName, category, description);
    }

    public XmlPropertyChildDescriptor category(String category) {
        return new XmlPropertyChildDescriptor(name, displayName, category, description);
    }

    public XmlPropertyChildDescriptor description(String description) {
        return new XmlPropertyChildDescriptor(name, displayName, category, description);
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
