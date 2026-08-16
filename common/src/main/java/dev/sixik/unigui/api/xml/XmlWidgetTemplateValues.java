package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Неизменяемый набор override-атрибутов исходного документа для экземпляра widget template.
 *
 * <p>Values применяются при {@link XmlWidgetTemplate#instantiate(XmlWidgetTemplateValues)}.
 * Root override меняет атрибут корневого элемента, а обычный override ищет target по id/name.</p>
 */
public final class XmlWidgetTemplateValues {
    private static final XmlWidgetTemplateValues EMPTY = new XmlWidgetTemplateValues(List.of());

    private final List<AttributeOverride> attributes;

    private XmlWidgetTemplateValues(List<AttributeOverride> attributes) {
        this.attributes = List.copyOf(attributes == null ? List.of() : attributes);
    }

    /**
     * Возвращает общий пустой набор overrides.
     *
     * @return empty template values
     */
    public static XmlWidgetTemplateValues empty() {
        return EMPTY;
    }

    /**
     * Возвращает новый набор с override-ом атрибута root element-а.
     *
     * @param name имя атрибута
     * @param value новое значение
     * @return новый values instance
     */
    public XmlWidgetTemplateValues rootAttribute(String name, String value) {
        return attribute("", name, value);
    }

    /**
     * Возвращает новый набор с override-ом атрибута элемента по id/name.
     *
     * @param elementId id/name целевого элемента; blank означает root
     * @param name имя атрибута
     * @param value новое значение
     * @return новый values instance
     */
    public XmlWidgetTemplateValues attribute(String elementId, String name, String value) {
        ArrayList<AttributeOverride> next = new ArrayList<>(attributes);
        next.add(new AttributeOverride(elementId, name, value));
        return new XmlWidgetTemplateValues(next);
    }

    /**
     * Возвращает overrides в порядке добавления.
     *
     * @return immutable список overrides
     */
    public List<AttributeOverride> attributes() {
        return attributes;
    }

    /**
     * Проверяет, что overrides отсутствуют.
     *
     * @return {@code true}, если список пуст
     */
    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    /**
     * Один override XML-атрибута при instantiation template-а.
     *
     * @param elementId id/name target element-а; пустая строка означает root
     * @param name имя атрибута
     * @param value новое значение атрибута
     */
    public record AttributeOverride(String elementId, String name, String value) {
        public AttributeOverride {
            elementId = elementId == null ? "" : elementId.trim();
            name = normalizeName(name);
            value = value == null ? "" : value;
        }

        /**
         * Проверяет, что override применяется к root element-у.
         *
         * @return {@code true}, если elementId пустой
         */
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
