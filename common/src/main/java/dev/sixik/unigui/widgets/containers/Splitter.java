package dev.sixik.unigui.widgets.containers;

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
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.widgets.core.Orientation;

/**
 * Интерактивный разделитель, встроенный в {@link SplitPanel}.
 *
 * <p>{@code Splitter} не предназначен для самостоятельного добавления в UI: его
 * создаёт владелец {@link SplitPanel}. Виджет отвечает за cursor feedback,
 * pointer capture и drag-события, а фактическое изменение layout'а делегирует
 * владельцу.</p>
 *
 * @see SplitPanel
 */
public final class Splitter extends Box {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SPLITTER;

    private final SplitPanel owner;
    private final MutableColor handleColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.55f);
    private boolean dragging;

    Splitter(SplitPanel owner) {
        this.owner = owner;
        boxVisualEnabled(false);
        backgroundVisible(true);
        borderVisible(false);
        background().set(0.09f, 0.10f, 0.12f, 0.95f);
        radius(2.0f);
        handleColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает, находится ли splitter в процессе drag resize.
     *
     * @return {@code true}, пока primary pointer удерживает разделитель
     */
    public boolean dragging() {
        return dragging;
    }

    /**
     * Возвращает live-цвет handle'а разделителя.
     *
     * @return изменяемый цвет центрального handle'а
     */
    public MutableColor handleColor() {
        return handleColor;
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
        applyTheme();
        super.renderContent(context);
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderCustomVisual(draw)) {
            return;
        }
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (backgroundVisible()) {
            draw.roundedRect(x, y, width, height, radius(),
                    Paint.fill(background()));
        }
        if (borderVisible() && borderWidth() > 0.0f) {
            draw.roundedRect(x, y, width, height, radius(),
                    Paint.stroke(borderColor(), borderWidth()));
        }
        if (owner.orientation() == Orientation.HORIZONTAL) {
            float handleWidth = Math.max(1.0f, Math.min(2.0f, width));
            draw.roundedRect(
                    x + (width - handleWidth) * 0.5f,
                    y + 3.0f,
                    handleWidth,
                    Math.max(1.0f, height - 6.0f),
                    handleWidth * 0.5f,
                    Paint.fill(handleColor));
        } else {
            float handleHeight = Math.max(1.0f, Math.min(2.0f, height));
            draw.roundedRect(
                    x + 3.0f,
                    y + (height - handleHeight) * 0.5f,
                    Math.max(1.0f, width - 6.0f),
                    handleHeight,
                    handleHeight * 0.5f,
                    Paint.fill(handleColor));
        }
    }
}
