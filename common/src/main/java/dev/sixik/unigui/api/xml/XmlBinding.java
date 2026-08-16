package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.event.EventSubscription;

import java.util.function.Consumer;

/** Активная связь типизированного observable-значения с code-behind или состоянием редактора. */
public final class XmlBinding<T> implements AutoCloseable {
    private final String targetName;
    private final Class<T> targetType;
    private final EventSubscription subscription;
    private XmlBindingStatus status;
    private boolean closed;

    private XmlBinding(String targetName, Class<T> targetType, EventSubscription subscription, XmlBindingStatus status) {
        this.targetName = XmlBindingStatus.normalizePath(targetName);
        this.targetType = targetType;
        this.subscription = subscription;
        this.status = status;
        this.closed = status == null || !status.active();
    }

    public static <T> XmlBinding<T> bind(XmlObservableValue<?> source, Class<T> targetType, Consumer<? super T> target) {
        String targetName = source == null ? "" : source.name();
        return bind(source, targetType, target, targetName);
    }

    public static <T> XmlBinding<T> bind(XmlObservableValue<?> source, Class<T> targetType,
                                         Consumer<? super T> target, String targetName) {
        if (targetType == null) throw new IllegalArgumentException("XML binding target type must not be null");
        if (target == null) throw new IllegalArgumentException("XML binding target must not be null");

        String path = targetName == null || targetName.trim().isEmpty()
                ? source == null ? "" : source.name()
                : targetName.trim();
        if (source == null) return inactive(path, targetType, XmlBindingStatus.missing(path, targetType));
        if (!targetType.isAssignableFrom(source.valueType())) {
            return inactive(path, targetType, XmlBindingStatus.typeMismatch(path, targetType, source.valueType()));
        }

        @SuppressWarnings("unchecked")
        XmlObservableValue<T> typedSource = (XmlObservableValue<T>) source;
        target.accept(typedSource.get());
        EventSubscription subscription = typedSource.onChanged(change -> target.accept(change.newValue()));
        return new XmlBinding<>(path, targetType, subscription, XmlBindingStatus.active(path, targetType, source.valueType()));
    }

    static <T> XmlBinding<T> inactive(String targetName, Class<T> targetType, XmlBindingStatus status) {
        return new XmlBinding<>(targetName, targetType, null, status);
    }

    public XmlBindingStatus status() {
        if (closed && status.active()) return XmlBindingStatus.closed(targetName, targetType);
        return status;
    }

    public boolean active() {
        return status().active();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (subscription != null) subscription.unsubscribe();
        status = XmlBindingStatus.closed(targetName, targetType);
    }
}
