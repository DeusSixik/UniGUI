package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.ShapeRenderer;
import dev.sixik.unigui.widgets.render.ShapeState;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

@XmlWidgetName("Shape")
public class Shape extends WidgetBase {
    public static final String STYLE_TYPE = StyleIds.Widget.SHAPE;

    public static final class StyleProperties {
        public static final String ACCENT_COLOR = StyleIds.Key.ACCENT_COLOR;
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;

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
    private ShapeRenderer renderer;
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
    public Shape color(ColorView color) {
        if (color != null) this.color.set(color);
        return this;
    }

    public ShapeRenderer renderer() {
        return renderer;
    }

    public Shape renderer(ShapeRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Shape useDefaultRenderer() {
        return renderer(null);
    }

    public boolean stroke() {
        return stroke;
    }

    @XmlAttribute(value = "stroke", category = "Appearance", defaultValue = "false", description = "Whether the shape is rendered as a stroke instead of a fill.")
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
    public Shape strokeWidth(float strokeWidth) {
        if (this.strokeWidth == strokeWidth) return this;
        this.strokeWidth = strokeWidth;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Corner radius for rounded rectangle rendering.")
    public Shape radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        pushOpacity(context);
        try {
            ShapeState state = snapshot();
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderer != null) {
                renderer.render(draw, state);
                return;
            }
            ShapeRenderer styled = styleRendererOverride(ShapeRenderer.class);
            if (styled != null) {
                styled.render(draw, state);
                return;
            }
            if (renderStylePlan(context, ShapeState.class, state)) return;
            WidgetsRender.shape().render(draw, state);
        } finally {
            popOpacity(context);
        }
    }

    protected ShapeRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(ShapeRenderer.class, WidgetsRender.shape()) : renderer;
    }

    protected ShapeState snapshot() {
        return new ShapeState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                type,
                color.copy(),
                stroke,
                strokeWidth,
                radius);
    }

    public enum Type {
        RECT,
        ROUNDED_RECT,
        CIRCLE,
        LINE
    }
}
