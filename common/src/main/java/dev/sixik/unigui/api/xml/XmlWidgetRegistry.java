package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.xml.BuiltInWidgetXmlRegistry;
import dev.sixik.unigui.impl.xml.WidgetXmlRegistry;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Изменяемый XML-реестр descriptor-ов виджетов.
 *
 * <p>Через него регистрируются встроенные и модовые XML-типы, алиасы и
 * аннотированные классы виджетов, доступные загрузчику.</p>
 */
public final class XmlWidgetRegistry {
    private final WidgetXmlRegistry delegate;

    private XmlWidgetRegistry(WidgetXmlRegistry delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
    }

    public static XmlWidgetRegistry empty() {
        return new XmlWidgetRegistry(new WidgetXmlRegistry());
    }

    public static XmlWidgetRegistry builtIns() {
        XmlWidgetRegistry registry = new XmlWidgetRegistry(BuiltInWidgetXmlRegistry.create());
        XmlWidgetRegistryContributions.applyTo(registry);
        return registry;
    }

    public <T extends Widget> XmlWidgetType<T> register(String xmlName, Supplier<T> factory) {
        return new XmlWidgetType<>(delegate.register(xmlName, factory));
    }

    public <T extends Widget> XmlWidgetType<T> registerAnnotated(Class<T> widgetType) {
        return XmlWidgetAnnotations.register(this, widgetType);
    }

    public <T extends Widget> XmlWidgetType<T> registerAnnotated(Class<T> widgetType, Supplier<T> factory) {
        return XmlWidgetAnnotations.register(this, widgetType, factory);
    }

    public XmlWidgetRegistry alias(String aliasName, String targetName) {
        delegate.alias(aliasName, targetName);
        return this;
    }

    public boolean contains(String xmlName) {
        return delegate.type(xmlName) != null;
    }

    public Optional<XmlWidgetType<? extends Widget>> type(String xmlName) {
        dev.sixik.unigui.impl.xml.WidgetXmlType<? extends Widget> type = delegate.type(xmlName);
        return type == null ? Optional.empty() : Optional.of(new XmlWidgetType<>(type));
    }

    public Optional<XmlWidgetDescriptor> descriptor(String xmlName) {
        dev.sixik.unigui.impl.xml.WidgetXmlType<? extends Widget> type = delegate.type(xmlName);
        return type == null ? Optional.empty() : Optional.of(type.descriptor());
    }

    public List<XmlWidgetDescriptor> descriptors() {
        return delegate.types().stream()
                .map(dev.sixik.unigui.impl.xml.WidgetXmlType::descriptor)
                .toList();
    }

    WidgetXmlRegistry delegate() {
        return delegate;
    }
}
