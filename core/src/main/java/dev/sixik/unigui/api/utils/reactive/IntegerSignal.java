package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class IntegerSignal {

    public interface Subscriber {
        void onValueChanged(int signal);
    }

    protected int value;
    protected boolean notifying;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();

    public IntegerSignal() {
        this(0);
    }

    public IntegerSignal(int initialValue) {
        this.value = initialValue;
    }

    public int get() {
        return value;
    }

    public void set(int newValue) {
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
