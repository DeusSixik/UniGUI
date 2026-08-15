package dev.sixik.unigui.widgets.interaction;

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
import dev.sixik.unigui.api.event.SliderValueChangedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.SliderRenderer;
import dev.sixik.unigui.widgets.render.SliderState;
import dev.sixik.unigui.widgets.containers.Box;

public class Slider extends Box {
    private static final float KNOB_WIDTH = 8.0f;

    private final MutableColor trackColor = new MutableColor(0.25f, 0.25f, 0.25f, 1.0f);
    private final MutableColor fillColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor knobColor = new MutableColor(0.95f, 0.95f, 0.95f, 1.0f);
    private SliderRenderer renderer;
    private float min;
    private float max = 1.0f;
    private float value;
    private float step;
    private boolean dragging;

    public Slider() {
        backgroundVisible(false);
        borderVisible(false);
        focusable(true);
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        fillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        knobColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public Slider range(float min, float max) {
        if (max < min) {
            float swap = min;
            min = max;
            max = swap;
        }
        if (this.min == min && this.max == max) return this;
        this.min = min;
        this.max = max;
        value(value);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float value() {
        return value;
    }

    public Slider value(float value) {
        setValue(value, true);
        return this;
    }

    public float step() {
        return step;
    }

    public Slider step(float step) {
        this.step = Math.max(0.0f, step);
        return this;
    }

    public boolean dragging() {
        return dragging;
    }

    public MutableColor trackColor() {
        return trackColor;
    }

    public MutableColor fillColor() {
        return fillColor;
    }

    public MutableColor knobColor() {
        return knobColor;
    }

    public SliderRenderer renderer() {
        return renderer;
    }

    public Slider renderer(SliderRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Slider useDefaultRenderer() {
        return renderer(null);
    }

    public EventSubscription onValueChanged(EventListener<? super SliderValueChangedEvent> listener) {
        return on(SliderValueChangedEvent.TYPE, listener);
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        trackColor.set(styleValue(StyleKeys.TRACK_COLOR, trackColor));
        fillColor.set(styleValue(StyleKeys.ACCENT_COLOR, fillColor));
        knobColor.set(styleValue(StyleKeys.THUMB_COLOR, knobColor));
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
            updateFromRootX(pointer.rootX());
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer && dragging) {
            updateFromRootX(pointer.rootX());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && dragging) {
            updateFromRootX(pointer.rootX());
            dragging = false;
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET && isFocused()) {
            if (key.keyCode() == KeyCodes.LEFT) {
                nudge(-1.0f);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.RIGHT) {
                nudge(1.0f);
                event.cancel();
            }
        }
    }

    @Override
    protected void renderContent(RenderContext context) {
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot());
        super.renderContent(context);
    }

    private SliderRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(SliderRenderer.class, WidgetsRender.slider()) : renderer;
    }

    private SliderState snapshot() {
        return new SliderState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                min,
                max,
                value,
                step,
                normalizedValue(),
                KNOB_WIDTH,
                dragging,
                trackColor.copy(),
                fillColor.copy(),
                knobColor.copy());
    }

    private boolean isFocused() {
        UIContext context = uiContext();
        return context != null && context.focusManager().isFocused(this);
    }

    private void updateFromLocalX(float localX) {
        float width = Math.max(1.0f, layoutBounds().width());
        float normalized = clamp(localX / width, 0.0f, 1.0f);
        setValue(min + normalized * (max - min), true);
    }

    private void updateFromRootX(float rootX) {
        updateFromLocalX(rootX - layoutBounds().x());
    }

    private void nudge(float direction) {
        float increment = step > 0.0f ? step : (max - min) / 20.0f;
        setValue(value + increment * direction, true);
    }

    private float normalizedValue() {
        float range = max - min;
        if (range == 0.0f) return 0.0f;
        return clamp((value - min) / range, 0.0f, 1.0f);
    }

    private void setValue(float value, boolean emitChange) {
        float normalized = normalizeValue(value);
        if (this.value == normalized) return;
        float oldValue = this.value;
        this.value = normalized;
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SliderValueChangedEvent(this, oldValue, this.value));
        }
    }

    private float normalizeValue(float value) {
        float clamped = clamp(value, min, max);
        if (step > 0.0f) {
            clamped = min + Math.round((clamped - min) / step) * step;
        }
        return clamp(clamped, min, max);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
