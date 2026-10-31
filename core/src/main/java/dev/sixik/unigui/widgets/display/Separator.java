package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Horizontal or vertical divider line widget for visually grouping interface sections.",
        purpose = "Demarcates visual boundaries between toolbar buttons, menu lists, or layout columns.",
        useWhen = {
                "Separating toolbar action groups.",
                "Dividing headers, body content, and footer toolbars."
        },
        criticalRules = {
                "orientation determines axis (HORIZONTAL expands width, VERTICAL expands height).",
                "thickness controls stroke line weight in pixels."
        },
        related = {"Box", "Orientation", "WidgetBase"})
@WikiPage(
        title = "<key:separator.title>",
        slug = "separator",
        category = "<key:core.widgets.display.category>",
        summary = "<key:separator.summary>",
        order = 60)
@WikiSection(
        title = "<key:separator.section.overview.title>",
        markdown = "<key:separator.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:separator.example.basic.title>",
        code = "<key:separator.example.basic.code>",
        order = 10)
@XmlWidgetName("Separator")
public final class Separator extends WidgetBase {
    public static final String STYLE_TYPE = StyleIds.Widget.SEPARATOR;

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

    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private Orientation orientation = Orientation.HORIZONTAL;
    private float thickness = 1.0f;

    public Separator() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    @WikiMethod(
            title = "<key:separator.method.color.title>",
            summary = "<key:separator.method.color.summary>")
    public MutableColor color() {
        return color;
    }

    public Orientation orientation() {
        return orientation;
    }

    @XmlAttribute(value = "orientation", category = "Layout", defaultValue = "horizontal", description = "Axis along which the separator is rendered.")
    @WikiMethod(
            title = "<key:separator.method.orientation.title>",
            summary = "<key:separator.method.orientation.summary>")
    public Separator orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float thickness() {
        return thickness;
    }

    @XmlAttribute(value = "thickness", category = "Appearance", defaultValue = "1", description = "Separator thickness in UI pixels.")
    @WikiMethod(
            title = "<key:separator.method.thickness.title>",
            summary = "<key:separator.method.thickness.summary>")
    public Separator thickness(float thickness) {
        if (this.thickness == thickness) return this;
        this.thickness = thickness;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
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
            boolean horizontal = orientation != Orientation.VERTICAL;
            float width = horizontal ? layoutBounds().width() : thickness;
            float height = horizontal ? thickness : layoutBounds().height();
            draw.rect(layoutBounds().x(), layoutBounds().y(), width, height, Paint.fill(color));
        } finally {
            popOpacity(context);
        }
    }
}