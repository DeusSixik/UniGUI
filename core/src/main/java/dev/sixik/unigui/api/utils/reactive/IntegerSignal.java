package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Objects;

public class IntegerSignal {

    public interface Subscriber {
        void onValueChanged(int signal);
    }

    protected int value;
    protected boolean notifying;
    protected boolean notificationPending;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();
    private Object[] subscriberSnapshot = new Object[0];

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
                int currentValue = value;
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
