package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetDescriptor;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * XML-метаданные одного зарегистрированного типа виджета.
 */
public final class WidgetXmlType<T extends Widget> {
    private final String xmlName;
    private final Supplier<T> factory;
    private final Map<String, XmlAttribute<T, ?>> attributes = new LinkedHashMap<>();
    private final Map<String, XmlChildPolicy<T>> propertyChildren = new LinkedHashMap<>();
    private final Map<String, XmlPropertyChildDescriptor> propertyChildDescriptors = new LinkedHashMap<>();
    private String displayName;
    private String category;
    private String description;
    private XmlChildPolicy<T> childPolicy;

    WidgetXmlType(String xmlName, Supplier<T> factory) {
        this.xmlName = xmlName;
        this.factory = factory;
    }

    public String xmlName() {
        return xmlName;
    }

    public T create() {
        return factory.get();
    }

    public <V> WidgetXmlType<T> attribute(String name, XmlValueParser<V> parser, XmlPropertySetter<? super T, ? super V> setter) {
        return attribute(name, parser, setter, XmlAttributeDescriptor.of(name));
    }

    public <V> WidgetXmlType<T> attribute(String name,
                                          XmlValueParser<V> parser,
                                          XmlPropertySetter<? super T, ? super V> setter,
                                          XmlAttributeDescriptor descriptor) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("XML attribute name must not be blank");
        }
        if (parser == null || setter == null) {
            throw new IllegalArgumentException("XML attribute parser and setter must not be null");
        }
        attributes.put(name, new XmlAttribute<>(name, parser, setter, normalizeDescriptor(name, descriptor)));
        return this;
    }

    public WidgetXmlType<T> describe(String displayName, String category, String description) {
        this.displayName = displayName;
        this.category = category;
        this.description = description;
        return this;
    }

    public WidgetXmlType<T> describeAttribute(String name, XmlAttributeDescriptor descriptor) {
        XmlAttribute<T, ?> attribute = attributes.get(name);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown XML attribute '" + name + "' on " + xmlName);
        }
        attributes.put(name, attribute.descriptor(normalizeDescriptor(name, descriptor)));
        return this;
    }

    public WidgetXmlType<T> childPolicy(XmlChildPolicy<T> childPolicy) {
        this.childPolicy = childPolicy;
        return this;
    }

    public WidgetXmlType<T> propertyChild(String name, XmlChildPolicy<T> childPolicy) {
        return propertyChild(name, childPolicy, XmlPropertyChildDescriptor.of(name));
    }

    public WidgetXmlType<T> propertyChild(String name, XmlChildPolicy<T> childPolicy, XmlPropertyChildDescriptor descriptor) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("XML property child name must not be blank");
        }
        if (name.indexOf('.') >= 0) {
            throw new IllegalArgumentException("XML property child name must not contain '.': " + name);
        }
        if (childPolicy == null) {
            throw new IllegalArgumentException("XML property child policy must not be null");
        }
        if (propertyChildren.putIfAbsent(name, childPolicy) != null) {
            throw new IllegalArgumentException("Duplicate XML property child '" + name + "' on " + xmlName);
        }
        propertyChildDescriptors.put(name, normalizeDescriptor(name, descriptor));
        return this;
    }

    public WidgetXmlType<T> describePropertyChild(String name, XmlPropertyChildDescriptor descriptor) {
        if (!propertyChildren.containsKey(name)) {
            throw new IllegalArgumentException("Unknown XML property child '" + name + "' on " + xmlName);
        }
        propertyChildDescriptors.put(name, normalizeDescriptor(name, descriptor));
        return this;
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public Set<String> attributeNames() {
        return attributes.keySet();
    }

    public List<XmlAttributeDescriptor> attributeDescriptors() {
        return attributes.values().stream().map(XmlAttribute::descriptor).toList();
    }

    public boolean hasPropertyChild(String name) {
        return propertyChildren.containsKey(name);
    }

    public Set<String> propertyChildNames() {
        return propertyChildren.keySet();
    }

    public List<XmlPropertyChildDescriptor> propertyChildDescriptors() {
        return propertyChildDescriptors.values().stream().toList();
    }

    public XmlWidgetDescriptor descriptor() {
        return new XmlWidgetDescriptor(
                xmlName,
                displayName,
                categoryFor(xmlName, category),
                description,
                acceptsChildren(),
                attributeDescriptors(),
                propertyChildDescriptors());
    }

    public void applyAttributes(T widget, Map<String, XmlWidgetLoader.XmlAttributeValue> values) {
        for (XmlAttribute<T, ?> attribute : attributes.values()) {
            XmlWidgetLoader.XmlAttributeValue value = values.get(attribute.name());
            if (value != null) {
                attribute.apply(widget, value);
            }
        }
    }

    public boolean acceptsChildren() {
        return childPolicy != null;
    }

    public void addChild(T parent, Widget child) {
        if (childPolicy == null) {
            throw new IllegalArgumentException("Widget " + xmlName + " cannot contain child "
                    + child.getClass().getSimpleName() + ".");
        }
        childPolicy.addChild(parent, child);
    }

    public void addPropertyChild(T parent, String propertyName, Widget child) {
        XmlChildPolicy<T> policy = propertyChildren.get(propertyName);
        if (policy == null) {
            throw new IllegalArgumentException("Unknown property element '" + xmlName + "." + propertyName
                    + "' on " + xmlName + ".");
        }
        policy.addChild(parent, child);
    }

    private record XmlAttribute<T extends Widget, V>(
            String name,
            XmlValueParser<V> parser,
            XmlPropertySetter<? super T, ? super V> setter,
            XmlAttributeDescriptor descriptor) {
        private XmlAttribute<T, V> descriptor(XmlAttributeDescriptor descriptor) {
            return new XmlAttribute<>(name, parser, setter, descriptor);
        }

        private void apply(T widget, XmlWidgetLoader.XmlAttributeValue value) {
            try {
                setter.set(widget, parser.parse(value.value()));
            } catch (RuntimeException failure) {
                throw new XmlAttributeApplyException(
                        value.line(),
                        value.column(),
                        "Cannot parse/apply attribute '" + value.displayName()
                                + "' value '" + value.value() + "': " + failure.getMessage(),
                        failure);
            }
        }
    }

    private static XmlAttributeDescriptor normalizeDescriptor(String name, XmlAttributeDescriptor descriptor) {
        if (descriptor == null) return XmlAttributeDescriptor.of(name);
        if (descriptor.name().equals(name)) return descriptor;
        return new XmlAttributeDescriptor(
                name,
                descriptor.displayName(),
                descriptor.category(),
                descriptor.defaultValue(),
                descriptor.description(),
                descriptor.valueType());
    }

    private static XmlPropertyChildDescriptor normalizeDescriptor(String name, XmlPropertyChildDescriptor descriptor) {
        if (descriptor == null) return XmlPropertyChildDescriptor.of(name);
        if (descriptor.name().equals(name)) return descriptor;
        return new XmlPropertyChildDescriptor(
                name,
                descriptor.displayName(),
                descriptor.category(),
                descriptor.description(),
                descriptor.singleChild());
    }

    private static String categoryFor(String xmlName, String explicitCategory) {
        if (explicitCategory != null && !explicitCategory.isBlank()) return explicitCategory;
        return switch (xmlName) {
            case "Panel", "Box", "VBox", "HBox", "StackPanel", "WrapPanel", "ScrollView" -> "Containers";
            case "TextWidget", "Text", "TextBlock", "Label", "TextureWidget", "ImageView", "Separator" -> "Display";
            case "Button", "ToggleButton", "Checkbox", "Slider", "ProgressBar" -> "Controls";
            default -> "Widgets";
        };
    }

    static final class XmlAttributeApplyException extends RuntimeException {
        private final int line;
        private final int column;

        private XmlAttributeApplyException(int line, int column, String message, Throwable cause) {
            super(message, cause);
            this.line = line;
            this.column = column;
        }

        int line() {
            return line;
        }

        int column() {
            return column;
        }
    }
}
