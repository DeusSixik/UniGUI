package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;

import java.util.Objects;

public final class DockLayoutRestoredEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<DockLayoutRestoredEvent> TYPE = EventType.create("dock.layout.restored");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final DockLayoutSnapshot snapshot;
    private final int restoredPaneCount;
    private final int missingPaneCount;

    public DockLayoutRestoredEvent(Widget target, DockLayoutSnapshot snapshot,
                                   int restoredPaneCount, int missingPaneCount) {
        this(target, target, EventPhase.TARGET, snapshot, restoredPaneCount, missingPaneCount);
    }

    public DockLayoutRestoredEvent(Widget target, Widget currentTarget, EventPhase phase,
                                   DockLayoutSnapshot snapshot, int restoredPaneCount, int missingPaneCount) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.snapshot = snapshot == null ? new DockLayoutSnapshot(null, "") : snapshot;
        this.restoredPaneCount = Math.max(0, restoredPaneCount);
        this.missingPaneCount = Math.max(0, missingPaneCount);
    }

    @Override
    public EventType<DockLayoutRestoredEvent> type() {
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

    public DockLayoutSnapshot snapshot() {
        return snapshot;
    }

    public int restoredPaneCount() {
        return restoredPaneCount;
    }

    public int missingPaneCount() {
        return missingPaneCount;
    }

    @Override
    public DockLayoutRestoredEvent routeTo(Widget currentTarget, EventPhase phase) {
        DockLayoutRestoredEvent event = new DockLayoutRestoredEvent(
                target, currentTarget, phase, snapshot, restoredPaneCount, missingPaneCount);
        if (isCancelled()) event.cancel();
        return event;
    }
}
