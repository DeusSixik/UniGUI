package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.Set;

/** Публичный builder XML-метаданных для одного зарегистрированного типа виджета. */
public final class XmlWidgetType<T extends Widget> {
    private final dev.sixik.unigui.impl.xml.WidgetXmlType<T> delegate;

    XmlWidgetType(dev.sixik.unigui.impl.xml.WidgetXmlType<T> delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
    }

    public String xmlName() {
        return delegate.xmlName();
    }

    public <V> XmlWidgetType<T> attribute(String name,
                                          XmlValueParser<V> parser,
                                          XmlPropertySetter<? super T, ? super V> setter) {
        return attribute(name, parser, setter, XmlAttributeDescriptor.of(name));
    }

    public <V> XmlWidgetType<T> attribute(String name,
                                          XmlValueParser<V> parser,
                                          XmlPropertySetter<? super T, ? super V> setter,
                                          XmlAttributeDescriptor descriptor) {
        if (parser == null || setter == null) {
            throw new IllegalArgumentException("XML attribute parser and setter must not be null");
        }
        delegate.attribute(name, parser::parse, setter::set, descriptor);
        return this;
    }

    public XmlWidgetType<T> describe(String displayName, String category, String description) {
        delegate.describe(displayName, category, description);
        return this;
    }

    public XmlWidgetType<T> describeAttribute(String name, XmlAttributeDescriptor descriptor) {
        delegate.describeAttribute(name, descriptor);
        return this;
    }

    public XmlWidgetType<T> childPolicy(XmlChildPolicy<T> childPolicy) {
        if (childPolicy == null) throw new IllegalArgumentException("XML child policy must not be null");
        delegate.childPolicy(childPolicy::addChild);
        return this;
    }

    public XmlWidgetType<T> propertyChild(String name, XmlChildPolicy<T> childPolicy) {
        return propertyChild(name, childPolicy, XmlPropertyChildDescriptor.of(name));
    }

    public XmlWidgetType<T> propertyChild(String name, XmlChildPolicy<T> childPolicy, XmlPropertyChildDescriptor descriptor) {
        if (childPolicy == null) throw new IllegalArgumentException("XML property child policy must not be null");
        delegate.propertyChild(name, childPolicy::addChild, descriptor);
        return this;
    }

    public XmlWidgetType<T> describePropertyChild(String name, XmlPropertyChildDescriptor descriptor) {
        delegate.describePropertyChild(name, descriptor);
        return this;
    }

    public boolean hasAttribute(String name) {
        return delegate.hasAttribute(name);
    }

    public Set<String> attributeNames() {
        return Set.copyOf(delegate.attributeNames());
    }

    public List<XmlAttributeDescriptor> attributeDescriptors() {
        return delegate.attributeDescriptors();
    }

    public boolean hasPropertyChild(String name) {
        return delegate.hasPropertyChild(name);
    }

    public Set<String> propertyChildNames() {
        return Set.copyOf(delegate.propertyChildNames());
    }

    public List<XmlPropertyChildDescriptor> propertyChildDescriptors() {
        return delegate.propertyChildDescriptors();
    }

    public XmlWidgetDescriptor descriptor() {
        return delegate.descriptor();
    }
}
