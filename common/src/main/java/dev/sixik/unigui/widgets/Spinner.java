package dev.sixik.unigui.widgets;

/** Convenience alias for the default spinner-style loading indicator. */
public final class Spinner extends LoadingIndicator {
    public Spinner() {
        super();
        mode(Mode.SPINNER);
    }

    public Spinner(Style style) {
        this();
        style(style);
    }

    public Style style() {
        return spinnerStyle();
    }

    public Spinner style(Style style) {
        spinnerStyle(style);
        return this;
    }

    @Override
    public Spinner speed(float speed) {
        super.speed(speed);
        return this;
    }

    @Override
    public Spinner segments(int segments) {
        super.segments(segments);
        return this;
    }

    @Override
    public Spinner dots(int dots) {
        super.dots(dots);
        return this;
    }

    @Override
    public Spinner activeDots(int activeDots) {
        super.activeDots(activeDots);
        return this;
    }

    @Override
    public Spinner arcs(int arcs) {
        super.arcs(arcs);
        return this;
    }

    @Override
    public Spinner thickness(float thickness) {
        super.thickness(thickness);
        return this;
    }

    @Override
    public Spinner radius(float radius) {
        super.radius(radius);
        return this;
    }

    @Override
    public Spinner angle(float radians) {
        super.angle(radians);
        return this;
    }

    @Override
    public Spinner indicatorSize(float size) {
        super.indicatorSize(size);
        return this;
    }

    public enum Style {
        DEFAULT,
        ARC_SWEEP,
        RING_ARC,
        DOTTED_TRAIL,
        DOTTED_PULSE,
        DISCRETE_FADE,
        DOTS_Y,
        DOTS_FADE,
        DOTS_RADIUS,
        DOTS_MOVING,
        GRADIENT_ARC,
        MULTI_ARC,
        GROWING_ARCS,
        SECTION_FADE
    }
}
