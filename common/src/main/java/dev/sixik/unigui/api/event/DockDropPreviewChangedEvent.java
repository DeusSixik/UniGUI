package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.DockDropIntent;

import java.util.Objects;

public final class DockDropPreviewChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<DockDropPreviewChangedEvent> TYPE = EventType.create("dock.drop_preview_changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String paneId;
    private final DockDropIntent oldIntent;
    private final DockDropIntent newIntent;

    public DockDropPreviewChangedEvent(Widget target, String paneId,
                                       DockDropIntent oldIntent, DockDropIntent newIntent) {
        this(target, target, EventPhase.TARGET, paneId, oldIntent, newIntent);
    }

    public DockDropPreviewChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                       String paneId, DockDropIntent oldIntent, DockDropIntent newIntent) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.paneId = paneId == null ? "" : paneId;
        this.oldIntent = oldIntent == null ? DockDropIntent.none() : oldIntent;
        this.newIntent = newIntent == null ? DockDropIntent.none() : newIntent;
    }

    @Override
    public EventType<DockDropPreviewChangedEvent> type() {
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

    public String paneId() {
        return paneId;
    }

    public DockDropIntent oldIntent() {
        return oldIntent;
    }

    public DockDropIntent newIntent() {
        return newIntent;
    }

    @Override
    public DockDropPreviewChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        DockDropPreviewChangedEvent event = new DockDropPreviewChangedEvent(
                target, currentTarget, phase, paneId, oldIntent, newIntent);
        if (isCancelled()) event.cancel();
        return event;
    }
}
