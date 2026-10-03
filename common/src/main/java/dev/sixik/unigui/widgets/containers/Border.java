package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

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
        if (this.thickness == thickness) return this;
        this.thickness = thickness;
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
        if (this.radius == radius) return this;
        this.radius = radius;
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
            draw.roundedRect(layoutBounds().x(), layoutBounds().y(),
                    layoutBounds().width(), layoutBounds().height(), radius,
                    Paint.stroke(color, thickness));
        } finally {
            popOpacity(context);
        }
    }
}
