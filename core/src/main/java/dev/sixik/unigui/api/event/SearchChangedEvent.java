package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class SearchChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<SearchChangedEvent> TYPE = EventType.create("search.changed");

    private final Widget target;
    private final String oldQuery;
    private final String newQuery;

    public SearchChangedEvent(Widget target, String oldQuery, String newQuery) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldQuery = oldQuery == null ? "" : oldQuery;
        this.newQuery = newQuery == null ? "" : newQuery;
    }

    @Override
    public EventType<SearchChangedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return target;
    }

    @Override
    public EventPhase phase() {
        return EventPhase.TARGET;
    }

    public String oldQuery() {
        return oldQuery;
    }

    public String newQuery() {
        return newQuery;
    }
}
