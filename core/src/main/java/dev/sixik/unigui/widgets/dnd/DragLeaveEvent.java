package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Указатель покинул drop-зону или сессия отменена, пока зона была активной целью.
 */
@AiDoc(
        description = "Routed event notified when the drag pointer exits a drop zone or when the gesture is aborted."
)
@WikiPage(
        title = "DragLeaveEvent",
        slug = "drag-leave-event",
        category = "<key:dnd.category>",
        summary = "<key:drag.leave.event.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.leave.event.section.overview.body>",
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
public final class DragLeaveEvent extends DragRoutedEvent {
    public static final EventType<DragLeaveEvent> TYPE = EventType.create("dnd.leave");

    public DragLeaveEvent(Widget target, DragSession session) {
        this(target, target, EventPhase.TARGET, session);
    }

    public DragLeaveEvent(Widget target, Widget currentTarget, EventPhase phase, DragSession session) {
        super(TYPE, target, currentTarget, phase, session);
    }

    @Override
    public DragLeaveEvent routeTo(Widget currentTarget, EventPhase phase) {
        return inheritCancel(new DragLeaveEvent(target(), currentTarget, phase, session()));
    }
}
