package dev.sixik.unigui.api.xml;

/** XML-атрибут исходного документа, сохраняемый моделью документа. */
public record XmlWidgetAttribute(String name, String value, int line, int column) {
    public XmlWidgetAttribute(String name, String value) {
        this(name, value, -1, -1);
    }

    public XmlWidgetAttribute {
        name = normalizeName(name);
        value = value == null ? "" : value;
    }

    public boolean hasLocation() {
        return line >= 0 && column >= 0;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("XML attribute name must not be blank");
        }
        return normalized;
    }
}
