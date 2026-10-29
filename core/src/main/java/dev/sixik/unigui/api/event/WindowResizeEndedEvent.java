package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowResizeEndedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowResizeEndedEvent> TYPE = EventType.create("window.resize_ended");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final String handle;

    public WindowResizeEndedEvent(Widget target, float x, float y, float width, float height, String handle) {
        this(target, target, EventPhase.TARGET, x, y, width, height, handle);
    }

    public WindowResizeEndedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                  float x, float y, float width, float height, String handle) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.handle = handle == null ? "" : handle;
    }

    @Override
    public EventType<WindowResizeEndedEvent> type() {
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

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public String handle() {
        return handle;
    }

    @Override
    public WindowResizeEndedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowResizeEndedEvent event = new WindowResizeEndedEvent(target, currentTarget, phase, x, y, width, height, handle);
        if (isCancelled()) event.cancel();
        return event;
    }
}
