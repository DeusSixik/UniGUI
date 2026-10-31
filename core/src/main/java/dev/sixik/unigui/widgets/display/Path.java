package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Vector path widget drawing complex custom 2D shapes, bezier curves, and polylines.",
        purpose = "Displays complex procedural vector shapes defined by VectorPath geometry.",
        useWhen = {
                "Drawing vector icons, custom arrows, stars, or polygon emblems.",
                "Mathematical curve plotting and dynamic vector lines."
        },
        criticalRules = {
                "VectorPath must be immutable or re-assigned to update visual geometry.",
                "strokeWidth applies when stroke() is enabled."
        },
        related = {"Shape", "VectorPath", "MutableColor"})
@WikiPage(
        title = "<key:path.widget.title>",
        slug = "path",
        category = "<key:core.widgets.display.category>",
        summary = "<key:path.widget.summary>",
        order = 80)
@WikiSection(
        title = "<key:path.widget.section.overview.title>",
        markdown = "<key:path.widget.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:path.widget.example.basic.title>",
        code = "<key:path.widget.example.basic.code>",
        order = 10)
@XmlWidgetName("Path")
public final class Path extends WidgetBase {
    public static final String STYLE_TYPE = StyleIds.Widget.PATH;

    public static final class StyleProperties {
        public static final String COLOR = StyleIds.Key.PATH_COLOR;
        public static final String STROKE = StyleIds.Key.PATH_STROKE;
        public static final String STROKE_WIDTH = StyleIds.Key.PATH_STROKE_WIDTH;

        private StyleProperties() {
        }
    }

    private final VectorPath path = new VectorPath();
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean stroke = true;
    private float strokeWidth = 1.0f;

    public Path() {
        path.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public VectorPath path() {
        return path;
    }

    public MutableColor color() {
        return color;
    }

    public boolean stroke() {
        return stroke;
    }

    @XmlAttribute(value = "stroke", category = "Appearance", defaultValue = "true", description = "Whether the vector path is stroked instead of filled.")
    @WikiMethod(
            title = "<key:path.widget.method.stroke.title>",
            summary = "<key:path.widget.method.stroke.summary>")
    public Path stroke(boolean stroke) {
        if (this.stroke == stroke) return this;
        this.stroke = stroke;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float strokeWidth() {
        return strokeWidth;
    }

    @XmlAttribute(value = "strokeWidth", category = "Appearance", defaultValue = "1", description = "Path stroke width in UI pixels.")
    @WikiMethod(
            title = "<key:path.widget.method.stroke_width.title>",
            summary = "<key:path.widget.method.stroke_width.summary>")
    public Path strokeWidth(float strokeWidth) {
        float normalized = Float.isFinite(strokeWidth) ? Math.max(0.0f, strokeWidth) : 0.0f;
        if (this.strokeWidth == normalized) return this;
        this.strokeWidth = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        if (path.isEmpty()) return;
        pushOpacity(context);
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderCustomVisual(draw)) {
                return;
            }
            boolean effectiveStroke = styleValue(StyleKeys.PATH_STROKE, styleState(), stroke);
            ColorView effectiveColor = styleValue(StyleKeys.PATH_COLOR, styleState(), color);
            Float styledWidth = styleValue(StyleKeys.PATH_STROKE_WIDTH, styleState(), strokeWidth);
            float effectiveWidth = styledWidth != null && Float.isFinite(styledWidth)
                    ? Math.max(0.0f, styledWidth) : strokeWidth;
            Paint paint = effectiveStroke
                    ? Paint.stroke(effectiveColor, effectiveWidth)
                    : Paint.fill(effectiveColor);
            draw.path(path, layoutBounds().x(), layoutBounds().y(),
                    layoutBounds().width(), layoutBounds().height(), paint);
        } finally {
            popOpacity(context);
        }
    }
}