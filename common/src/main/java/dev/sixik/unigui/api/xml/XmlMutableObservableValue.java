package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.event.EventSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Простое in-memory observable-значение для code-behind и редакторских прототипов. */
public final class XmlMutableObservableValue<T> implements XmlObservableValue<T> {
    private final String name;
    private final Class<T> valueType;
    private final List<XmlBindingListener<T>> listeners = new ArrayList<>();
    private T value;

    private XmlMutableObservableValue(String name, Class<T> valueType, T value) {
        this.name = normalizeName(name);
        this.valueType = requireType(valueType);
        this.value = checked(value);
    }

    public static <T> XmlMutableObservableValue<T> of(String name, Class<T> valueType, T value) {
        return new XmlMutableObservableValue<>(name, valueType, value);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<T> valueType() {
        return valueType;
    }

    @Override
    public T get() {
        return value;
    }

    public XmlMutableObservableValue<T> set(T value) {
        T next = checked(value);
        if (Objects.equals(this.value, next)) return this;

        T old = this.value;
        this.value = next;
        XmlBindingChange<T> change = new XmlBindingChange<>(name, valueType, old, next);
        for (XmlBindingListener<T> listener : List.copyOf(listeners)) {
            listener.changed(change);
        }
        return this;
    }

    @Override
    public EventSubscription onChanged(XmlBindingListener<T> listener) {
        if (listener == null) throw new IllegalArgumentException("XML binding listener must not be null");
        listeners.add(listener);
        return new EventSubscription() {
            private boolean subscribed = true;

            @Override
            public void unsubscribe() {
                if (!subscribed) return;
                subscribed = false;
                listeners.remove(listener);
            }
        };
    }

    private T checked(T value) {
        if (value != null && !valueType.isInstance(value)) {
            throw new IllegalArgumentException("XML binding value '" + name + "' expected "
                    + valueType.getSimpleName() + ", got " + value.getClass().getSimpleName());
        }
        return value;
    }

    private static <T> Class<T> requireType(Class<T> type) {
        if (type == null) throw new IllegalArgumentException("XML binding value type must not be null");
        return type;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML binding value name must not be blank");
        return normalized;
    }
}
