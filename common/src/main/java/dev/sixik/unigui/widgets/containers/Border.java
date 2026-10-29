package dev.sixik.unigui.widgets.containers;

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
    public Border() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает изменяемый цвет рамки.
     *
     * @return live-цвет; его можно менять без повторного вызова setter'а
     */
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
    public Border thickness(float thickness) {
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
    public Border radius(float radius) {
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
