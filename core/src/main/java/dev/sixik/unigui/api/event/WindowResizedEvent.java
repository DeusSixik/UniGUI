package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowResizedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowResizedEvent> TYPE = EventType.create("window.resized");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float oldX;
    private final float oldY;
    private final float oldWidth;
    private final float oldHeight;
    private final float newX;
    private final float newY;
    private final float newWidth;
    private final float newHeight;
    private final String handle;

    public WindowResizedEvent(Widget target,
                              float oldX, float oldY, float oldWidth, float oldHeight,
                              float newX, float newY, float newWidth, float newHeight,
                              String handle) {
        this(target, target, EventPhase.TARGET,
                oldX, oldY, oldWidth, oldHeight, newX, newY, newWidth, newHeight, handle);
    }

    public WindowResizedEvent(Widget target, Widget currentTarget, EventPhase phase,
                              float oldX, float oldY, float oldWidth, float oldHeight,
                              float newX, float newY, float newWidth, float newHeight,
                              String handle) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.oldX = oldX;
        this.oldY = oldY;
        this.oldWidth = oldWidth;
        this.oldHeight = oldHeight;
        this.newX = newX;
        this.newY = newY;
        this.newWidth = newWidth;
        this.newHeight = newHeight;
        this.handle = handle == null ? "" : handle;
    }

    @Override
    public EventType<WindowResizedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return currentTarget;
    }

    @Override
    public EventPhase phase() {
        return phase;
    }

    public float oldX() {
        return oldX;
    }

    public float oldY() {
        return oldY;
    }

    public float oldWidth() {
        return oldWidth;
    }

    public float oldHeight() {
        return oldHeight;
    }

    public float newX() {
        return newX;
    }

    public float newY() {
        return newY;
    }

    public float newWidth() {
        return newWidth;
    }

    public float newHeight() {
        return newHeight;
    }

    public String handle() {
        return handle;
    }

    @Override
    public WindowResizedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowResizedEvent event = new WindowResizedEvent(target, currentTarget, phase,
                oldX, oldY, oldWidth, oldHeight, newX, newY, newWidth, newHeight, handle);
        if (isCancelled()) event.cancel();
        return event;
    }
}
