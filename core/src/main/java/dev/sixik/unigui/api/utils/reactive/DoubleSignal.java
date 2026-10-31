package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Objects;

public class DoubleSignal {

    public interface Subscriber {
        void onValueChanged(double signal);
    }

    protected double value;
    protected boolean notifying;
    protected boolean notificationPending;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();
    private Object[] subscriberSnapshot = new Object[0];

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
        if (this.value == newValue) return;
        this.value = newValue;
        if (notifying) {
            notificationPending = true;
            return;
        }
        notifying = true;
        try {
            do {
                notificationPending = false;
                double currentValue = value;
                Object[] elements = subscriberSnapshot;
                for (Object element : elements) ((Subscriber) element).onValueChanged(currentValue);
            } while (notificationPending);
        } finally {
            notifying = false;
        }
    }

    /**
     * Registers a listener and immediately sends it the current value.
     */
    public SignalSubscription subscribe(Subscriber subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        subscribers.add(subscriber);
        refreshSubscriberSnapshot();
        subscriber.onValueChanged(this.value);
        return new SignalSubscription() {
            private boolean active = true;

            @Override
            public void unsubscribe() {
                if (!active) return;
                active = false;
                for (int i = 0; i < subscribers.size(); i++) {
                    if (subscribers.get(i) == subscriber) {
                        subscribers.remove(i);
                        refreshSubscriberSnapshot();
                        break;
                    }
                }
            }
        };
    }

    private void refreshSubscriberSnapshot() {
        subscriberSnapshot = subscribers.toArray();
    }
}
