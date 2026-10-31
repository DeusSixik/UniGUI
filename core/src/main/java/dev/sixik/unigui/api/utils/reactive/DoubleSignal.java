package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DoubleSignal {

    public interface Subscriber {
        void onValueChanged(double signal);
    }

    protected double value;
    protected boolean notifying;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();

    public DoubleSignal() {
        this(0D);
    }

    public DoubleSignal(double initialValue) {
        this.value = initialValue;
    }

    public double get() {
        return value;
    }

    public void set(double newValue) {
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
