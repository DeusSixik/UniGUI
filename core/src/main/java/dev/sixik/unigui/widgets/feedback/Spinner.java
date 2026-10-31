package dev.sixik.unigui.widgets.feedback;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;


/** Convenience alias for the default spinner-style loading indicator. */
@AiDoc(summary = "Animated circular or segmented loading spinner widget indicating ongoing background activity.")
@WikiPage(
        title = "Spinner",
        slug = "spinner",
        category = "<key:feedback.category>",
        summary = "<key:spinner.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:spinner.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:spinner.example.basic.title>",
        code = "Spinner spinner = new Spinner(Spinner.Style.ARC_SWEEP);\nspinner.speed(1.5f);"
)
@WikiLink(
        label = "LoadingIndicator",
        target = "page:loading-indicator",
        description = "<key:loading.indicator.summary>"
)
@XmlWidgetName("Spinner")
public final class Spinner extends LoadingIndicator {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SPINNER;

        @AiDoc(summary = "Constructs a default Spinner initialized to Mode.SPINNER.")
    @WikiMethod(summary = "<key:spinner.method.ctorDefault>")
    public Spinner() {
        super();
        mode(Mode.SPINNER);
    }

        @AiDoc(summary = "Constructs a Spinner with the specified visual style preset.")
    @WikiMethod(summary = "<key:spinner.method.ctorStyle>")
    public Spinner(
            @WikiParameter(description = "<key:spinner.param.style>")
            Style style
    ) {
        this();
        style(style);
    }

        @AiDoc(summary = "Returns the active spinner visual style preset.")
    @WikiMethod(summary = "<key:spinner.method.styleGet>")
    @WikiReturn("<key:spinner.return.style>")
    public Style style() {
        return spinnerStyle();
    }

    @XmlAttribute(value = "style", category = "Appearance", defaultValue = "default", description = "Spinner-specific visual style.")
        @AiDoc(summary = "Sets the spinner visual style preset.")
    @WikiMethod(summary = "<key:spinner.method.styleSet>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner style(
            @WikiParameter(description = "<key:spinner.param.style>")
            Style style
    ) {
        spinnerStyle(style);
        return this;
    }

    @Override
    @XmlAttribute(value = "speed", category = "Behavior", defaultValue = "1", description = "Animation speed multiplier.")
        @AiDoc(summary = "Sets the animation speed multiplier.")
    @WikiMethod(summary = "<key:spinner.method.speed>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner speed(
            @WikiParameter(description = "<key:spinner.param.speed>")
            float speed
    ) {
        super.speed(speed);
        return this;
    }

    @Override
    @XmlAttribute(value = "segments", category = "Appearance", defaultValue = "8", description = "Segment count for segmented spinner styles.")
        @AiDoc(summary = "Sets the segment count for segmented spinner styles.")
    @WikiMethod(summary = "<key:spinner.method.segments>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner segments(
            @WikiParameter(description = "<key:spinner.param.segments>")
            int segments
    ) {
        super.segments(segments);
        return this;
    }

    @Override
    @XmlAttribute(value = "dots", category = "Appearance", defaultValue = "8", description = "Dot count for dotted loading styles.")
        @AiDoc(summary = "Sets the dot count for dotted spinner styles.")
    @WikiMethod(summary = "<key:spinner.method.dots>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner dots(
            @WikiParameter(description = "<key:spinner.param.dots>")
            int dots
    ) {
        super.dots(dots);
        return this;
    }

    @Override
    @XmlAttribute(value = "activeDots", category = "Appearance", defaultValue = "4", description = "Number of highlighted dots in dotted styles.")
        @AiDoc(summary = "Sets the number of highlighted active dots in dotted styles.")
    @WikiMethod(summary = "<key:spinner.method.activeDots>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner activeDots(
            @WikiParameter(description = "<key:spinner.param.activeDots>")
            int activeDots
    ) {
        super.activeDots(activeDots);
        return this;
    }

    @Override
    @XmlAttribute(value = "arcs", category = "Appearance", defaultValue = "3", description = "Arc count for multi-arc spinner styles.")
        @AiDoc(summary = "Sets the arc count for multi-arc spinner styles.")
    @WikiMethod(summary = "<key:spinner.method.arcs>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner arcs(
            @WikiParameter(description = "<key:spinner.param.arcs>")
            int arcs
    ) {
        super.arcs(arcs);
        return this;
    }

    @Override
    @XmlAttribute(value = "thickness", category = "Appearance", defaultValue = "3", description = "Stroke thickness in UI pixels.")
        @AiDoc(summary = "Sets the stroke thickness in UI pixels.")
    @WikiMethod(summary = "<key:spinner.method.thickness>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner thickness(
            @WikiParameter(description = "<key:spinner.param.thickness>")
            float thickness
    ) {
        super.thickness(thickness);
        return this;
    }

    @Override
    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Explicit indicator radius; 0 derives it from bounds.")
        @AiDoc(summary = "Sets an explicit indicator radius in UI pixels, or 0 to derive from bounds.")
    @WikiMethod(summary = "<key:spinner.method.radius>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner radius(
            @WikiParameter(description = "<key:spinner.param.radius>")
            float radius
    ) {
        super.radius(radius);
        return this;
    }

    @Override
    @XmlAttribute(value = "angle", category = "Appearance", defaultValue = "4.555", description = "Arc sweep angle in radians for arc-based styles.")
        @AiDoc(summary = "Sets the arc sweep angle in radians for arc-based spinner styles.")
    @WikiMethod(summary = "<key:spinner.method.angle>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner angle(
            @WikiParameter(description = "<key:spinner.param.angle>")
            float radians
    ) {
        super.angle(radians);
        return this;
    }

    @Override
        @AiDoc(summary = "Sets both preferred width and preferred height simultaneously.")
    @WikiMethod(summary = "<key:spinner.method.indicatorSize>")
    @WikiReturn("<key:spinner.return.this>")
    public Spinner indicatorSize(
            @WikiParameter(description = "<key:spinner.param.size>")
            float size
    ) {
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
