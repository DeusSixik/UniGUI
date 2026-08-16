package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;

/** Неизменяемый набор override-атрибутов исходного документа для экземпляра widget template. */
public final class XmlWidgetTemplateValues {
    private static final XmlWidgetTemplateValues EMPTY = new XmlWidgetTemplateValues(List.of());

    private final List<AttributeOverride> attributes;

    private XmlWidgetTemplateValues(List<AttributeOverride> attributes) {
        this.attributes = List.copyOf(attributes == null ? List.of() : attributes);
    }

    public static XmlWidgetTemplateValues empty() {
        return EMPTY;
    }

    public XmlWidgetTemplateValues rootAttribute(String name, String value) {
        return attribute("", name, value);
    }

    public XmlWidgetTemplateValues attribute(String elementId, String name, String value) {
        ArrayList<AttributeOverride> next = new ArrayList<>(attributes);
        next.add(new AttributeOverride(elementId, name, value));
        return new XmlWidgetTemplateValues(next);
    }

    public List<AttributeOverride> attributes() {
        return attributes;
    }

    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    public record AttributeOverride(String elementId, String name, String value) {
        public AttributeOverride {
            elementId = elementId == null ? "" : elementId.trim();
            name = normalizeName(name);
            value = value == null ? "" : value;
        }

        public boolean rootTarget() {
            return elementId.isEmpty();
        }

        private static String normalizeName(String name) {
            String normalized = name == null ? "" : name.trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException("XML template attribute name must not be blank");
            return normalized;
        }
    }
}
