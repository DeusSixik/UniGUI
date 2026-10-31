package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Зондирование drop-зоны: отправляется виджету под указателем на каждое движение и каждый тик
 * drag-сессии и всплывает вверх по дереву.
 *
 * <p>Drop-зона, готовая принять payload, вызывает {@link #accept(Widget)}. Это отмечает её
 * активной целью сессии и останавливает всплытие, поэтому побеждает ближайшая к указателю зона.
 * Зона, которая payload не принимает, ничего не делает, и событие уходит к родителям.</p>
 */
@AiDoc(summary = "Routed event probing candidate drop zones beneath the pointer during drag movement and animation ticks."
)
@WikiPage(
        title = "DragOverEvent",
        slug = "drag-over-event",
        category = "<key:dnd.category>",
        summary = "<key:drag.over.event.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.over.event.section.overview.body>",
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
public final class DragOverEvent extends DragRoutedEvent {
    public static final EventType<DragOverEvent> TYPE = EventType.create("dnd.over");

    public DragOverEvent(Widget target, DragSession session) {
        this(target, target, EventPhase.TARGET, session);
    }

    public DragOverEvent(Widget target, Widget currentTarget, EventPhase phase, DragSession session) {
        super(TYPE, target, currentTarget, phase, session);
    }

    /**
     * Объявляет зону активной целью drop и останавливает всплытие.
     *
     * @param zone виджет-drop-зона, который получит {@link DragDropEvent} при отпускании
     */
    @AiDoc(summary = "Designates the specified widget as the active drop target and stops further event bubbling.")
    @WikiMethod(summary = "<key:drag.over.event.method.accept>")
    public void accept(
            @WikiParameter(description = "<key:drag.over.event.param.zone>")
            Widget zone
    ) {
        session().acceptDropTarget(zone);
        cancel();
    }

    @Override
    public DragOverEvent routeTo(Widget currentTarget, EventPhase phase) {
        return inheritCancel(new DragOverEvent(target(), currentTarget, phase, session()));
    }
}
