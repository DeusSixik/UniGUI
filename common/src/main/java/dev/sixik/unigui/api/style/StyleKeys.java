package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;

/**
 * Стандартные типизированные ключи style-системы.
 *
 * <p>Ключи используются Java builders, StylePack XML и editor inspector'ом.
 * Строковые id лежат отдельно в {@link StyleIds.Key}, а этот класс добавляет Java-тип значения.</p>
 */
public final class StyleKeys {
    /**
     * Опциональный renderer override для типа виджета, которому принадлежит стиль.
     *
     * <p>Значение — {@link dev.sixik.unigui.api.widget.render.WidgetRender} или его
     * строковый id из registry. Renderer, назначенный прямо на instance виджета,
     * остаётся приоритетнее этого ключа.</p>
     */
    public static final StyleKey<Object> RENDERER = StyleKey.of(StyleIds.Key.RENDERER, Object.class);

    /** Источник фона поверхности: цвет, текстура или шейдер. */
    public static final StyleKey<BackgroundKind> BACKGROUND_KIND = StyleKey.of(StyleIds.Key.BACKGROUND_KIND, BackgroundKind.class);

    /** Цвет заливки фона базового прямоугольника виджета. */
    public static final StyleKey<ColorView> BACKGROUND_COLOR = StyleKey.of(StyleIds.Key.BACKGROUND_COLOR, ColorView.class);

    /** Текстура фона виджета. */
    public static final StyleKey<TextureHandle> BACKGROUND_TEXTURE = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE, TextureHandle.class);

    /** Tint-цвет фоновой текстуры. */
    public static final StyleKey<ColorView> BACKGROUND_TEXTURE_TINT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_TINT, ColorView.class);

    /** Режим подгонки фоновой текстуры в bounds виджета. */
    public static final StyleKey<ImageFit> BACKGROUND_TEXTURE_FIT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_FIT, ImageFit.class);

    /** Шейдер фона виджета. */
    public static final StyleKey<ShaderHandle> BACKGROUND_SHADER = StyleKey.of(StyleIds.Key.BACKGROUND_SHADER, ShaderHandle.class);

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