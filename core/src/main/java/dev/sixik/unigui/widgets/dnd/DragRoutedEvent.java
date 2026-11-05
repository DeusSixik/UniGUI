package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.BaseEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.event.RoutableWidgetEvent;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Базовый routed-event drag-and-drop протокола.
 *
 * <p>Все события несут {@link DragSession}: payload, позицию указателя и источник читаются из неё,
 * поэтому копии события при маршрутизации (capture/target/bubble) остаются лёгкими.</p>
 */
@AiDoc(summary = "Abstract base class for routed drag-and-drop events conveying the active DragSession across widget hierarchies."
)
@WikiPage(
        title = "DragRoutedEvent",
        slug = "drag-routed-event",
        category = "<key:dnd.category>",
        summary = "<key:drag.routed.event.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.routed.event.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DragSession",
        target = "page:drag-session",
        description = "<key:drag.session.summary>"
)
public abstract class DragRoutedEvent extends BaseEvent implements RoutableWidgetEvent {
    private final EventType<? extends Event> type;
    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final DragSession session;

    protected DragRoutedEvent(EventType<? extends Event> type, Widget target, Widget currentTarget,
                              EventPhase phase, DragSession session) {
        this.type = Objects.requireNonNull(type, "type");
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    @AiDoc(summary = "Returns the event type token.")
    @WikiMethod(summary = "<key:drag.routed.event.method.type>")
    @WikiReturn("<key:drag.routed.event.return.type>")
    public EventType<? extends Event> type() {
        return type;
    }

    @Override
    @AiDoc(summary = "Returns the original target widget where the event was initiated.")
    @WikiMethod(summary = "<key:drag.routed.event.method.target>")
    @WikiReturn("<key:drag.routed.event.return.target>")
    public Widget target() {
        return target;
    }

    @Override
    @AiDoc(summary = "Returns the current widget handling the event during dispatch.")
    @WikiMethod(summary = "<key:drag.routed.event.method.currentTarget>")
    @WikiReturn("<key:drag.routed.event.return.currentTarget>")
    public Widget currentTarget() {
        return currentTarget;
    }

    @Override
    @AiDoc(summary = "Returns the current routing phase (capture, target, bubble).")
    @WikiMethod(summary = "<key:drag.routed.event.method.phase>")
    @WikiReturn("<key:drag.routed.event.return.phase>")
    public EventPhase phase() {
        return phase;
    }

    /**
     * @return активная drag-сессия
     */
    @AiDoc(summary = "Returns the active DragSession associated with this event.")
    @WikiMethod(summary = "<key:drag.routed.event.method.session>")
    @WikiReturn("<key:drag.routed.event.return.session>")
    public DragSession session() {
        return session;
    }

    /**
     * @return payload перетаскиваемого объекта
     */
    @AiDoc(summary = "Convenience shortcut returning the payload from the active drag session.")
    @WikiMethod(summary = "<key:drag.routed.event.method.payload>")
    @WikiReturn("<key:drag.routed.event.return.payload>")
    public DragPayload payload() {
        return session.payload();
    }

    /**
     * @return X указателя в координатах корня
     */
    @AiDoc(summary = "Returns the pointer X coordinate in root UI space.")
    @WikiMethod(summary = "<key:drag.routed.event.method.rootX>")
    @WikiReturn("<key:drag.routed.event.return.rootX>")
    public float rootX() {
        return session.pointerX();
    }

    /**
     * @return Y указателя в координатах корня
     */
    @AiDoc(summary = "Returns the pointer Y coordinate in root UI space.")
    @WikiMethod(summary = "<key:drag.routed.event.method.rootY>")
    @WikiReturn("<key:drag.routed.event.return.rootY>")
    public float rootY() {
        return session.pointerY();
    }

    /**
     * Копирует состояние отмены в маршрутизированный экземпляр.
     */
    protected <T extends DragRoutedEvent> T inheritCancel(T routed) {
        if (isCancelled()) routed.cancel();
        return routed;
    }
}
