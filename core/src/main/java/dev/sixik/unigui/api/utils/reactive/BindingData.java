package dev.sixik.unigui.api.utils.reactive;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.Objects;

/**
 * Identity key describing a signal type that a widget can bind to one of its properties.
 */
@AiDoc(
        summary = "Identity key that names one widget binding and its required source type.",
        purpose = "Lets WidgetBase subclasses add property bindings through the shared bind(key, signal) API.",
        criticalRules = {
                "BindingData keys are matched by object identity; declare a static final key and reuse that instance.",
                "The key's Class must match the source type expected by the binding implementation."
        },
        examples = @AiDoc.Example(
                title = "Declare a custom binding key",
                order = 10,
                code = "static final BindingData<BooleanSignal> MY_CHECKBOX_VALUE =\n"
                        + "        new BindingData<>(BooleanSignal.class);"),
        related = {"BindingKeys", "SignalSubscription", "WidgetBase"})
public final class BindingData<T> {
    private final Class<T> type;

    public BindingData(Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public Class<T> type() {
        return type;
    }

    public T cast(Object value) {
        return type.cast(value);
    }
}
