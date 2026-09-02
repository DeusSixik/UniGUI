package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;

/**
 * Стандартные типизированные ключи style-системы.
 *
 * <p>Ключи используются Java builders, StylePack XML, RenderPlan builders и editor inspector'ом.
 * Строковые id лежат отдельно в {@link StyleIds.Key}, а этот класс добавляет Java-тип значения.</p>
 */
public final class StyleKeys {
    /**
     * Опциональный renderer override для типа виджета, которому принадлежит стиль.
     *
     * <p>Значение намеренно имеет тип {@link Object}: каждый виджет приводит его к своему
     * конкретному renderer-интерфейсу. Renderer, назначенный прямо на instance виджета,
     * остаётся приоритетнее этого ключа; ключ заменяет только дефолтный renderer из WidgetsRender.</p>
     */
    public static final StyleKey<Object> RENDERER = StyleKey.of(StyleIds.Key.RENDERER, Object.class);

    /** Цвет заливки фона базового прямоугольника виджета. */
    public static final StyleKey<ColorView> BACKGROUND_COLOR = StyleKey.of(StyleIds.Key.BACKGROUND_COLOR, ColorView.class);

    /** Текстура фона виджета. */
    public static final StyleKey<TextureHandle> BACKGROUND_TEXTURE = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE, TextureHandle.class);

    /** Tint-цвет фоновой текстуры. */
    public static final StyleKey<ColorView> BACKGROUND_TEXTURE_TINT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_TINT, ColorView.class);

    /** Режим подгонки фоновой текстуры в bounds виджета. */
    public static final StyleKey<ImageFit> BACKGROUND_TEXTURE_FIT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_FIT, ImageFit.class);

    /** Цвет рамки виджета. */
    public static final StyleKey<ColorView> BORDER_COLOR = StyleKey.of(StyleIds.Key.BORDER_COLOR, ColorView.class);

    /** Основной цвет текста. */
    public static final StyleKey<ColorView> TEXT_COLOR = StyleKey.of(StyleIds.Key.TEXT_COLOR, ColorView.class);

    /** Цвет placeholder-текста в input-like виджетах. */
    public static final StyleKey<ColorView> PLACEHOLDER_COLOR = StyleKey.of(StyleIds.Key.PLACEHOLDER_COLOR, ColorView.class);

    /** Акцентный цвет для selected/focused/active частей виджета. */
    public static final StyleKey<ColorView> ACCENT_COLOR = StyleKey.of(StyleIds.Key.ACCENT_COLOR, ColorView.class);

    /** Цвет track-части slider/progress/scrollbar виджетов. */
    public static final StyleKey<ColorView> TRACK_COLOR = StyleKey.of(StyleIds.Key.TRACK_COLOR, ColorView.class);

    /** Цвет thumb/handle-части slider/scrollbar виджетов. */
    public static final StyleKey<ColorView> THUMB_COLOR = StyleKey.of(StyleIds.Key.THUMB_COLOR, ColorView.class);

    /** Толщина рамки в UI-пикселях. */
    public static final StyleKey<Float> BORDER_WIDTH = StyleKey.of(StyleIds.Key.BORDER_WIDTH, Float.class);

    /** Радиус скругления в UI-пикселях. */
    public static final StyleKey<Float> RADIUS = StyleKey.of(StyleIds.Key.RADIUS, Float.class);

    /** Длительность автоматического перехода style-свойств в секундах. Ноль отключает переходы. */
    public static final StyleKey<Float> TRANSITION_DURATION = StyleKey.of(StyleIds.Key.TRANSITION_DURATION, Float.class);

    /** Стандартная easing-кривая автоматического перехода style-свойств. */
    public static final StyleKey<dev.sixik.unigui.api.animation.AnimationEasing> TRANSITION_EASING =
            StyleKey.of(StyleIds.Key.TRANSITION_EASING, dev.sixik.unigui.api.animation.AnimationEasing.class);

    private StyleKeys() {
    }
}