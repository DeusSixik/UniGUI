package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;


/** Convenience alias for the default spinner-style loading indicator. */
@XmlWidgetName("Spinner")
public final class Spinner extends LoadingIndicator {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SPINNER;

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

    @XmlAttribute(value = "style", category = "Appearance", defaultValue = "default", description = "Spinner-specific visual style.")
    public Spinner style(Style style) {
        spinnerStyle(style);
        return this;
    }

    @Override
    @XmlAttribute(value = "speed", category = "Behavior", defaultValue = "1", description = "Animation speed multiplier.")
    public Spinner speed(float speed) {
        super.speed(speed);
        return this;
    }

    @Override
    @XmlAttribute(value = "segments", category = "Appearance", defaultValue = "8", description = "Segment count for segmented spinner styles.")
    public Spinner segments(int segments) {
        super.segments(segments);
        return this;
    }

    @Override
    @XmlAttribute(value = "dots", category = "Appearance", defaultValue = "8", description = "Dot count for dotted loading styles.")
    public Spinner dots(int dots) {
        super.dots(dots);
        return this;
    }

    @Override
    @XmlAttribute(value = "activeDots", category = "Appearance", defaultValue = "4", description = "Number of highlighted dots in dotted styles.")
    public Spinner activeDots(int activeDots) {
        super.activeDots(activeDots);
        return this;
    }

    @Override
    @XmlAttribute(value = "arcs", category = "Appearance", defaultValue = "3", description = "Arc count for multi-arc spinner styles.")
    public Spinner arcs(int arcs) {
        super.arcs(arcs);
        return this;
    }

    @Override
    @XmlAttribute(value = "thickness", category = "Appearance", defaultValue = "3", description = "Stroke thickness in UI pixels.")
    public Spinner thickness(float thickness) {
        super.thickness(thickness);
        return this;
    }

    @Override
    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Explicit indicator radius; 0 derives it from bounds.")
    public Spinner radius(float radius) {
        super.radius(radius);
        return this;
    }

    @Override
    @XmlAttribute(value = "angle", category = "Appearance", defaultValue = "4.555", description = "Arc sweep angle in radians for arc-based styles.")
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
