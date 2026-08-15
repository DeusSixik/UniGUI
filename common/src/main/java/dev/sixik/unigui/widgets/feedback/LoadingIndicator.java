package dev.sixik.unigui.widgets.feedback;

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
import dev.sixik.unigui.widgets.containers.Box;

/**
 * Generic animated activity indicator.
 *
 * <p>{@link Mode#BAR} communicates that work is in progress without exposing a
 * progress range. Use {@link ProgressBar#indeterminate(boolean)} when the UI
 * represents progress for a ranged operation, but the current value is unknown.</p>
 */
public class LoadingIndicator extends Box {
    public static final float DEFAULT_PREFERRED_SIZE = 24.0f;
    public static final float DEFAULT_BAR_PREFERRED_WIDTH = 96.0f;
    public static final float DEFAULT_BAR_PREFERRED_HEIGHT = 8.0f;

    private final MutableColor accentColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor secondaryColor = new MutableColor(1.0f, 1.0f, 1.0f, 0.95f);
    private final MutableColor trackColor = new MutableColor(0.16f, 0.17f, 0.19f, 0.75f);
    private Mode mode = Mode.SPINNER;
    private Spinner.Style spinnerStyle = Spinner.Style.DEFAULT;
    private LoadingIndicatorRenderer renderer;
    private boolean running = true;
    private float phase;
    private float elapsedSeconds;
    private float speed = 1.0f;
    private int segments = 8;
    private int dots = 8;
    private int activeDots = 4;
    private int arcs = 3;
    private float thickness = 3.0f;
    private float radius;
    private float angle = (float) (Math.PI * 1.45);
    private float preferredWidth = Float.NaN;
    private float preferredHeight = Float.NaN;

    public LoadingIndicator() {
        backgroundVisible(false);
        borderVisible(false);
        accentColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        secondaryColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
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
        elapsedSeconds = speed > 0.0f ? normalized / speed : normalized;
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
        int normalized = Math.max(3, Math.min(96, segments));
        if (this.segments == normalized) return this;
        this.segments = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Spinner.Style spinnerStyle() {
        return spinnerStyle;
    }

    public LoadingIndicator spinnerStyle(Spinner.Style spinnerStyle) {
        Spinner.Style normalized = spinnerStyle == null ? Spinner.Style.DEFAULT : spinnerStyle;
        if (this.spinnerStyle == normalized) return this;
        this.spinnerStyle = normalized;
        mode(Mode.SPINNER);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int dots() {
        return dots;
    }

    public LoadingIndicator dots(int dots) {
        int normalized = Math.max(2, Math.min(32, dots));
        if (this.dots == normalized) return this;
        this.dots = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int activeDots() {
        return activeDots;
    }

    public LoadingIndicator activeDots(int activeDots) {
        int normalized = Math.max(1, Math.min(32, activeDots));
        if (this.activeDots == normalized) return this;
        this.activeDots = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int arcs() {
        return arcs;
    }

    public LoadingIndicator arcs(int arcs) {
        int normalized = Math.max(1, Math.min(12, arcs));
        if (this.arcs == normalized) return this;
        this.arcs = normalized;
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

    public float radius() {
        return radius;
    }

    public LoadingIndicator radius(float radius) {
        float normalized = Float.isFinite(radius) ? Math.max(0.0f, radius) : 0.0f;
        if (this.radius == normalized) return this;
        this.radius = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float angle() {
        return angle;
    }

    public LoadingIndicator angle(float radians) {
        float normalized = Float.isFinite(radians) ? Math.max(0.0f, radians) : (float) (Math.PI * 1.45);
        if (this.angle == normalized) return this;
        this.angle = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public LoadingIndicator indicatorSize(float size) {
        float normalized = positiveOr(size, DEFAULT_PREFERRED_SIZE);
        return preferredSize(normalized, normalized);
    }

    public float preferredWidth() {
        return effectivePreferredWidth();
    }

    public LoadingIndicator preferredWidth(float preferredWidth) {
        float normalized = positiveOr(preferredWidth, defaultPreferredWidth());
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float preferredHeight() {
        return effectivePreferredHeight();
    }

    public LoadingIndicator preferredHeight(float preferredHeight) {
        float normalized = positiveOr(preferredHeight, defaultPreferredHeight());
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public LoadingIndicator preferredSize(float width, float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    public LoadingIndicator useDefaultPreferredSize() {
        if (Float.isNaN(preferredWidth) && Float.isNaN(preferredHeight)) return this;
        preferredWidth = Float.NaN;
        preferredHeight = Float.NaN;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor accentColor() {
        return accentColor;
    }

    public MutableColor secondaryColor() {
        return secondaryColor;
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
        setDesiredSize(resolveDesiredSize(context, effectivePreferredWidth(), effectivePreferredHeight()));
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (!running || visibility() != Visibility.VISIBLE || speed <= 0.0f) return;
        float deltaSeconds = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
        elapsedSeconds += deltaSeconds;
        phase = wrap01(elapsedSeconds * speed);
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
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot());
        super.renderContent(context);
    }

    private LoadingIndicatorRenderer effectiveRenderer() {
        if (renderer != null) return renderer;
        LoadingIndicatorRenderer fallback = switch (mode) {
            case SPINNER -> WidgetsRender.loadingSpinner();
            case DOTS -> WidgetsRender.loadingDots();
            case BAR -> WidgetsRender.loadingBar();
        };
        return styleRenderer(LoadingIndicatorRenderer.class, fallback);
    }
    private LoadingIndicatorState snapshot() {
        return new LoadingIndicatorState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                phase,
                elapsedSeconds,
                speed,
                segments,
                dots,
                activeDots,
                arcs,
                thickness,
                effectiveRadius(),
                angle,
                accentColor.copy(),
                secondaryColor.copy(),
                trackColor.copy(),
                spinnerStyle);
    }

    private float effectiveRadius() {
        if (radius > 0.0f) return radius;
        float size = Math.max(1.0f, Math.min(layoutBounds().width(), layoutBounds().height()));
        return Math.max(1.0f, size * 0.5f - Math.max(1.0f, thickness));
    }

    private float effectivePreferredWidth() {
        return Float.isFinite(preferredWidth) ? preferredWidth : defaultPreferredWidth();
    }

    private float effectivePreferredHeight() {
        return Float.isFinite(preferredHeight) ? preferredHeight : defaultPreferredHeight();
    }

    private float defaultPreferredWidth() {
        return mode == Mode.BAR ? DEFAULT_BAR_PREFERRED_WIDTH : DEFAULT_PREFERRED_SIZE;
    }

    private float defaultPreferredHeight() {
        return mode == Mode.BAR ? DEFAULT_BAR_PREFERRED_HEIGHT : DEFAULT_PREFERRED_SIZE;
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
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
