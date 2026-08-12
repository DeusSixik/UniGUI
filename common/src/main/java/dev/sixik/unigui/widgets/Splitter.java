package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.SplitterRenderer;
import dev.sixik.unigui.widgets.render.SplitterState;

public final class Splitter extends Box {
    private final SplitPanel owner;
    private final MutableColor handleColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.55f);
    private SplitterRenderer renderer;
    private boolean dragging;

    Splitter(SplitPanel owner) {
        this.owner = owner;
        backgroundVisible(true);
        borderVisible(false);
        background().set(0.09f, 0.10f, 0.12f, 0.95f);
        radius(2.0f);
        handleColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public boolean dragging() {
        return dragging;
    }

    public MutableColor handleColor() {
        return handleColor;
    }

    public SplitterRenderer renderer() {
        return renderer;
    }

    public Splitter renderer(SplitterRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Splitter useDefaultRenderer() {
        return renderer(null);
    }

    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        return owner.orientation() == Orientation.HORIZONTAL
                ? MouseCursor.RESIZE_HORIZONTAL
                : MouseCursor.RESIZE_VERTICAL;
    }

    @Override
    public void handle(Event event) {
        if (visibility() != dev.sixik.unigui.api.widget.Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            dragging = true;
            UIContext context = uiContext();
            if (context != null) {
                context.focusManager().requestFocus(owner);
                context.capturePointer(pointer.pointerId(), this);
            }
            owner.beginSplitterDrag(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer && dragging) {
            owner.dragSplitterTo(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && dragging) {
            owner.dragSplitterTo(pointer.rootX(), pointer.rootY());
            dragging = false;
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            event.cancel();
        }
    }

    void cancelDrag() {
        dragging = false;
    }

    @Override
    protected void renderContent(RenderContext context) {
        super.renderContent(context);
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot());
    }

    protected SplitterRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(SplitterRenderer.class, WidgetsRender.splitter()) : renderer;
    }

    protected SplitterState snapshot() {
        return new SplitterState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                owner.orientation(),
                dragging,
                handleColor.copy());
    }
}
