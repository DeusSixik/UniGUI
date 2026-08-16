package dev.sixik.unigui.api.xml;

/** Снимок изменения типизированного значения-источника XML-binding. */
public record XmlBindingChange<T>(String name, Class<T> valueType, T oldValue, T newValue) {
    public XmlBindingChange {
        name = name == null ? "" : name.trim();
        if (valueType == null) throw new IllegalArgumentException("XML binding value type must not be null");
    }
}
