package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.Spinner;

public final class LoadingIndicatorRenderers {
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float PI = (float) Math.PI;

    public static final LoadingIndicatorRenderer SPINNER = (draw, state) -> {
        if (state.spinnerStyle() != Spinner.Style.DEFAULT) {
            renderStyledSpinner(draw, state);
            return;
        }

        float size = Math.max(1.0f, state.size());
        float dotSize = Math.max(2.0f, Math.min(size * 0.20f, state.thickness() * 1.6f));
        float radius = Math.max(0.0f, size * 0.5f - dotSize * 0.5f);
        float centerX = state.centerX();
        float centerY = state.centerY();

        for (int i = 0; i < state.segments(); i++) {
            float angle = ((i / (float) state.segments()) + state.phase()) * TAU - (float) Math.PI * 0.5f;
            float fade = (i + 1.0f) / state.segments();
            MutableColor color = colorWithAlpha(state.accentColor(), 0.18f + fade * 0.82f);
            draw.circle(
                    centerX + (float) Math.cos(angle) * radius - dotSize * 0.5f,
                    centerY + (float) Math.sin(angle) * radius - dotSize * 0.5f,
                    dotSize,
                    dotSize,
                    Paint.fill(color));
        }
    };

    public static final LoadingIndicatorRenderer DOTS = (draw, state) -> {
        float width = Math.max(1.0f, state.width());
        float height = Math.max(1.0f, state.height());
        float dotSize = Math.max(2.0f, Math.min(height, width / 5.0f));
        float gap = dotSize * 0.65f;
        float totalWidth = dotSize * 3.0f + gap * 2.0f;
        float startX = state.x() + (width - totalWidth) * 0.5f;
        float centerY = state.y() + height * 0.5f;

        for (int i = 0; i < 3; i++) {
            float wave = (float) Math.sin((state.phase() + i / 3.0f) * TAU);
            float scale = 0.72f + (wave + 1.0f) * 0.14f;
            float alpha = 0.35f + (wave + 1.0f) * 0.325f;
            float actualSize = dotSize * scale;
            draw.circle(
                    startX + i * (dotSize + gap) + (dotSize - actualSize) * 0.5f,
                    centerY - actualSize * 0.5f,
                    actualSize,
                    actualSize,
                    Paint.fill(colorWithAlpha(state.accentColor(), alpha)));
        }
    };

    public static final LoadingIndicatorRenderer BAR = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(1.0f, state.width());
        float height = Math.max(1.0f, state.height());
        float radius = height * 0.5f;
        float thumbWidth = Math.max(height, width * 0.35f);
        float travel = Math.max(0.0f, width - thumbWidth);
        float pingPong = state.phase() < 0.5f ? state.phase() * 2.0f : (1.0f - state.phase()) * 2.0f;

        draw.roundedRect(x, y, width, height, radius, Paint.fill(state.trackColor()));
        draw.roundedRect(x + travel * pingPong, y, thumbWidth, height, radius,
                Paint.fill(state.accentColor()));
    };

    private LoadingIndicatorRenderers() {
    }

    private static void renderStyledSpinner(DrawScope draw, LoadingIndicatorState state) {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;
        switch (state.spinnerStyle()) {
            case ARC_SWEEP -> arcSweep(draw, state);
            case RING_ARC -> ringArc(draw, state);
            case DOTTED_TRAIL -> dottedTrail(draw, state);
            case DOTTED_PULSE -> dottedPulse(draw, state);
            case DISCRETE_FADE -> discreteFade(draw, state);
            case DOTS_Y -> dotsY(draw, state);
            case DOTS_FADE -> dotsFade(draw, state);
            case DOTS_RADIUS -> dotsRadius(draw, state);
            case DOTS_MOVING -> dotsMoving(draw, state);
            case GRADIENT_ARC -> gradientArc(draw, state);
            case MULTI_ARC -> multiArc(draw, state);
            case GROWING_ARCS -> growingArcs(draw, state);
            case SECTION_FADE -> sectionFade(draw, state);
            case DEFAULT -> {
            }
        }
    }

    private static void arcSweep(DrawScope draw, LoadingIndicatorState state) {
        int segments = autoSegments(state.radius(), state.segments());
        float time = time(state);
        // The sweep angle oscillates between ~15% and ~85% of the full circle,
        // driven by two sine waves at different speeds so the arc smoothly
        // grows and shrinks as it spins — matching the Material Design pattern.
        float minSweep = TAU * 0.15f;
        float maxSweep = TAU * 0.85f;
        float sweepAngle = minSweep + (maxSweep - minSweep) * (0.5f + 0.5f * sin(time * 0.7f));
        // The trailing end lags behind the leading end, creating the classic
        // rubber-band effect: start moves fast, end moves at a different phase.
        float arcStart = time - sweepAngle * 0.5f + sin(time * 0.5f) * 0.4f;
        float arcEnd   = arcStart + sweepAngle;
        strokeArc(draw, state.centerX(), state.centerY(), state.radius(),
                arcStart, arcEnd, segments, state.accentColor(), state.thickness());
    }

    private static void ringArc(DrawScope draw, LoadingIndicatorState state) {
        int segments = autoSegments(state.radius(), state.segments());
        float start = time(state);
        strokeCircle(draw, state.centerX(), state.centerY(), state.radius(), segments, state.trackColor(), state.thickness());
        strokeArc(draw, state.centerX(), state.centerY(), state.radius(), start, start + state.angle(),
                segments, state.accentColor(), state.thickness());
    }

    private static void dottedTrail(DrawScope draw, LoadingIndicatorState state) {
        int dots = Math.min(32, Math.max(3, state.dots()));
        float start = time(state);
        float offset = TAU / dots;
        float dotRadius = Math.max(1.0f, state.thickness() * 0.5f);
        for (int i = 0; i <= dots; i++) {
            float angle = wrapRadians(start + i * offset);
            draw.addCircleFilled(
                    state.centerX() + cos(-angle) * state.radius(),
                    state.centerY() + sin(-angle) * state.radius(),
                    dotRadius,
                    state.trackColor(),
                    8);
        }
        strokeArc(draw, state.centerX(), state.centerY(), state.radius(), start, start + activeDotAngle(state),
                dots, state.accentColor(), state.thickness());
    }

    private static void dottedPulse(DrawScope draw, LoadingIndicatorState state) {
        int dots = Math.min(32, Math.max(3, state.dots()));
        int active = Math.max(1, Math.min(dots, state.activeDots()));
        float start = time(state);
        float offset = TAU / dots;
        float nextDot = (time(state) / TAU * dots) % dots;
        float minRadius = Math.max(1.0f, state.thickness() * 0.5f);

        for (int i = 0; i <= dots; i++) {
            float angle = wrapRadians(start + i * offset);
            float radius = minRadius;
            if (isInActiveWindow(i, nextDot, active, dots)) {
                float wave = (i - nextDot) / active;
                if (wave < 0.0f) wave += 1.0f;
                radius = Math.max(minRadius, sin(wave * PI) * state.thickness());
            }
            draw.addCircleFilled(
                    state.centerX() + cos(-angle) * state.radius(),
                    state.centerY() + sin(-angle) * state.radius(),
                    radius,
                    state.accentColor(),
                    8);
        }
    }

    private static void discreteFade(DrawScope draw, LoadingIndicatorState state) {
        int dots = Math.min(32, Math.max(3, state.dots()));
        float start = time(state);
        float step = PI / dots;
        start -= start % step;
        for (int i = 0; i <= dots; i++) {
            float alpha = Math.max(0.1f, i / (float) dots);
            float angle = start + i * step;
            draw.addCircleFilled(
                    state.centerX() + cos(angle) * state.radius(),
                    state.centerY() + sin(angle) * state.radius(),
                    state.thickness(),
                    colorWithAlpha(state.accentColor(), alpha),
                    8);
        }
    }

    private static void dotsY(DrawScope draw, LoadingIndicatorState state) {
        int dots = Math.max(2, state.dots());
        float start = time(state);
        float spacing = linearSpacing(state, dots);
        float offset = PI / Math.max(1, dots - 1);
        float centerY = state.centerY();
        float startX = state.x() + (state.width() - spacing * (dots - 1)) * 0.5f;
        for (int i = 0; i < dots; i++) {
            float angle = start + (PI - i * offset);
            float y = centerY + sin(angle * 1.35f) * state.thickness() * 2.0f;
            if (y > centerY) y = centerY;
            draw.addCircleFilled(startX + i * spacing, y, state.thickness(), state.accentColor(), 8);
        }
    }

    private static void dotsFade(DrawScope draw, LoadingIndicatorState state) {
        int dots = Math.max(2, state.dots());
        float start = time(state);
        float spacing = linearSpacing(state, dots);
        float offset = PI / Math.max(1, dots - 1);
        float startX = state.x() + (state.width() - spacing * (dots - 1)) * 0.5f;
        for (int i = 0; i < dots; i++) {
            float alpha = Math.max(0.1f, sin((start + (PI - i * offset)) * 1.35f));
            draw.addCircleFilled(startX + i * spacing, state.centerY(), state.thickness(),
                    colorWithAlpha(state.accentColor(), alpha), 8);
        }
    }

    private static void dotsRadius(DrawScope draw, LoadingIndicatorState state) {
        int dots = Math.max(2, state.dots());
        float start = time(state);
        float spacing = linearSpacing(state, dots);
        float offset = PI / Math.max(1, dots - 1);
        float startX = state.x() + (state.width() - spacing * (dots - 1)) * 0.5f;
        MutableColor faded = colorWithAlpha(state.accentColor(), 0.10f);
        for (int i = 0; i < dots; i++) {
            float radius = Math.max(0.0f, state.thickness() * sin((start + (PI - i * offset)) * 1.35f));
            float x = startX + i * spacing;
            draw.addCircleFilled(x, state.centerY(), state.thickness(), faded, 8);
            draw.addCircleFilled(x, state.centerY(), radius, state.accentColor(), 8);
        }
    }

    private static void dotsMoving(DrawScope draw, LoadingIndicatorState state) {
        int dots = Math.max(2, state.dots());
        float start = time(state);
        float innerWidth = Math.max(1.0f, state.width() - state.thickness() * 2.0f);
        for (int i = 0; i < dots; i++) {
            float offset = (start * state.width() / TAU + i * (innerWidth / dots)) % innerWidth;
            float radius = state.thickness();
            if (offset < radius) radius = offset;
            if (offset > innerWidth - radius) radius = innerWidth - offset;
            draw.addCircleFilled(state.x() + state.thickness() + offset, state.centerY(),
                    Math.max(0.0f, radius), state.accentColor(), 8);
        }
    }

    private static void gradientArc(DrawScope draw, LoadingIndicatorState state) {
        int segments = autoSegments(state.radius(), state.segments());
        float start = time(state);
        float angleOffset = state.angle() / segments;
        float thicknessStep = Math.max(0.25f, state.thickness() / segments);
        for (int i = 1; i < segments; i++) {
            float a0 = start + (i - 1) * angleOffset;
            float a1 = start + i * angleOffset;
            draw.addLine(
                    state.centerX() + cos(a0) * state.radius(),
                    state.centerY() + sin(a0) * state.radius(),
                    state.centerX() + cos(a1) * state.radius(),
                    state.centerY() + sin(a1) * state.radius(),
                    state.accentColor(),
                    thicknessStep * i);
        }
    }

    private static void multiArc(DrawScope draw, LoadingIndicatorState state) {
        int segments = autoSegments(state.radius(), state.segments());
        int arcs = Math.max(1, state.arcs());
        float time = time(state);
        // Background track ring.
        strokeCircle(draw, state.centerX(), state.centerY(), state.radius(),
                segments, state.trackColor(), state.thickness());
        // N equally-spaced arcs, all rotating at the same speed.
        // The sweep angle of each arc equals state.angle(); a sensible
        // default is TAU / arcs * 0.5 (half the section per arc).
        float sweepAngle = state.angle() > 0.0f
                ? state.angle()
                : TAU / arcs * 0.5f;
        for (int arc = 0; arc < arcs; arc++) {
            float arcStart = TAU * arc / arcs + time;
            strokeArc(draw, state.centerX(), state.centerY(), state.radius(),
                    arcStart, arcStart + sweepAngle,
                    segments, state.accentColor(), state.thickness());
        }
    }

    private static void growingArcs(DrawScope draw, LoadingIndicatorState state) {
        int segments = autoSegments(state.radius(), state.segments());
        float start = time(state);
        float grow = (0.18f + 0.82f * Math.abs(sin(start * 0.7f))) * PI;
        strokeArc(draw, state.centerX(), state.centerY(), state.radius(), start, start + grow * 2.0f,
                segments * 2, state.secondaryColor(), state.thickness());
        strokeArc(draw, state.centerX(), state.centerY(), Math.max(0.0f, state.radius() - state.thickness() * 1.6f),
                start, start + grow, segments, state.accentColor(), state.thickness());
    }

    private static void sectionFade(DrawScope draw, LoadingIndicatorState state) {
        int arcs = Math.max(2, state.arcs());
        int segments = Math.max(3, Math.max(12, state.segments()) / arcs);
        float arcAngle = TAU / arcs;
        float angleOffset = arcAngle / segments;
        float start = (state.elapsedSeconds() * state.speed() * TAU) % (TAU * 2.0f);
        for (int arc = 0; arc < arcs; arc++) {
            draw.pathClear();
            for (int i = 0; i <= segments + 1; i++) {
                float a = arcAngle * arc + i * angleOffset - PI * 0.75f;
                draw.pathLineTo(state.centerX() + cos(a) * state.radius(), state.centerY() + sin(a) * state.radius());
            }
            float a = arcAngle * arc;
            float alpha = start < TAU
                    ? sectionFillAlpha(start, a, arcAngle)
                    : sectionFadeAlpha(start - TAU, a, arcAngle);
            draw.pathStroke(colorWithAlpha(state.accentColor(), Math.max(0.05f, alpha)), false, state.thickness());
        }
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

    private static float activeDotAngle(LoadingIndicatorState state) {
        int dots = Math.max(1, state.dots());
        return Math.max(1, state.activeDots()) / (float) dots * TAU;
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

    private static float linearSpacing(LoadingIndicatorState state, int dots) {
        return Math.max(state.thickness() * 2.4f,
                (Math.max(1.0f, state.width()) - state.thickness() * 2.0f) / Math.max(1, dots - 1));
    }

    private static float time(LoadingIndicatorState state) {
        return state.elapsedSeconds() * state.speed() * TAU;
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
}
