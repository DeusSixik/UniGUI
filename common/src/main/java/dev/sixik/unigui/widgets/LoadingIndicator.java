package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.Visibility;

public class LoadingIndicator extends Box {
    private static final float DEFAULT_SIZE = 24.0f;
    private static final float TAU = (float) (Math.PI * 2.0);

    private final MutableColor accentColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor trackColor = new MutableColor(0.16f, 0.17f, 0.19f, 0.75f);
    private Mode mode = Mode.SPINNER;
    private boolean running = true;
    private float phase;
    private float speed = 1.0f;
    private int segments = 8;
    private float thickness = 3.0f;

    public LoadingIndicator() {
        backgroundVisible(false);
        borderVisible(false);
        accentColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Mode mode() {
        return mode;
    }

    public LoadingIndicator mode(Mode mode) {
        Mode normalized = mode == null ? Mode.SPINNER : mode;
        if (this.mode == normalized) return this;
        this.mode = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean running() {
        return running;
    }

    public LoadingIndicator running(boolean running) {
        if (this.running == running) return this;
        this.running = running;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public LoadingIndicator start() {
        return running(true);
    }

    public LoadingIndicator stop() {
        return running(false);
    }

    public float phase() {
        return phase;
    }

    public LoadingIndicator phase(float phase) {
        float normalized = wrap01(phase);
        if (this.phase == normalized) return this;
        this.phase = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float speed() {
        return speed;
    }

    public LoadingIndicator speed(float speed) {
        float normalized = Float.isFinite(speed) ? Math.max(0.0f, speed) : 1.0f;
        if (this.speed == normalized) return this;
        this.speed = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int segments() {
        return segments;
    }

    public LoadingIndicator segments(int segments) {
        int normalized = Math.max(3, Math.min(16, segments));
        if (this.segments == normalized) return this;
        this.segments = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float thickness() {
        return thickness;
    }

    public LoadingIndicator thickness(float thickness) {
        float normalized = Float.isFinite(thickness) ? Math.max(1.0f, thickness) : 3.0f;
        if (this.thickness == normalized) return this;
        this.thickness = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public LoadingIndicator indicatorSize(float size) {
        float normalized = Float.isFinite(size) ? Math.max(1.0f, size) : DEFAULT_SIZE;
        preferredSize(normalized, normalized);
        return this;
    }

    public MutableColor accentColor() {
        return accentColor;
    }

    public MutableColor trackColor() {
        return trackColor;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        float width = mode == Mode.BAR ? 96.0f : DEFAULT_SIZE;
        float height = mode == Mode.BAR ? 8.0f : DEFAULT_SIZE;
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (!running || visibility() != Visibility.VISIBLE || speed <= 0.0f) return;
        float deltaSeconds = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
        phase = wrap01(phase + deltaSeconds * speed);
        invalidate(InvalidationFlags.VISUAL);
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        accentColor.set(styleValue(StyleKeys.ACCENT_COLOR, accentColor));
        trackColor.set(styleValue(StyleKeys.TRACK_COLOR, trackColor));
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        switch (mode) {
            case SPINNER -> renderSpinner(context);
            case DOTS -> renderDots(context);
            case BAR -> renderBar(context);
        }
        super.renderContent(context);
    }

    private void renderSpinner(RenderContext context) {
        float size = Math.max(1.0f, Math.min(layoutBounds().width(), layoutBounds().height()));
        float x = layoutBounds().x() + (layoutBounds().width() - size) * 0.5f;
        float y = layoutBounds().y() + (layoutBounds().height() - size) * 0.5f;
        float dotSize = Math.max(2.0f, Math.min(size * 0.20f, thickness * 1.6f));
        float radius = Math.max(0.0f, size * 0.5f - dotSize * 0.5f);
        float centerX = x + size * 0.5f;
        float centerY = y + size * 0.5f;
        for (int i = 0; i < segments; i++) {
            float angle = ((i / (float) segments) + phase) * TAU - (float) Math.PI * 0.5f;
            float fade = (i + 1.0f) / segments;
            MutableColor color = colorWithAlpha(accentColor, 0.18f + fade * 0.82f);
            context.circle(
                    centerX + (float) Math.cos(angle) * radius - dotSize * 0.5f,
                    centerY + (float) Math.sin(angle) * radius - dotSize * 0.5f,
                    dotSize,
                    dotSize,
                    Paint.fill(color),
                    transform());
        }
    }

    private void renderDots(RenderContext context) {
        float width = Math.max(1.0f, layoutBounds().width());
        float height = Math.max(1.0f, layoutBounds().height());
        float dotSize = Math.max(2.0f, Math.min(height, width / 5.0f));
        float gap = dotSize * 0.65f;
        float totalWidth = dotSize * 3.0f + gap * 2.0f;
        float startX = layoutBounds().x() + (width - totalWidth) * 0.5f;
        float centerY = layoutBounds().y() + height * 0.5f;
        for (int i = 0; i < 3; i++) {
            float wave = (float) Math.sin((phase + i / 3.0f) * TAU);
            float scale = 0.72f + (wave + 1.0f) * 0.14f;
            float alpha = 0.35f + (wave + 1.0f) * 0.325f;
            float actualSize = dotSize * scale;
            context.circle(
                    startX + i * (dotSize + gap) + (dotSize - actualSize) * 0.5f,
                    centerY - actualSize * 0.5f,
                    actualSize,
                    actualSize,
                    Paint.fill(colorWithAlpha(accentColor, alpha)),
                    transform());
        }
    }

    private void renderBar(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = Math.max(1.0f, layoutBounds().width());
        float height = Math.max(1.0f, layoutBounds().height());
        float radius = height * 0.5f;
        float thumbWidth = Math.max(height, width * 0.35f);
        float travel = Math.max(0.0f, width - thumbWidth);
        float pingPong = phase < 0.5f ? phase * 2.0f : (1.0f - phase) * 2.0f;
        context.roundedRect(x, y, width, height, radius, Paint.fill(trackColor), transform());
        context.roundedRect(x + travel * pingPong, y, thumbWidth, height, radius,
                Paint.fill(accentColor), transform());
    }

    private MutableColor colorWithAlpha(MutableColor source, float alphaMultiplier) {
        return new MutableColor(source.r(), source.g(), source.b(), source.a() * clamp01(alphaMultiplier));
    }

    private static float wrap01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        float wrapped = value % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public enum Mode {
        SPINNER,
        DOTS,
        BAR
    }
}
