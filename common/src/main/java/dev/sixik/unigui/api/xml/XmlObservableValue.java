package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.event.EventSubscription;

/** Типизированное observable-значение для XML-binding прототипов до появления полноценного binding-синтаксиса. */
public interface XmlObservableValue<T> {
    String name();

    Class<T> valueType();

    T get();

    EventSubscription onChanged(XmlBindingListener<T> listener);

    default XmlBindingStatus status() {
        return XmlBindingStatus.active(name(), valueType());
    }
}
