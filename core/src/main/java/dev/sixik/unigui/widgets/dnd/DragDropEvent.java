package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Отпускание payload над drop-зоной.
 *
 * <p>Событие доставляется напрямую зоне, которая последней приняла {@link DragOverEvent}.
 * К этому моменту указатель уже проверен в финальной точке отпускания.</p>
 */
@AiDoc(
        description = "Routed event delivered to the target drop zone when the pointer is released with an accepted payload."
)
@WikiPage(
        title = "DragDropEvent",
        slug = "drag-drop-event",
        category = "<key:dnd.category>",
        summary = "<key:drag.drop.event.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.drop.event.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DropController",
        target = "page:drop-controller",
        description = "<key:drop.controller.summary>"
)
@WikiLink(
        label = "DragSession",
        target = "page:drag-session",
        description = "<key:drag.session.summary>"
)
public final class DragDropEvent extends DragRoutedEvent {
    public static final EventType<DragDropEvent> TYPE = EventType.create("dnd.drop");

    public DragDropEvent(Widget target, DragSession session) {
        this(target, target, EventPhase.TARGET, session);
    }

    public DragDropEvent(Widget target, Widget currentTarget, EventPhase phase, DragSession session) {
        super(TYPE, target, currentTarget, phase, session);
    }

    @Override
    public DragDropEvent routeTo(Widget currentTarget, EventPhase phase) {
        return inheritCancel(new DragDropEvent(target(), currentTarget, phase, session()));
    }
}
