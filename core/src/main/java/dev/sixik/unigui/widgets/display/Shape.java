package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.api.style.StyleAnimationIds;
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
        summary = "Vector geometric shape widget supporting rectangles, rounded rectangles, circles, ovals, and strokes.",
        purpose = "Renders hardware-accelerated 2D geometric primitives with antialiasing and customizable strokes.",
        useWhen = {
                "Drawing notification badges, circular avatars, or ornamental frames.",
                "Custom geometric accents without importing external PNG texture files."
        },
        criticalRules = {
                "type selects geometric primitive (RECTANGLE, ROUNDED_RECT, CIRCLE, OVAL).",
                "stroke toggles between outline drawing and filled geometry."
        },
        related = {"Path", "MutableColor", "WidgetBase"})
@WikiPage(
        title = "<key:shape.title>",
        slug = "shape",
        category = "<key:core.widgets.display.category>",
        summary = "<key:shape.summary>",
        order = 70)
@WikiSection(
        title = "<key:shape.section.overview.title>",
        markdown = "<key:shape.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:shape.example.basic.title>",
        code = "<key:shape.example.basic.code>",
        order = 10)
@XmlWidgetName("Shape")
public class Shape extends WidgetBase {
    public static final String STYLE_TYPE = StyleIds.Widget.SHAPE;

    public static final class StyleProperties {
        public static final String ACCENT_COLOR = StyleIds.Key.ACCENT_COLOR;
        public static final String COLOR = StyleIds.Key.SHAPE_COLOR;
        public static final String TYPE = StyleIds.Key.SHAPE_TYPE;
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;
        public static final String STROKE = StyleIds.Key.SHAPE_STROKE;

        private StyleProperties() {
        }
    }

    public static final class AnimationProperties {
        public static final String ACCENT_COLOR = StyleAnimationIds.Property.ACCENT_COLOR;
        public static final String BORDER_COLOR = StyleAnimationIds.Property.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleAnimationIds.Property.BORDER_WIDTH;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final java.util.List<String> ALL = java.util.List.of(ACCENT_COLOR, BORDER_COLOR, BORDER_WIDTH, RADIUS, OPACITY);

        private AnimationProperties() {
        }
    }

    private Type type = Type.RECT;
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean stroke;
    private float strokeWidth = 1.0f;
    private float radius;

    public Shape() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Type type() {
        return type;
    }

    @XmlAttribute(value = "type", category = "Appearance", defaultValue = "rect", description = "Shape primitive type.")
    @WikiMethod(
            title = "<key:shape.method.type.title>",
            summary = "<key:shape.method.type.summary>")
    public Shape type(Type type) {
        Type normalized = type == null ? Type.RECT : type;
        if (this.type == normalized) return this;
        this.type = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor color() {
        return color;
    }

    @XmlAttribute(value = "color", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Shape fill or stroke color parsed from XML color syntax.")
    @WikiMethod(
            title = "<key:shape.method.color.title>",
            summary = "<key:shape.method.color.summary>")
    public Shape color(ColorView color) {
        if (color != null) this.color.set(color);
        return this;
    }

    public boolean stroke() {
        return stroke;
    }

    @XmlAttribute(value = "stroke", category = "Appearance", defaultValue = "false", description = "Whether the shape is rendered as a stroke instead of a fill.")
    @WikiMethod(
            title = "<key:shape.method.stroke.title>",
            summary = "<key:shape.method.stroke.summary>")
    public Shape stroke(boolean stroke) {
        if (this.stroke == stroke) return this;
        this.stroke = stroke;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float strokeWidth() {
        return strokeWidth;
    }

    @XmlAttribute(value = "strokeWidth", category = "Appearance", defaultValue = "1", description = "Stroke width in UI pixels.")
    @WikiMethod(
            title = "<key:shape.method.stroke_width.title>",
            summary = "<key:shape.method.stroke_width.summary>")
    public Shape strokeWidth(float strokeWidth) {
        float normalized = Float.isFinite(strokeWidth) ? Math.max(0.0f, strokeWidth) : 0.0f;
        if (this.strokeWidth == normalized) return this;
        this.strokeWidth = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Corner radius for rounded rectangle rendering.")
    public Shape radius(float radius) {
        float normalized = Float.isFinite(radius) ? Math.max(0.0f, radius) : 0.0f;
        if (this.radius == normalized) return this;
        this.radius = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        pushOpacity(context);
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderCustomVisual(draw)) {
                return;
            }
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();
            dev.sixik.unigui.api.style.WidgetState state = styleState();
            String styledType = styleValue(StyleKeys.SHAPE_TYPE, state, null);
            Type effectiveType = parseType(styledType, type);
            boolean effectiveStroke = styleValue(StyleKeys.SHAPE_STROKE, state, stroke);
            ColorView effectiveFill = styleValue(StyleKeys.SHAPE_COLOR, state,
                    styleValue(StyleKeys.ACCENT_COLOR, state, color));
            ColorView effectiveBorder = styleValue(StyleKeys.BORDER_COLOR, state, null);
            ColorView effectiveColor = effectiveStroke
                    ? (effectiveBorder == null ? color : effectiveBorder)
                    : effectiveFill;
            Float styledWidth = styleValue(StyleKeys.BORDER_WIDTH, state, strokeWidth);
            Float styledRadius = styleValue(StyleKeys.RADIUS, state, radius);
            float effectiveWidth = styledWidth != null && Float.isFinite(styledWidth)
                    ? Math.max(0.0f, styledWidth) : strokeWidth;
            float effectiveRadius = styledRadius != null && Float.isFinite(styledRadius)
                    ? Math.max(0.0f, styledRadius) : radius;
            Paint paint = effectiveStroke
                    ? Paint.stroke(effectiveColor, effectiveWidth)
                    : Paint.fill(effectiveColor);
            switch (effectiveType) {
                case RECT -> draw.rect(x, y, width, height, paint);
                case ROUNDED_RECT -> draw.roundedRect(x, y, width, height, effectiveRadius, paint);
                case CIRCLE -> draw.circle(x, y, width, height, paint);
                case LINE -> draw.line(x, y, x + width, y + height, paint);
            }
        } finally {
            popOpacity(context);
        }
    }

    private static Type parseType(String value, Type fallback) {
        if (value == null || value.isBlank()) return fallback == null ? Type.RECT : fallback;
        String normalized = value.trim().replace('-', '_').replace('.', '_').toUpperCase(java.util.Locale.ROOT);
        try {
            return Type.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback == null ? Type.RECT : fallback;
        }
    }

    public enum Type {
        RECT,
        ROUNDED_RECT,
        CIRCLE,
        LINE
    }
}