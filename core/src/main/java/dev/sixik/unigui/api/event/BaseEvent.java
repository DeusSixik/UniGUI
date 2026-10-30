package dev.sixik.unigui.api.event;

public abstract class BaseEvent implements Event {
    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
