package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class SearchSubmittedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<SearchSubmittedEvent> TYPE = EventType.create("search.submitted");

    private final Widget target;
    private final String query;

    public SearchSubmittedEvent(Widget target, String query) {
        this.target = Objects.requireNonNull(target, "target");
        this.query = query == null ? "" : query;
    }

    @Override
    public EventType<SearchSubmittedEvent> type() {
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

    public String query() {
        return query;
    }
}
