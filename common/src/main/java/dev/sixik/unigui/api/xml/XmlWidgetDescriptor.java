package dev.sixik.unigui.api.xml;

import java.util.List;

/**
 * Неизменяемый снимок XML-метаданных виджета для палитр редактора и инспекторов.
 *
 * <p>Descriptor собирается из {@link XmlWidgetType} и хранит только read-only metadata:
 * имя XML-типа, display label, категорию, описание, список атрибутов и property-child slots.</p>
 *
 * @param xmlName XML tag name виджета
 * @param displayName имя для UI; blank значение заменяется xmlName
 * @param category категория palette/inspector
 * @param description описание widget type
 * @param acceptsChildren может ли widget принимать обычные child elements
 * @param attributes metadata атрибутов
 * @param propertyChildren metadata property-child slots
 */
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
        description = normalize(description, descriptionFor(displayName, category, acceptsChildren));
        attributes = List.copyOf(attributes == null ? List.of() : attributes);
        propertyChildren = List.copyOf(propertyChildren == null ? List.of() : propertyChildren);
    }

    /**
     * Создаёт descriptor только с XML-именем и дефолтной metadata.
     *
     * @param xmlName XML tag name виджета
     * @return descriptor
     */
    public static XmlWidgetDescriptor of(String xmlName) {
        return new XmlWidgetDescriptor(xmlName, null, null, null, false, List.of(), List.of());
    }

    /**
     * Возвращает копию descriptor-а с новым display name.
     *
     * @param displayName имя для UI
     * @return новый descriptor instance
     */
    public XmlWidgetDescriptor displayName(String displayName) {
        return new XmlWidgetDescriptor(xmlName, displayName, category, description, acceptsChildren, attributes, propertyChildren);
    }

    /**
     * Возвращает копию descriptor-а с новой категорией.
     *
     * @param category категория palette/inspector
     * @return новый descriptor instance
     */
    public XmlWidgetDescriptor category(String category) {
        return new XmlWidgetDescriptor(xmlName, displayName, category, description, acceptsChildren, attributes, propertyChildren);
    }

    /**
     * Возвращает копию descriptor-а с новым описанием.
     *
     * @param description описание widget type
     * @return новый descriptor instance
     */
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

    private static String descriptionFor(String displayName, String category, boolean acceptsChildren) {
        String normalizedDisplay = normalize(displayName, "Widget");
        String normalizedCategory = normalize(category, "Widgets");
        String kind = switch (normalizedCategory) {
            case "Containers" -> "container";
            case "Controls" -> "control";
            case "Display" -> "display";
            case "Navigation" -> "navigation";
            case "Feedback" -> "feedback";
            case "Data" -> "data";
            case "Editor" -> "editor";
            case "Minecraft" -> "Minecraft";
            case "Performance" -> "performance";
            default -> "widget";
        };
        return normalizedDisplay + " XML " + kind + " widget"
                + (acceptsChildren ? " with child widget support." : ".");
    }
}
