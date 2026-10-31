package dev.sixik.unigui.api.utils.reactive;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BooleanSignal {

    public interface Subscriber {
        void onValueChanged(boolean signal);
    }

    protected boolean value;
    protected boolean notifying;
    protected boolean notificationPending;
    protected final ObjectArrayList<Subscriber> subscribers = new ObjectArrayList<>();
    private Object[] subscriberSnapshot = new Object[0];

    public BooleanSignal() {

    }

    public BooleanSignal(boolean initialValue) {
        this.value = initialValue;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean newValue) {
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
                boolean currentValue = value;
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
        java.util.Objects.requireNonNull(subscriber, "subscriber");
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
