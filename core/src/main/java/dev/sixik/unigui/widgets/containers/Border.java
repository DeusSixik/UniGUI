package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;

/**
 * Render-only рамка без дочерних виджетов.
 *
 * <p>{@code Border} полезен как самостоятельный декоративный слой: например,
 * когда нужно отрисовать outline поверх другого контейнера или подсветить
 * bounds элемента. Если нужны фон, рамка и вложенный контент в одном виджете,
 * обычно удобнее использовать {@link Box}.</p>
 *
 * <p>Цвет хранится как live {@link MutableColor}: изменение компонентов цвета
 * автоматически инвалидирует визуальное состояние.</p>
 *
 * @see Box
 */
@AiDoc(
        summary = "Render-only decorative outline widget without child widgets.",
        purpose = "Draws a rectangular stroke with optional corner radius. Designed for overlay highlights and borders.",
        useWhen = {
                "Draw an outline overlay over another component (e.g. selection indicator, drag target).",
                "Highlight widget boundaries without modifying their internal layout."
        },
        criticalRules = {
                "Border does not support child widgets: use Box when nested content is needed.",
                "color() returns a live MutableColor that automatically invalidates visual state upon modification."
        },
        related = {"Box", "SurfaceWidget", "WidgetBase"},
        examples = @AiDoc.Example(
                title = "Highlight border",
                code = "Border highlight = new Border().thickness(2.0f).radius(4.0f);"))
@WikiPage(
        title = "<key:border.title>",
        slug = "border",
        category = "<key:border.category>",
        summary = "<key:border.summary>",
        order = 170)
@WikiSection(title = "<key:border.section.overview.title>",
        markdown = "<key:border.section.overview.body>", order = 10)
@WikiExample(title = "<key:border.example.outline.title>",
        code = "<key:border.example.outline.code>", order = 10)
@WikiLink(label = "Box", target = "page:box")
public final class Border extends WidgetBase {
    public static final String STYLE_TYPE = StyleIds.Widget.BORDER;

    public static final class StyleProperties {
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;

        private StyleProperties() {
        }
    }

    public static final class AnimationProperties {
        public static final String BORDER_COLOR = StyleAnimationIds.Property.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleAnimationIds.Property.BORDER_WIDTH;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final java.util.List<String> ALL = java.util.List.of(BORDER_COLOR, BORDER_WIDTH, RADIUS, OPACITY);

        private AnimationProperties() {
        }
    }

    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private float thickness = 1.0f;
    private float radius;

    /**
     * Создаёт белую рамку толщиной {@code 1px} без скругления.
     */
    @WikiMethod(order = 10, title = "<key:border.method.ctor.title>", summary = "<key:border.method.ctor.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 10, order = 10, summary = "Creates a border decorator with default 1px white border.")
    public Border() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает изменяемый цвет рамки.
     *
     * @return live-цвет; его можно менять без повторного вызова setter'а
     */
    @WikiMethod(order = 20, title = "<key:border.method.color.title>", summary = "<key:border.method.color.summary>")
    @WikiReturn("<key:border.method.color.return>")
    @AiDoc(section = "Appearance", sectionOrder = 20, order = 20, summary = "Returns live mutable border color.")
    public MutableColor color() {
        return color;
    }

    /**
     * Возвращает толщину линии рамки в пикселях UI-пространства.
     *
     * @return текущая толщина рамки
     */
    public float thickness() {
        return thickness;
    }

    /**
     * Задаёт толщину линии рамки.
     *
     * @param thickness новая толщина в пикселях UI-пространства
     * @return эта рамка для fluent-настройки
     */
    @WikiMethod(order = 30, title = "<key:border.method.thickness.title>", summary = "<key:border.method.thickness.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Appearance", sectionOrder = 20, order = 30, summary = "Sets border stroke thickness in UI pixels.")
    public Border thickness(@WikiParameter(description = "<key:border.method.thickness.param.thickness>") float thickness) {
        float normalized = Float.isFinite(thickness) ? Math.max(0.0f, thickness) : 0.0f;
        if (this.thickness == normalized) return this;
        this.thickness = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает радиус скругления углов.
     *
     * @return радиус скругления в пикселях UI-пространства
     */
    public float radius() {
        return radius;
    }

    /**
     * Задаёт радиус скругления углов.
     *
     * @param radius радиус скругления в пикселях UI-пространства
     * @return эта рамка для fluent-настройки
     */
    @WikiMethod(order = 40, title = "<key:border.method.radius.title>", summary = "<key:border.method.radius.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Appearance", sectionOrder = 20, order = 40, summary = "Sets border corner rounding radius.")
    public Border radius(@WikiParameter(description = "<key:border.method.radius.param.radius>") float radius) {
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
            if (!styleValue(StyleKeys.BORDER_VISIBLE, styleState(), true)) {
                return;
            }
            ColorView effectiveColor = styleValue(StyleKeys.BORDER_COLOR, styleState(), color);
            Float styledWidth = styleValue(StyleKeys.BORDER_WIDTH, styleState(), thickness);
            Float styledRadius = styleValue(StyleKeys.RADIUS, styleState(), radius);
            float effectiveWidth = styledWidth != null && Float.isFinite(styledWidth)
                    ? Math.max(0.0f, styledWidth) : thickness;
            float effectiveRadius = styledRadius != null && Float.isFinite(styledRadius)
                    ? Math.max(0.0f, styledRadius) : radius;
            draw.roundedRect(layoutBounds().x(), layoutBounds().y(),
                    layoutBounds().width(), layoutBounds().height(), effectiveRadius,
                    Paint.stroke(effectiveColor, effectiveWidth));
        } finally {
            popOpacity(context);
        }
    }
}
