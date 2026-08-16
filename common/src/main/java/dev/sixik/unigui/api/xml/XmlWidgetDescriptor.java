package dev.sixik.unigui.api.xml;

import java.util.List;

/** Неизменяемый снимок XML-метаданных виджета для палитр редактора и инспекторов. */
public record XmlWidgetDescriptor(
        String xmlName,
        String displayName,
        String category,
        String description,
        boolean acceptsChildren,
        List<XmlAttributeDescriptor> attributes,
        List<XmlPropertyChildDescriptor> propertyChildren) {
    public XmlWidgetDescriptor {
        xmlName = normalizeRequired(xmlName, "xmlName");
        displayName = normalize(displayName, xmlName);
        category = normalize(category, "Widgets");
        description = normalize(description, "");
        attributes = List.copyOf(attributes == null ? List.of() : attributes);
        propertyChildren = List.copyOf(propertyChildren == null ? List.of() : propertyChildren);
    }

    public static XmlWidgetDescriptor of(String xmlName) {
        return new XmlWidgetDescriptor(xmlName, null, null, null, false, List.of(), List.of());
    }

    public XmlWidgetDescriptor displayName(String displayName) {
        return new XmlWidgetDescriptor(xmlName, displayName, category, description, acceptsChildren, attributes, propertyChildren);
    }

    public XmlWidgetDescriptor category(String category) {
        return new XmlWidgetDescriptor(xmlName, displayName, category, description, acceptsChildren, attributes, propertyChildren);
    }

    public XmlWidgetDescriptor description(String description) {
        return new XmlWidgetDescriptor(xmlName, displayName, category, description, acceptsChildren, attributes, propertyChildren);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("XML widget descriptor " + field + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
