package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Регион (область с комментарием) графа был добавлен, удалён, перемещён или изменён по размеру.
 */
public final class NodeGraphRegionChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphRegionChangedEvent> TYPE = EventType.create("node_graph.region_changed");

    /**
     * Что произошло с регионом.
     */
    public enum Kind {
        ADDED,
        REMOVED,
        MOVED,
        RESIZED
    }

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String regionId;
    private final Kind kind;
    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public NodeGraphRegionChangedEvent(Widget target, String regionId, Kind kind,
                                       float x, float y, float width, float height) {
        this(target, target, EventPhase.TARGET, regionId, kind, x, y, width, height);
    }

    public NodeGraphRegionChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                       String regionId, Kind kind, float x, float y, float width, float height) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.regionId = regionId == null ? "" : regionId;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public EventType<NodeGraphRegionChangedEvent> type() {
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

    public String regionId() {
        return regionId;
    }

    public Kind kind() {
        return kind;
    }

    /**
     * @return X региона в world-координатах после изменения
     */
    public float x() {
        return x;
    }

    /**
     * @return Y региона в world-координатах после изменения
     */
    public float y() {
        return y;
    }

    /**
     * @return ширина региона после изменения
     */
    public float width() {
        return width;
    }

    /**
     * @return высота региона после изменения
     */
    public float height() {
        return height;
    }

    @Override
    public NodeGraphRegionChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphRegionChangedEvent event = new NodeGraphRegionChangedEvent(target, currentTarget, phase,
                regionId, kind, x, y, width, height);
        if (isCancelled()) event.cancel();
        return event;
    }
}
