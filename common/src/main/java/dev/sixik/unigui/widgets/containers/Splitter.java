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
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.render.SplitterRenderer;
import dev.sixik.unigui.widgets.render.SplitterState;

/**
 * Интерактивный разделитель, встроенный в {@link SplitPanel}.
 *
 * <p>{@code Splitter} не предназначен для самостоятельного добавления в UI: его
 * создаёт владелец {@link SplitPanel}. Виджет отвечает за cursor feedback,
 * pointer capture и drag-события, а фактическое изменение layout'а делегирует
 * владельцу.</p>
 *
 * @see SplitPanel
 * @see SplitterRenderer
 */
public final class Splitter extends Box {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SPLITTER;

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

    /**
     * Возвращает renderer, заданный напрямую для splitter'а.
     *
     * @return кастомный renderer или {@code null}, если используется тема/default
     */
    public SplitterRenderer renderer() {
        return renderer;
    }

    /**
     * Задаёт renderer разделителя.
     *
     * @param renderer renderer splitter'а или {@code null} для theme/default renderer'а
     * @return этот splitter для fluent-настройки
     */
    public Splitter renderer(SplitterRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Сбрасывает кастомный renderer и возвращает renderer из темы/default.
     *
     * @return этот splitter для fluent-настройки
     */
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

    /**
     * Возвращает renderer, который будет использован на текущем render-проходе.
     *
     * @return локальный, theme или default renderer
     */
    protected SplitterRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(SplitterRenderer.class, WidgetsRender.splitter()) : renderer;
    }

    /**
     * Создаёт immutable snapshot visual/interaction-состояния splitter'а.
     *
     * @return состояние splitter'а на текущий кадр
     */
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
