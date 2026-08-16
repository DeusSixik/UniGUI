package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ручной XML-реестр виджетов, который использует загрузчик.
 */
public final class WidgetXmlRegistry {
    private final Map<String, WidgetXmlType<? extends Widget>> types = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public <T extends Widget> WidgetXmlType<T> register(String xmlName, Supplier<T> factory) {
        if (xmlName == null || xmlName.isBlank()) {
            throw new IllegalArgumentException("XML widget name must not be blank");
        }
        if (factory == null) {
            throw new IllegalArgumentException("XML widget factory must not be null");
        }
        if (aliases.containsKey(xmlName)) {
            throw new IllegalArgumentException("XML widget name is already used by an alias: " + xmlName);
        }
        WidgetXmlType<T> type = new WidgetXmlType<>(xmlName, factory);
        if (types.putIfAbsent(xmlName, type) != null) {
            throw new IllegalArgumentException("Duplicate XML widget type: " + xmlName);
        }
        return type;
    }

    public WidgetXmlRegistry alias(String aliasName, String targetName) {
        if (aliasName == null || aliasName.isBlank()) {
            throw new IllegalArgumentException("XML widget alias must not be blank");
        }
        if (targetName == null || targetName.isBlank()) {
            throw new IllegalArgumentException("XML widget alias target must not be blank");
        }
        if (types.containsKey(aliasName) || aliases.containsKey(aliasName)) {
            throw new IllegalArgumentException("Duplicate XML widget alias: " + aliasName);
        }
        WidgetXmlType<? extends Widget> target = type(targetName);
        if (target == null) {
            throw new IllegalArgumentException("Unknown XML widget alias target: " + targetName);
        }
        aliases.put(aliasName, target.xmlName());
        return this;
    }

    public WidgetXmlType<? extends Widget> type(String xmlName) {
        WidgetXmlType<? extends Widget> type = types.get(xmlName);
        if (type != null) return type;
        String targetName = aliases.get(xmlName);
        return targetName == null ? null : types.get(targetName);
    }

    public List<WidgetXmlType<? extends Widget>> types() {
        return List.copyOf(types.values());
    }
}
