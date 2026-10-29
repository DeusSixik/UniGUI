package dev.sixik.unigui.api.xml;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Модель инспектора source XML-элементов, построенная на descriptor metadata.
 *
 * <p>Inspector сопоставляет реальные атрибуты элемента с {@link XmlAttributeDescriptor}
 * из реестра, добавляет editor-level {@code id/name} атрибуты и возвращает список доступных
 * атрибутов для UI. Он не меняет документ и не парсит значения атрибутов.</p>
 */
public final class XmlWidgetInspector {
    private static final XmlAttributeDescriptor ID_ATTRIBUTE = XmlAttributeDescriptor.of("id")
            .displayName("Id")
            .category("Common")
            .defaultValue("")
            .description("Runtime/debug/editor identifier for code-behind lookup.");

    private XmlWidgetInspector() {
    }

    /**
     * Инспектирует элемент против встроенного XML-реестра.
     *
     * @param element source XML element
     * @return immutable inspection snapshot
     */
    public static Inspection inspect(XmlWidgetElement element) {
        return inspect(element, XmlWidgetRegistry.builtIns());
    }

    /**
     * Инспектирует элемент против указанного XML-реестра.
     *
     * @param element source XML element; не может быть {@code null}
     * @param registry реестр descriptor-ов; {@code null} заменяется built-ins
     * @return immutable inspection snapshot
     */
    public static Inspection inspect(XmlWidgetElement element, XmlWidgetRegistry registry) {
        if (element == null) throw new IllegalArgumentException("XML widget element must not be null");
        XmlWidgetRegistry normalized = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        Optional<XmlWidgetDescriptor> descriptor = element.propertyElement()
                ? Optional.empty()
                : normalized.descriptor(element.name());
        List<Attribute> attributes = new ArrayList<>();
        for (XmlWidgetAttribute attribute : element.attributes()) {
            if (namespaceDeclaration(attribute)) continue;
            attributes.add(inspectAttribute(attribute, descriptor));
        }
        List<XmlAttributeDescriptor> availableAttributes = descriptor
                .map(XmlWidgetInspector::availableAttributes)
                .orElseGet(List::of);
        List<XmlPropertyChildDescriptor> propertyChildren = descriptor
                .map(XmlWidgetDescriptor::propertyChildren)
                .orElseGet(List::of);
        return new Inspection(element, descriptor, attributes, availableAttributes, propertyChildren);
    }

    private static Attribute inspectAttribute(XmlWidgetAttribute attribute, Optional<XmlWidgetDescriptor> descriptor) {
        String localName = localName(attribute.name());
        Optional<XmlAttributeDescriptor> attributeDescriptor = idAttribute(localName)
                ? Optional.of(ID_ATTRIBUTE)
                : descriptor.flatMap(widget -> widget.attributes().stream()
                .filter(candidate -> candidate.name().equals(localName))
                .findFirst());
        return new Attribute(attribute, localName, attributeDescriptor);
    }

    private static List<XmlAttributeDescriptor> availableAttributes(XmlWidgetDescriptor descriptor) {
        List<XmlAttributeDescriptor> attributes = new ArrayList<>();
        attributes.add(ID_ATTRIBUTE);
        attributes.addAll(descriptor.attributes());
        return List.copyOf(attributes);
    }

    private static boolean idAttribute(String localName) {
        return "id".equals(localName) || "name".equals(localName) || "Name".equals(localName);
    }

    private static boolean namespaceDeclaration(XmlWidgetAttribute attribute) {
        String name = attribute.name();
        return XMLConstants.XMLNS_ATTRIBUTE.equals(name)
                || name.startsWith(XMLConstants.XMLNS_ATTRIBUTE + ":");
    }

    private static String localName(String name) {
        int prefix = name.indexOf(':');
        return prefix >= 0 ? name.substring(prefix + 1) : name;
    }

    /**
     * Результат инспекции одного source XML-элемента.
     *
     * @param element исходный элемент
     * @param descriptor descriptor виджета, если тип известен
     * @param attributes атрибуты элемента с metadata, если она найдена
     * @param availableAttributes атрибуты, которые можно добавить через inspector UI
     * @param propertyChildren property-child слоты, доступные для этого виджета
     */
    public record Inspection(
            XmlWidgetElement element,
            Optional<XmlWidgetDescriptor> descriptor,
            List<Attribute> attributes,
            List<XmlAttributeDescriptor> availableAttributes,
            List<XmlPropertyChildDescriptor> propertyChildren) {
        public Inspection {
            if (element == null) throw new IllegalArgumentException("XML widget inspection element must not be null");
            descriptor = descriptor == null ? Optional.empty() : descriptor;
            attributes = List.copyOf(attributes == null ? List.of() : attributes);
            availableAttributes = List.copyOf(availableAttributes == null ? List.of() : availableAttributes);
            propertyChildren = List.copyOf(propertyChildren == null ? List.of() : propertyChildren);
        }

        /**
         * Проверяет, найден ли descriptor для XML-типа элемента.
         *
         * @return {@code true}, если widget type известен реестру
         */
        public boolean knownWidget() {
            return descriptor.isPresent();
        }

        /**
         * Возвращает display name для inspector header.
         *
         * @return display name descriptor-а или raw XML tag name
         */
        public String displayName() {
            return descriptor.map(XmlWidgetDescriptor::displayName).orElse(element.name());
        }

        /**
         * Возвращает категорию виджета.
         *
         * @return category descriptor-а или {@code Unknown}
         */
        public String category() {
            return descriptor.map(XmlWidgetDescriptor::category).orElse("Unknown");
        }

        /**
         * Находит атрибут по raw или local name.
         *
         * @param name имя атрибута, возможно с namespace prefix
         * @return inspected attribute или empty
         */
        public Optional<Attribute> attribute(String name) {
            String normalized = localName(name);
            return attributes.stream()
                    .filter(attribute -> attribute.name().equals(normalized)
                            || attribute.source().name().equals(name))
                    .findFirst();
        }
    }

    /**
     * Атрибут source element-а, обогащённый descriptor metadata.
     *
     * @param source исходный XML-атрибут
     * @param name local name без namespace prefix
     * @param descriptor descriptor атрибута, если он известен реестру
     */
    public record Attribute(
            XmlWidgetAttribute source,
            String name,
            Optional<XmlAttributeDescriptor> descriptor) {
        public Attribute {
            if (source == null) throw new IllegalArgumentException("XML widget inspected attribute must not be null");
            name = name == null || name.isBlank() ? source.name() : name;
            descriptor = descriptor == null ? Optional.empty() : descriptor;
        }

        /**
         * Проверяет, что атрибут описан descriptor-ом.
         *
         * @return {@code true}, если metadata найдена
         */
        public boolean known() {
            return descriptor.isPresent();
        }

        /**
         * Возвращает текущее строковое значение source attribute.
         *
         * @return XML attribute value
         */
        public String value() {
            return source.value();
        }

        /**
         * Возвращает имя для отображения в inspector UI.
         *
         * @return descriptor display name или raw source name
         */
        public String displayName() {
            return descriptor.map(XmlAttributeDescriptor::displayName).orElse(source.name());
        }

        /**
         * Возвращает категорию атрибута.
         *
         * @return descriptor category или {@code Unknown}
         */
        public String category() {
            return descriptor.map(XmlAttributeDescriptor::category).orElse("Unknown");
        }

        /**
         * Возвращает default value из descriptor metadata.
         *
         * @return default value или пустая строка
         */
        public String defaultValue() {
            return descriptor.map(XmlAttributeDescriptor::defaultValue).orElse("");
        }

        /**
         * Возвращает описание атрибута для tooltip/help UI.
         *
         * @return описание или пустая строка
         */
        public String description() {
            return descriptor.map(XmlAttributeDescriptor::description).orElse("");
        }
    }
}
