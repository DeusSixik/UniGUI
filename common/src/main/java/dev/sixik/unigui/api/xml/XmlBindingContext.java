package dev.sixik.unigui.api.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Реестр именованных значений для будущего разрешения XML-binding выражений. */
public final class XmlBindingContext {
    private final Map<String, XmlObservableValue<?>> values = new LinkedHashMap<>();

    public static XmlBindingContext empty() {
        return new XmlBindingContext();
    }

    public <T> XmlMutableObservableValue<T> mutable(String path, Class<T> valueType, T value) {
        XmlMutableObservableValue<T> observable = XmlMutableObservableValue.of(path, valueType, value);
        value(path, observable);
        return observable;
    }

    public XmlBindingContext value(String path, XmlObservableValue<?> value) {
        if (value == null) throw new IllegalArgumentException("XML binding value must not be null");
        values.put(requirePath(path), value);
        return this;
    }

    public Optional<XmlObservableValue<?>> value(String path) {
        return Optional.ofNullable(values.get(normalizePath(path)));
    }

    public <T> Optional<XmlObservableValue<T>> resolve(String path, Class<T> expectedType) {
        if (!status(path, expectedType).active()) return Optional.empty();
        @SuppressWarnings("unchecked")
        XmlObservableValue<T> observable = (XmlObservableValue<T>) values.get(normalizePath(path));
        return Optional.of(observable);
    }

    public XmlBindingStatus status(String path, Class<?> expectedType) {
        if (expectedType == null) throw new IllegalArgumentException("XML binding expected type must not be null");
        String normalized = normalizePath(path);
        XmlObservableValue<?> value = values.get(normalized);
        if (value == null) return XmlBindingStatus.missing(normalized, expectedType);
        if (!expectedType.isAssignableFrom(value.valueType())) {
            return XmlBindingStatus.typeMismatch(normalized, expectedType, value.valueType());
        }
        return XmlBindingStatus.active(normalized, expectedType, value.valueType());
    }

    public <T> XmlBinding<T> bind(String path, Class<T> targetType, Consumer<? super T> target) {
        if (targetType == null) throw new IllegalArgumentException("XML binding target type must not be null");
        if (target == null) throw new IllegalArgumentException("XML binding target must not be null");
        String normalized = normalizePath(path);
        XmlObservableValue<?> value = values.get(normalized);
        if (value == null) return XmlBinding.inactive(normalized, targetType, XmlBindingStatus.missing(normalized, targetType));
        return XmlBinding.bind(value, targetType, target, normalized);
    }

    public List<String> paths() {
        return List.copyOf(values.keySet());
    }

    public List<XmlBindingStatus> statuses() {
        return values.entrySet().stream()
                .map(entry -> XmlBindingStatus.active(entry.getKey(), entry.getValue().valueType()))
                .toList();
    }

    private static String requirePath(String path) {
        String normalized = normalizePath(path);
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML binding path must not be blank");
        return normalized;
    }

    private static String normalizePath(String path) {
        return XmlBindingStatus.normalizePath(path);
    }
}
