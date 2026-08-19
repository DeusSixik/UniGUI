package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.BorderRenderer;
import dev.sixik.unigui.widgets.render.BorderState;
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
 * @see BorderRenderer
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
    private BorderRenderer renderer;
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
     * Возвращает renderer, заданный напрямую для этой рамки.
     *
     * @return кастомный renderer или {@code null}, если используется theme/default renderer
     */
    public BorderRenderer renderer() {
        return renderer;
    }

    /**
     * Задаёт renderer рамки.
     *
     * <p>{@code null} возвращает поведение к renderer'у из темы или стандартному
     * {@link WidgetsRender#border()}.</p>
     *
     * @param renderer renderer рамки или {@code null}
     * @return эта рамка для fluent-настройки
     */
    public Border renderer(BorderRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Сбрасывает кастомный renderer и снова использует renderer из темы/default.
     *
     * @return эта рамка для fluent-настройки
     */
    public Border useDefaultRenderer() {
        return renderer(null);
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
            BorderState state = snapshot();
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderer != null) {
                renderer.render(draw, state);
                return;
            }
            BorderRenderer styled = styleRendererOverride(BorderRenderer.class);
            if (styled != null) {
                styled.render(draw, state);
                return;
            }
            if (renderStylePlan(context, BorderState.class, state)) return;
            WidgetsRender.border().render(draw, state);
        } finally {
            popOpacity(context);
        }
    }

    private BorderRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(BorderRenderer.class, WidgetsRender.border()) : renderer;
    }

    private BorderState snapshot() {
        return new BorderState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                color.copy(),
                thickness,
                radius);
    }
}
