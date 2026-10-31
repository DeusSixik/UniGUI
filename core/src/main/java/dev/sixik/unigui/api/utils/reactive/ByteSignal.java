package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ByteSignal {

    public interface Subscriber {
        void onValueChanged(byte signal);
    }

    protected byte value;
    protected boolean notifying;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();

    public ByteSignal() {
        this((byte) 0);
    }

    public ByteSignal(byte initialValue) {
        this.value = initialValue;
    }

    public byte get() {
        return value;
    }

    public void set(byte newValue) {
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
