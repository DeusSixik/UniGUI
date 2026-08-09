package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderer;
import dev.sixik.unigui.widgets.render.LoadingIndicatorState;

public class LoadingIndicator extends Box {
    private static final float DEFAULT_SIZE = 24.0f;

    private final MutableColor accentColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor trackColor = new MutableColor(0.16f, 0.17f, 0.19f, 0.75f);
    private Mode mode = Mode.SPINNER;
    private LoadingIndicatorRenderer renderer;
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

    public LoadingIndicatorRenderer renderer() {
        return renderer;
    }

    public LoadingIndicator renderer(LoadingIndicatorRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public LoadingIndicator useDefaultRenderer() {
        return renderer(null);
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
        layout(style -> style.size(normalized, normalized));
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
        effectiveRenderer().render(new DrawScope(context, transform()), snapshot());
        super.renderContent(context);
    }

    private LoadingIndicatorRenderer effectiveRenderer() {
        if (renderer != null) return renderer;
        return switch (mode) {
            case SPINNER -> WidgetsRender.loadingSpinner();
            case DOTS -> WidgetsRender.loadingDots();
            case BAR -> WidgetsRender.loadingBar();
        };
    }

    private LoadingIndicatorState snapshot() {
        return new LoadingIndicatorState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                phase,
                speed,
                segments,
                thickness,
                accentColor.copy(),
                trackColor.copy());
    }

    private static float wrap01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        float wrapped = value % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    public enum Mode {
        SPINNER,
        DOTS,
        BAR
    }
}
