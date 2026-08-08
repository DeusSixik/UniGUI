package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollBarValueChangedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;
import dev.sixik.unigui.widgets.render.ScrollBarState;

public class ScrollBar extends Box {
    public static final float DEFAULT_SIZE = 6.0f;
    public static final float DEFAULT_GAP = 8.0f;

    private final MutableColor trackColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.28f);
    private final MutableColor thumbColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.75f);
    private Orientation orientation = Orientation.VERTICAL;
    private ScrollBarRenderer renderer;
    private float min;
    private float max;
    private float value;
    private float pageSize = 1.0f;
    private float step = 16.0f;
    private boolean dragging;

    public ScrollBar() {
        backgroundVisible(false);
        borderVisible(false);
        focusable(true);
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        thumbColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Orientation orientation() {
        return orientation;
    }

    public ScrollBar orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.VERTICAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public ScrollBar range(float min, float max) {
        if (max < min) {
            float swap = min;
            min = max;
            max = swap;
        }
        if (this.min == min && this.max == max) return this;
        this.min = min;
        this.max = max;
        silentValue(value);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float value() {
        return value;
    }

    public ScrollBar value(float value) {
        setValue(value, true);
        return this;
    }

    public ScrollBar silentValue(float value) {
        setValue(value, false);
        return this;
    }

    public float pageSize() {
        return pageSize;
    }

    public ScrollBar pageSize(float pageSize) {
        float normalized = Math.max(1.0f, pageSize);
        if (this.pageSize == normalized) return this;
        this.pageSize = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float step() {
        return step;
    }

    public ScrollBar step(float step) {
        this.step = Math.max(1.0f, step);
        return this;
    }

    public boolean dragging() {
        return dragging;
    }

    public MutableColor trackColor() {
        return trackColor;
    }

    public MutableColor thumbColor() {
        return thumbColor;
    }

    public ScrollBarRenderer renderer() {
        return renderer;
    }

    public ScrollBar renderer(ScrollBarRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public ScrollBar useDefaultRenderer() {
        return renderer(null);
    }

    public EventSubscription onValueChanged(EventListener<? super ScrollBarValueChangedEvent> listener) {
        return on(ScrollBarValueChangedEvent.TYPE, listener);
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        trackColor.set(styleValue(StyleKeys.TRACK_COLOR, trackColor));
        thumbColor.set(styleValue(StyleKeys.THUMB_COLOR, thumbColor));
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            UIContext context = uiContext();
            if (context != null) {
                context.focusManager().requestFocus(this);
                context.capturePointer(pointer.pointerId(), this);
            }
            dragging = true;
            updateFromRoot(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer && dragging) {
            updateFromRoot(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && dragging) {
            updateFromRoot(pointer.rootX(), pointer.rootY());
            dragging = false;
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET && isFocused()) {
            if (key.keyCode() == KeyCodes.LEFT || key.keyCode() == KeyCodes.UP) {
                nudge(-1.0f);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.RIGHT || key.keyCode() == KeyCodes.DOWN) {
                nudge(1.0f);
                event.cancel();
            }
        }
    }

    @Override
    protected void renderContent(RenderContext context) {
        effectiveRenderer().render(new DrawScope(context, transform()), snapshot());
        super.renderContent(context);
    }

    private ScrollBarRenderer effectiveRenderer() {
        return renderer == null ? WidgetsRender.scrollBar() : renderer;
    }

    private ScrollBarState snapshot() {
        return new ScrollBarState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                orientation,
                min,
                max,
                value,
                pageSize,
                step,
                normalizedValue(),
                dragging,
                trackColor.copy(),
                thumbColor.copy());
    }

    private boolean isFocused() {
        UIContext context = uiContext();
        return context != null && context.focusManager().isFocused(this);
    }

    private void updateFromLocal(float localX, float localY) {
        float length = Math.max(1.0f, orientation == Orientation.VERTICAL ? layoutBounds().height() : layoutBounds().width());
        float thumbLength = thumbLength(length);
        float position = orientation == Orientation.VERTICAL ? localY : localX;
        float trackTravel = Math.max(1.0f, length - thumbLength);
        float normalized = clamp((position - thumbLength * 0.5f) / trackTravel, 0.0f, 1.0f);
        setValue(min + normalized * (max - min), true);
    }

    private void updateFromRoot(float rootX, float rootY) {
        updateFromLocal(rootX - layoutBounds().x(), rootY - layoutBounds().y());
    }

    private void nudge(float direction) {
        setValue(value + step * direction, true);
    }

    private float normalizedValue() {
        float range = max - min;
        if (range <= 0.0f) return 0.0f;
        return clamp((value - min) / range, 0.0f, 1.0f);
    }

    private float thumbLength(float trackLength) {
        float contentExtent = Math.max(pageSize, pageSize + Math.max(0.0f, max - min));
        return Math.max(8.0f, trackLength * (pageSize / contentExtent));
    }

    private void setValue(float value, boolean emitChange) {
        float normalized = clamp(value, min, max);
        if (this.value == normalized) return;
        float oldValue = this.value;
        this.value = normalized;
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new ScrollBarValueChangedEvent(this, oldValue, this.value));
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
