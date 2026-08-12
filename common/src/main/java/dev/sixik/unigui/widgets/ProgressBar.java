package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarState;

/**
 * Progress widget for operations with a measurable range.
 *
 * <p>Use {@link #indeterminate(boolean)} when the operation is still progress-like,
 * but the current value is temporarily unknown. For a generic "busy" activity
 * indicator that is not tied to a range/value contract, use {@link LoadingIndicator}
 * with {@link LoadingIndicator.Mode#BAR} instead.</p>
 */
public class ProgressBar extends Box {
    private final MutableColor trackColor = new MutableColor(0.16f, 0.16f, 0.16f, 1.0f);
    private final MutableColor fillColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private ProgressBarRenderer renderer;
    private float min;
    private float max = 1.0f;
    private float value;
    private boolean indeterminate;
    private float indeterminateOffset;
    private float indeterminateSpeed = 0.85f;

    public ProgressBar() {
        backgroundVisible(false);
        borderVisible(false);
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        fillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public ProgressBar range(float min, float max) {
        if (max < min) {
            float swap = min;
            min = max;
            max = swap;
        }
        this.min = min;
        this.max = max;
        value(value);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float value() {
        return value;
    }

    public ProgressBar value(float value) {
        float normalized = clamp(value, min, max);
        if (this.value == normalized) return this;
        this.value = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float progress() {
        float range = max - min;
        return range == 0.0f ? 0.0f : clamp((value - min) / range, 0.0f, 1.0f);
    }

    public boolean indeterminate() {
        return indeterminate;
    }

    public ProgressBar indeterminate(boolean indeterminate) {
        if (this.indeterminate == indeterminate) return this;
        this.indeterminate = indeterminate;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float indeterminateOffset() {
        return indeterminateOffset;
    }

    public float indeterminateSpeed() {
        return indeterminateSpeed;
    }

    public ProgressBar indeterminateSpeed(float indeterminateSpeed) {
        float normalized = Float.isFinite(indeterminateSpeed) ? Math.max(0.0f, indeterminateSpeed) : 0.85f;
        if (this.indeterminateSpeed == normalized) return this;
        this.indeterminateSpeed = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor trackColor() {
        return trackColor;
    }

    public MutableColor fillColor() {
        return fillColor;
    }

    public ProgressBarRenderer renderer() {
        return renderer;
    }

    public ProgressBar renderer(ProgressBarRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public ProgressBar useDefaultRenderer() {
        return renderer(null);
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        trackColor.set(styleValue(StyleKeys.TRACK_COLOR, trackColor));
        fillColor.set(styleValue(StyleKeys.ACCENT_COLOR, fillColor));
    }

    @Override
    protected void renderContent(RenderContext context) {
        effectiveRenderer().render(new DrawScope(context, transform()), snapshot());
        super.renderContent(context);
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (!indeterminate || frame == null) return;
        float delta = Float.isFinite(frame.deltaSeconds()) ? Math.max(0.0f, frame.deltaSeconds()) : 0.0f;
        indeterminateOffset = wrap01(indeterminateOffset + delta * indeterminateSpeed);
        invalidate(InvalidationFlags.VISUAL);
    }

    private ProgressBarRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(ProgressBarRenderer.class, WidgetsRender.progressBar()) : renderer;
    }

    private ProgressBarState snapshot() {
        return new ProgressBarState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                min,
                max,
                value,
                progress(),
                indeterminate,
                indeterminateOffset,
                trackColor.copy(),
                fillColor.copy());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrap01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return value - (float) Math.floor(value);
    }
}
