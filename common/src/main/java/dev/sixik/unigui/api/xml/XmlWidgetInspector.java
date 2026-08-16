package dev.sixik.unigui.api.xml;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Модель инспектора source XML-элементов, построенная на descriptor metadata. */
public final class XmlWidgetInspector {
    private static final XmlAttributeDescriptor ID_ATTRIBUTE = XmlAttributeDescriptor.of("id")
            .displayName("Id")
            .category("Common")
            .defaultValue("")
            .description("Runtime/debug/editor identifier for code-behind lookup.");

    private XmlWidgetInspector() {
    }

    public static Inspection inspect(XmlWidgetElement element) {
        return inspect(element, XmlWidgetRegistry.builtIns());
    }

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

        public boolean knownWidget() {
            return descriptor.isPresent();
        }

        public String displayName() {
            return descriptor.map(XmlWidgetDescriptor::displayName).orElse(element.name());
        }

        public String category() {
            return descriptor.map(XmlWidgetDescriptor::category).orElse("Unknown");
        }

        public Optional<Attribute> attribute(String name) {
            String normalized = localName(name);
            return attributes.stream()
                    .filter(attribute -> attribute.name().equals(normalized)
                            || attribute.source().name().equals(name))
                    .findFirst();
        }
    }

    public record Attribute(
            XmlWidgetAttribute source,
            String name,
            Optional<XmlAttributeDescriptor> descriptor) {
        public Attribute {
            if (source == null) throw new IllegalArgumentException("XML widget inspected attribute must not be null");
            name = name == null || name.isBlank() ? source.name() : name;
            descriptor = descriptor == null ? Optional.empty() : descriptor;
        }

        public boolean known() {
            return descriptor.isPresent();
        }

        public String value() {
            return source.value();
        }

        public String displayName() {
            return descriptor.map(XmlAttributeDescriptor::displayName).orElse(source.name());
        }

        public String category() {
            return descriptor.map(XmlAttributeDescriptor::category).orElse("Unknown");
        }

        public String defaultValue() {
            return descriptor.map(XmlAttributeDescriptor::defaultValue).orElse("");
        }

        public String description() {
            return descriptor.map(XmlAttributeDescriptor::description).orElse("");
        }
    }
}
