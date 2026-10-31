package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Objects;

public class StringSignal {

    public interface Subscriber {
        void onValueChanged(String signal);
    }

    protected String value;
    protected boolean notifying;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();

    public StringSignal() {
        this("");
    }

    public StringSignal(String initialValue) {
        this.value = initialValue;
    }

    public String get() {
        return value;
    }

    public void set(String newValue) {
        if (Objects.equals(this.value, newValue) || notifying) return;
        this.value = newValue;
        notifying = true;
        try {
            Object[] elements = subscribers.elements();
            for (int i = 0, size = subscribers.size(); i < size; i++) {
                ((Subscriber) elements[i]).onValueChanged(newValue);
            }
        } finally {
            notifying = false;
        }
    }

    public void subscribe(Subscriber subscriber) {
        subscribers.add(subscriber);
        subscriber.onValueChanged(this.value);
    }
}
