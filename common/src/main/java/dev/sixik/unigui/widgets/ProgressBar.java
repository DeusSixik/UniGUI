package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarState;

public class ProgressBar extends Box {
    private final MutableColor trackColor = new MutableColor(0.16f, 0.16f, 0.16f, 1.0f);
    private final MutableColor fillColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private ProgressBarRenderer renderer;
    private float min;
    private float max = 1.0f;
    private float value;

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

    private ProgressBarRenderer effectiveRenderer() {
        return renderer == null ? WidgetsRender.progressBar() : renderer;
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
                trackColor.copy(),
                fillColor.copy());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
