package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharSignal {

    public interface Subscriber {
        void onValueChanged(char signal);
    }

    protected char value;
    protected boolean notifying;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();

    public CharSignal() {
        this((char) 0);
    }

    public CharSignal(char initialValue) {
        this.value = initialValue;
    }

    public char get() {
        return value;
    }

    public void set(char newValue) {
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
