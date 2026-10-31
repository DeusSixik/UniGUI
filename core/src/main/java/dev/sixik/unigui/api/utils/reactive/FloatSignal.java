package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FloatSignal {

    public interface Subscriber {
        void onValueChanged(float signal);
    }

    protected float value;
    protected boolean notifying;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();

    public FloatSignal() {
        this(0F);
    }

    public FloatSignal(float initialValue) {
        this.value = initialValue;
    }

    public float get() {
        return value;
    }

    public void set(float newValue) {
        if (this.value == newValue || notifying) return;
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
