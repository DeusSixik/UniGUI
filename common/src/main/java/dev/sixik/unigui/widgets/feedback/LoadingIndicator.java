package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;

/**
 * Generic animated activity indicator.
 *
 * <p>{@link Mode#BAR} communicates that work is in progress without exposing a
 * progress range. Use {@link ProgressBar#indeterminate(boolean)} when the UI
 * represents progress for a ranged operation, but the current value is unknown.</p>
 */
@XmlWidgetName("LoadingIndicator")
public class LoadingIndicator extends Box {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.LOADING_INDICATOR;

    public static final float DEFAULT_PREFERRED_SIZE = 24.0f;
    public static final float DEFAULT_BAR_PREFERRED_WIDTH = 96.0f;
    public static final float DEFAULT_BAR_PREFERRED_HEIGHT = 8.0f;

    private final MutableColor accentColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor secondaryColor = new MutableColor(1.0f, 1.0f, 1.0f, 0.95f);
    private final MutableColor trackColor = new MutableColor(0.16f, 0.17f, 0.19f, 0.75f);
    private Mode mode = Mode.SPINNER;
    private Spinner.Style spinnerStyle = Spinner.Style.DEFAULT;
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
        boxVisualEnabled(false);
        backgroundVisible(false);
        borderVisible(false);
        accentColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        secondaryColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Mode mode() {
        return mode;
    }

    @XmlAttribute(value = "mode", category = "Behavior", defaultValue = "spinner", description = "Indicator rendering mode.")
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

    @XmlAttribute(value = "running", category = "Behavior", defaultValue = "true", description = "Whether the indicator animation advances over time.")
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

    @XmlAttribute(value = "phase", category = "Behavior", defaultValue = "0", description = "Initial animation phase in the 0..1 range.")
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

    @XmlAttribute(value = "speed", category = "Behavior", defaultValue = "1", description = "Animation speed multiplier.")
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

    @XmlAttribute(value = "segments", category = "Appearance", defaultValue = "8", description = "Segment count for segmented spinner styles.")
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

    @XmlAttribute(value = "spinnerStyle", category = "Appearance", defaultValue = "default", description = "Visual style used when mode is spinner.")
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

    @XmlAttribute(value = "dots", category = "Appearance", defaultValue = "8", description = "Dot count for dotted loading styles.")
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

    @XmlAttribute(value = "activeDots", category = "Appearance", defaultValue = "4", description = "Number of highlighted dots in dotted styles.")
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

    @XmlAttribute(value = "arcs", category = "Appearance", defaultValue = "3", description = "Arc count for multi-arc spinner styles.")
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

    @XmlAttribute(value = "thickness", category = "Appearance", defaultValue = "3", description = "Stroke thickness in UI pixels.")
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

    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Explicit indicator radius; 0 derives it from bounds.")
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

    @XmlAttribute(value = "angle", category = "Appearance", defaultValue = "4.555", description = "Arc sweep angle in radians for arc-based styles.")
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

    @XmlAttribute(value = "preferredWidth", category = "Layout", defaultValue = "24", description = "Intrinsic indicator width before layout constraints are applied.")
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

    @XmlAttribute(value = "preferredHeight", category = "Layout", defaultValue = "24", description = "Intrinsic indicator height before layout constraints are applied.")
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

    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float PI = (float) Math.PI;

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderCustomVisual(draw)) {
            super.renderContent(context);
            return;
        }
        switch (mode) {
            case SPINNER -> renderSpinner(draw);
            case DOTS -> renderDots(draw);
            case BAR -> renderBar(draw);
        }
        super.renderContent(context);
    }

    private void renderSpinner(DrawScope draw) {
        if (spinnerStyle != Spinner.Style.DEFAULT) {
            renderStyledSpinner(draw);
            return;
        }

        float size = Math.max(1.0f, Math.min(layoutBounds().width(), layoutBounds().height()));
        float dotSize = Math.max(2.0f, Math.min(size * 0.20f, thickness * 1.6f));
        float radius = Math.max(0.0f, size * 0.5f - dotSize * 0.5f);
        float centerX = layoutBounds().x() + layoutBounds().width() * 0.5f;
        float centerY = layoutBounds().y() + layoutBounds().height() * 0.5f;

        for (int i = 0; i < segments; i++) {
            float angle = ((i / (float) segments) + phase + 0.125f) * TAU - (float) Math.PI * 0.5f;
            float fade = (i + 1.0f) / segments;
            MutableColor color = colorWithAlpha(accentColor, 0.18f + fade * 0.82f);
            draw.circle(
                    centerX + (float) Math.cos(angle) * radius - dotSize * 0.5f,
                    centerY + (float) Math.sin(angle) * radius - dotSize * 0.5f,
                    dotSize,
                    dotSize,
                    Paint.fill(color));
        }
    }

    private void renderDots(DrawScope draw) {
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
            draw.circle(
                    startX + i * (dotSize + gap) + (dotSize - actualSize) * 0.5f,
                    centerY - actualSize * 0.5f,
                    actualSize,
                    actualSize,
                    Paint.fill(colorWithAlpha(accentColor, alpha)));
        }
    }

    private void renderBar(DrawScope draw) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = Math.max(1.0f, layoutBounds().width());
        float height = Math.max(1.0f, layoutBounds().height());
        float radius = height * 0.5f;
        float thumbWidth = Math.max(height, width * 0.35f);
        float travel = Math.max(0.0f, width - thumbWidth);
        float pingPong = phase < 0.5f ? phase * 2.0f : (1.0f - phase) * 2.0f;

        draw.roundedRect(x, y, width, height, radius, Paint.fill(trackColor));
        draw.roundedRect(x + travel * pingPong, y, thumbWidth, height, radius,
                Paint.fill(accentColor));
    }

    private void renderStyledSpinner(DrawScope draw) {
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;
        switch (spinnerStyle) {
            case ARC_SWEEP -> arcSweep(draw);
            case RING_ARC -> ringArc(draw);
            case DOTTED_TRAIL -> dottedTrail(draw);
            case DOTTED_PULSE -> dottedPulse(draw);
            case DISCRETE_FADE -> discreteFade(draw);
            case DOTS_Y -> dotsY(draw);
            case DOTS_FADE -> dotsFade(draw);
            case DOTS_RADIUS -> dotsRadius(draw);
            case DOTS_MOVING -> dotsMoving(draw);
            case GRADIENT_ARC -> gradientArc(draw);
            case MULTI_ARC -> multiArc(draw);
            case GROWING_ARCS -> growingArcs(draw);
            case SECTION_FADE -> sectionFade(draw);
            case DEFAULT -> {
            }
        }
    }

    private void arcSweep(DrawScope draw) {
        int segs = autoSegments(effectiveRadius(), segments);
        float time = time();
        float minSweep = TAU * 0.15f;
        float maxSweep = TAU * 0.85f;
        float sweepAngle = minSweep + (maxSweep - minSweep) * (0.5f + 0.5f * sin(time * 0.7f));
        float arcStart = time - sweepAngle * 0.5f + sin(time * 0.5f) * 0.4f;
        float arcEnd = arcStart + sweepAngle;
        strokeArc(draw, centerX(), centerY(), effectiveRadius(),
                arcStart, arcEnd, segs, accentColor, thickness);
    }

    private void ringArc(DrawScope draw) {
        int segs = autoSegments(effectiveRadius(), segments);
        float start = time();
        strokeCircle(draw, centerX(), centerY(), effectiveRadius(), segs, trackColor, thickness);
        strokeArc(draw, centerX(), centerY(), effectiveRadius(), start, start + angle,
                segs, accentColor, thickness);
    }

    private void dottedTrail(DrawScope draw) {
        int dotCount = Math.min(32, Math.max(3, dots));
        float start = time();
        float offset = TAU / dotCount;
        float dotRadius = Math.max(1.0f, thickness * 0.5f);
        for (int i = 0; i <= dotCount; i++) {
            float a = wrapRadians(start + i * offset);
            draw.addCircleFilled(
                    centerX() + cos(-a) * effectiveRadius(),
                    centerY() + sin(-a) * effectiveRadius(),
                    dotRadius,
                    trackColor,
                    8);
        }
        strokeArc(draw, centerX(), centerY(), effectiveRadius(), start, start + activeDotAngle(),
                dotCount, accentColor, thickness);
    }

    private void dottedPulse(DrawScope draw) {
        int dotCount = Math.min(32, Math.max(3, dots));
        int active = Math.max(1, Math.min(dotCount, activeDots));
        float start = time();
        float offset = TAU / dotCount;
        float nextDot = (time() / TAU * dotCount) % dotCount;
        float minRadius = Math.max(1.0f, thickness * 0.5f);

        for (int i = 0; i <= dotCount; i++) {
            float a = wrapRadians(start + i * offset);
            float dotRadius = minRadius;
            if (isInActiveWindow(i, nextDot, active, dotCount)) {
                float wave = (i - nextDot) / active;
                if (wave < 0.0f) wave += 1.0f;
                dotRadius = Math.max(minRadius, sin(wave * PI) * thickness);
            }
            draw.addCircleFilled(
                    centerX() + cos(-a) * effectiveRadius(),
                    centerY() + sin(-a) * effectiveRadius(),
                    dotRadius,
                    accentColor,
                    8);
        }
    }

    private void discreteFade(DrawScope draw) {
        int dotCount = Math.min(32, Math.max(3, dots));
        float start = time();
        float step = PI / dotCount;
        start -= start % step;
        for (int i = 0; i <= dotCount; i++) {
            float alpha = Math.max(0.1f, i / (float) dotCount);
            float a = start + i * step;
            draw.addCircleFilled(
                    centerX() + cos(a) * effectiveRadius(),
                    centerY() + sin(a) * effectiveRadius(),
                    thickness,
                    colorWithAlpha(accentColor, alpha),
                    8);
        }
    }

    private void dotsY(DrawScope draw) {
        int dotCount = Math.max(2, dots);
        float start = time();
        float spacing = linearSpacing(dotCount);
        float offset = PI / Math.max(1, dotCount - 1);
        float centerY = centerY();
        float startX = layoutBounds().x() + (layoutBounds().width() - spacing * (dotCount - 1)) * 0.5f;
        for (int i = 0; i < dotCount; i++) {
            float a = start + (PI - i * offset);
            float y = centerY + sin(a * 1.35f) * thickness * 2.0f;
            if (y > centerY) y = centerY;
            draw.addCircleFilled(startX + i * spacing, y, thickness, accentColor, 8);
        }
    }

    private void dotsFade(DrawScope draw) {
        int dotCount = Math.max(2, dots);
        float start = time();
        float spacing = linearSpacing(dotCount);
        float offset = PI / Math.max(1, dotCount - 1);
        float startX = layoutBounds().x() + (layoutBounds().width() - spacing * (dotCount - 1)) * 0.5f;
        for (int i = 0; i < dotCount; i++) {
            float alpha = Math.max(0.1f, sin((start + (PI - i * offset)) * 1.35f));
            draw.addCircleFilled(startX + i * spacing, centerY(),
                    thickness, colorWithAlpha(accentColor, alpha), 8);
        }
    }

    private void dotsRadius(DrawScope draw) {
        int dotCount = Math.max(2, dots);
        float start = time();
        float spacing = linearSpacing(dotCount);
        float offset = PI / Math.max(1, dotCount - 1);
        float startX = layoutBounds().x() + (layoutBounds().width() - spacing * (dotCount - 1)) * 0.5f;
        MutableColor faded = colorWithAlpha(accentColor, 0.10f);
        for (int i = 0; i < dotCount; i++) {
            float dotRadius = Math.max(0.0f, thickness * sin((start + (PI - i * offset)) * 1.35f));
            float x = startX + i * spacing;
            draw.addCircleFilled(x, centerY(), thickness, faded, 8);
            draw.addCircleFilled(x, centerY(), dotRadius, accentColor, 8);
        }
    }

    private void dotsMoving(DrawScope draw) {
        int dotCount = Math.max(2, dots);
        float start = time();
        float innerWidth = Math.max(1.0f, layoutBounds().width() - thickness * 2.0f);
        for (int i = 0; i < dotCount; i++) {
            float offset = (start * layoutBounds().width() / TAU + i * (innerWidth / dotCount)) % innerWidth;
            float dotRadius = thickness;
            if (offset < dotRadius) dotRadius = offset;
            if (offset > innerWidth - dotRadius) dotRadius = innerWidth - offset;
            draw.addCircleFilled(layoutBounds().x() + thickness + offset, centerY(),
                    Math.max(0.0f, dotRadius), accentColor, 8);
        }
    }

    private void gradientArc(DrawScope draw) {
        int segs = autoSegments(effectiveRadius(), segments);
        float start = time();
        float angleOffset = angle / segs;
        float thicknessStep = Math.max(0.25f, thickness / segs);
        for (int i = 1; i < segs; i++) {
            float a0 = start + (i - 1) * angleOffset;
            float a1 = start + i * angleOffset;
            draw.addLine(
                    centerX() + cos(a0) * effectiveRadius(),
                    centerY() + sin(a0) * effectiveRadius(),
                    centerX() + cos(a1) * effectiveRadius(),
                    centerY() + sin(a1) * effectiveRadius(),
                    accentColor,
                    thicknessStep * i);
        }
    }

    private void multiArc(DrawScope draw) {
        int segs = autoSegments(effectiveRadius(), segments);
        int arcCount = Math.max(1, arcs);
        float time = time();
        strokeCircle(draw, centerX(), centerY(), effectiveRadius(),
                segs, trackColor, thickness);
        float sweepAngle = angle > 0.0f ? angle : TAU / arcCount * 0.5f;
        for (int arc = 0; arc < arcCount; arc++) {
            float arcStart = TAU * arc / arcCount + time;
            strokeArc(draw, centerX(), centerY(), effectiveRadius(),
                    arcStart, arcStart + sweepAngle,
                    segs, accentColor, thickness);
        }
    }

    private void growingArcs(DrawScope draw) {
        int segs = autoSegments(effectiveRadius(), segments);
        float start = time();
        float grow = (0.18f + 0.82f * Math.abs(sin(start * 0.7f))) * PI;
        strokeArc(draw, centerX(), centerY(), effectiveRadius(), start, start + grow * 2.0f,
                segs * 2, secondaryColor, thickness);
        strokeArc(draw, centerX(), centerY(), Math.max(0.0f, effectiveRadius() - thickness * 1.6f),
                start, start + grow, segs, accentColor, thickness);
    }

    private void sectionFade(DrawScope draw) {
        int arcCount = Math.max(2, arcs);
        int segs = Math.max(3, Math.max(12, segments) / arcCount);
        float arcAngle = TAU / arcCount;
        float angleOffset = arcAngle / segs;
        float start = (elapsedSeconds * speed * TAU) % (TAU * 2.0f);
        for (int arc = 0; arc < arcCount; arc++) {
            draw.pathClear();
            for (int i = 0; i <= segs + 1; i++) {
                float a = arcAngle * arc + i * angleOffset - PI * 0.75f;
                draw.pathLineTo(centerX() + cos(a) * effectiveRadius(), centerY() + sin(a) * effectiveRadius());
            }
            float a = arcAngle * arc;
            float alpha = start < TAU
                    ? sectionFillAlpha(start, a, arcAngle)
                    : sectionFadeAlpha(start - TAU, a, arcAngle);
            draw.pathStroke(colorWithAlpha(accentColor, Math.max(0.05f, alpha)), false, thickness);
        }
    }

    private float centerX() {
        return layoutBounds().x() + layoutBounds().width() * 0.5f;
    }

    private float centerY() {
        return layoutBounds().y() + layoutBounds().height() * 0.5f;
    }

    private float time() {
        return elapsedSeconds * speed * TAU;
    }

    private float activeDotAngle() {
        int dotCount = Math.max(1, dots);
        return Math.max(1, activeDots) / (float) dotCount * TAU;
    }

    private float linearSpacing(int dotCount) {
        return Math.max(thickness * 2.4f,
                (Math.max(1.0f, layoutBounds().width()) - thickness * 2.0f) / Math.max(1, dotCount - 1));
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

    private static void strokeCircle(DrawScope draw, float centerX, float centerY, float radius,
                                     int segments, ColorView color, float thickness) {
        draw.pathClear();
        for (int i = 0; i <= segments; i++) {
            float a = i * TAU / segments;
            draw.pathLineTo(centerX + cos(a) * radius, centerY + sin(a) * radius);
        }
        draw.pathStroke(color, false, thickness);
    }

    private static void strokeArc(DrawScope draw, float centerX, float centerY, float radius,
                                  float minAngle, float maxAngle, int segments,
                                  ColorView color, float thickness) {
        draw.pathClear();
        int count = Math.max(2, segments);
        for (int i = 0; i < count; i++) {
            float t = i / (float) (count - 1);
            float a = minAngle + t * (maxAngle - minAngle);
            draw.pathLineTo(centerX + cos(a) * radius, centerY + sin(a) * radius);
        }
        draw.pathStroke(color, false, thickness);
    }

    private static float sectionFillAlpha(float start, float a, float arcAngle) {
        float value = 0.0f;
        if (start > a && start < a + arcAngle) {
            value = 1.0f - (start - a) / arcAngle;
        } else if (start < a) {
            value = 1.0f;
        }
        return 1.0f - value;
    }

    private static float sectionFadeAlpha(float start, float a, float arcAngle) {
        float value = 0.0f;
        if (start > a && start < a + arcAngle) {
            value = 1.0f - (start - a) / arcAngle;
        } else if (start < a) {
            value = 1.0f;
        }
        return value;
    }

    private static boolean isInActiveWindow(int i, float nextDot, int active, int dots) {
        float end = nextDot + active;
        if (end < dots) {
            return i > nextDot && i < end;
        }
        return i > nextDot || i < (end % dots);
    }

    private static int autoSegments(float radius, int requested) {
        if (requested > 0) return Math.max(8, Math.min(96, requested));
        return Math.max(12, Math.min(64, Math.round(radius * 1.6f)));
    }

    private static float wrapRadians(float value) {
        float wrapped = value % TAU;
        return wrapped < 0.0f ? wrapped + TAU : wrapped;
    }

    private static float sin(float value) {
        return (float) Math.sin(value);
    }

    private static float cos(float value) {
        return (float) Math.cos(value);
    }

    private static MutableColor colorWithAlpha(ColorView source, float alphaMultiplier) {
        return new MutableColor(source.r(), source.g(), source.b(), source.a() * clamp01(alphaMultiplier));
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
