package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;

public class ProgressBar extends Box {
    private final MutableColor trackColor = new MutableColor(0.16f, 0.16f, 0.16f, 1.0f);
    private final MutableColor fillColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
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

    @Override
    protected void applyTheme() {
        super.applyTheme();
        trackColor.set(styleValue(StyleKeys.TRACK_COLOR, trackColor));
        fillColor.set(styleValue(StyleKeys.ACCENT_COLOR, fillColor));
    }

    @Override
    protected void renderContent(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        float radius = Math.min(width, height) * 0.5f;
        context.roundedRect(x, y, width, height, radius, Paint.fill(trackColor), transform());
        context.roundedRect(x, y, width * progress(), height, radius, Paint.fill(fillColor), transform());
        super.renderContent(context);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
