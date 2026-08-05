package dev.sixik.unigui.impl.widget;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.event.FastEventEmitter;

import java.util.Collections;
import java.util.List;

public abstract class WidgetBase implements Widget {
    private final FastEventEmitter events = new FastEventEmitter();
    private final MutableRect layoutBounds = new MutableRect();
    private final Transform transform = new Transform();
    private UIContext uiContext;
    private Widget parent;
    private int invalidationFlags = InvalidationFlags.ALL;
    private int subtreeInvalidationFlags = InvalidationFlags.ALL;

    protected WidgetBase() {
        layoutBounds.onChanged(() -> invalidate(InvalidationFlags.LAYOUT));
        transform.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    @Override
    public UIContext uiContext() {
        return uiContext;
    }

    public void setUiContextInternal(UIContext uiContext) {
        this.uiContext = uiContext;
    }

    @Override
    public Widget parent() {
        return parent;
    }

    public void setParentInternal(Widget parent) {
        if (this.parent == parent) return;
        Widget oldParent = this.parent;
        this.parent = parent;

        recomputeSubtreeInvalidation(oldParent);
        recomputeSubtreeInvalidation(parent);
    }

    @Override
    public List<Widget> children() {
        return Collections.emptyList();
    }

    @Override
    public RectView layoutBounds() {
        return layoutBounds;
    }

    protected MutableRect mutableLayoutBounds() {
        return layoutBounds;
    }

    @Override
    public Transform transform() {
        return transform;
    }

    @Override
    public int invalidationFlags() {
        return invalidationFlags;
    }

    @Override
    public int subtreeInvalidationFlags() {
        return subtreeInvalidationFlags;
    }

    @Override
    public void invalidate(int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags |= flags;
        markSubtreeInvalidation(flags);
    }

    @Override
    public void clearInvalidation(int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags &= ~flags;
        recomputeSubtreeInvalidation();
    }

    protected void recomputeSubtreeInvalidation() {
        int flags = invalidationFlags;
        for (Widget child : children()) {
            flags |= child.subtreeInvalidationFlags();
        }

        if (subtreeInvalidationFlags == flags) {
            return;
        }

        subtreeInvalidationFlags = flags;
        recomputeSubtreeInvalidation(parent);
    }

    private void markSubtreeInvalidation(int flags) {
        int previous = subtreeInvalidationFlags;
        subtreeInvalidationFlags |= flags;
        if (previous != subtreeInvalidationFlags) {
            if (parent instanceof WidgetBase base) {
                base.markSubtreeInvalidation(flags);
            } else if (parent != null) {
                parent.invalidate(flags);
            }
        }
    }

    private static void recomputeSubtreeInvalidation(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.recomputeSubtreeInvalidation();
        } else if (widget != null) {
            widget.invalidate(InvalidationFlags.ALL);
        }
    }

    @Override
    public void measure(LayoutContext context) {
    }

    @Override
    public void arrange(RectView bounds) {
        layoutBounds.set(bounds);
    }

    @Override
    public void render(RenderContext context) {
    }

    @Override
    public void handle(Event event) {
        emit(event);
    }

    @Override
    public <T extends Event> EventSubscription on(EventType<T> type, EventListener<? super T> listener) {
        return events.on(type, listener);
    }

    @Override
    public void emit(Event event) {
        events.emit(event);
    }
}
