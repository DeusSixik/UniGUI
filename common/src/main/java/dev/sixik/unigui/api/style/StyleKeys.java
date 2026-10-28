package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureGroup;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.text.TextTransform;

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

    /**
     * Источник фона поверхности: цвет, текстура или шейдер.
     */
    public static final StyleKey<BackgroundKind> BACKGROUND_KIND = StyleKey.of(StyleIds.Key.BACKGROUND_KIND, BackgroundKind.class);

    /**
     * Цвет заливки фона базового прямоугольника виджета.
     */
    public static final StyleKey<ColorView> BACKGROUND_COLOR = StyleKey.of(StyleIds.Key.BACKGROUND_COLOR, ColorView.class);

    /**
     * Текстура фона виджета.
     */
    public static final StyleKey<TextureHandle> BACKGROUND_TEXTURE = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE, TextureHandle.class);

    /**
     * Упорядоченная группа текстурных слоёв фоновой поверхности.
     */
    public static final StyleKey<TextureGroup> BACKGROUND_TEXTURE_GROUP =
            StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_GROUP, TextureGroup.class);

    /**
     * Tint-цвет фоновой текстуры.
     */
    public static final StyleKey<ColorView> BACKGROUND_TEXTURE_TINT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_TINT, ColorView.class);

    /**
     * Режим подгонки фоновой текстуры в bounds виджета.
     */
    public static final StyleKey<ImageFit> BACKGROUND_TEXTURE_FIT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_FIT, ImageFit.class);

    /**
     * Рамки фоновой текстуры для режима nine-slice.
     */
    public static final StyleKey<SliceBorders> SLICE_BORDERS = StyleKey.of(StyleIds.Key.SLICE_BORDERS, SliceBorders.class);

    /**
     * Признак отрисовки фоновой поверхности.
     */
    public static final StyleKey<Boolean> BACKGROUND_VISIBLE = StyleKey.of(StyleIds.Key.BACKGROUND_VISIBLE, Boolean.class);

    /**
     * Шейдер фона виджета.
     */
    public static final StyleKey<ShaderHandle> BACKGROUND_SHADER = StyleKey.of(StyleIds.Key.BACKGROUND_SHADER, ShaderHandle.class);

    /**
     * Цвет рамки виджета.
     */
    public static final StyleKey<ColorView> BORDER_COLOR = StyleKey.of(StyleIds.Key.BORDER_COLOR, ColorView.class);

    /**
     * Признак отрисовки рамки поверхности.
     */
    public static final StyleKey<Boolean> BORDER_VISIBLE = StyleKey.of(StyleIds.Key.BORDER_VISIBLE, Boolean.class);

    /**
     * Основной цвет текста.
     */
    public static final StyleKey<ColorView> TEXT_COLOR = StyleKey.of(StyleIds.Key.TEXT_COLOR, ColorView.class);

    /**
     * Размер текста в UI-пикселях для текстовых контролов.
     */
    public static final StyleKey<Float> TEXT_SIZE = StyleKey.of(StyleIds.Key.TEXT_SIZE, Float.class);

    /**
     * Дополнительный межсимвольный интервал в UI-пикселях.
     */
    public static final StyleKey<Float> TEXT_TRACKING = StyleKey.of(StyleIds.Key.TEXT_TRACKING, Float.class);

    /**
     * Преобразование текста перед измерением и отрисовкой.
     */
    public static final StyleKey<TextTransform> TEXT_TRANSFORM = StyleKey.of(StyleIds.Key.TEXT_TRANSFORM, TextTransform.class);

    /**
     * Цвет заливки индикатора Checkbox для каждого состояния.
     */
    public static final StyleKey<ColorView> CHECKBOX_BACKGROUND_COLOR =
            StyleKey.of(StyleIds.Key.CHECKBOX_BACKGROUND_COLOR, ColorView.class);

    /**
     * Цвет placeholder-текста в input-like виджетах.
     */
    public static final StyleKey<ColorView> PLACEHOLDER_COLOR = StyleKey.of(StyleIds.Key.PLACEHOLDER_COLOR, ColorView.class);

    /**
     * Цвет каретки однострочных и многострочных текстовых редакторов.
     */
    public static final StyleKey<ColorView> CARET_COLOR = StyleKey.of(StyleIds.Key.CARET_COLOR, ColorView.class);

    /**
     * Цвет внешнего кольца фокуса текстового поля.
     */
    public static final StyleKey<ColorView> FOCUS_RING_COLOR = StyleKey.of(StyleIds.Key.FOCUS_RING_COLOR, ColorView.class);

    /**
     * Акцентный цвет для selected/focused/active частей виджета.
     */
    public static final StyleKey<ColorView> ACCENT_COLOR = StyleKey.of(StyleIds.Key.ACCENT_COLOR, ColorView.class);

    /**
     * Цвет track-части slider/progress/scrollbar виджетов.
     */
    public static final StyleKey<ColorView> TRACK_COLOR = StyleKey.of(StyleIds.Key.TRACK_COLOR, ColorView.class);

    /**
     * Цвет thumb/handle-части slider/scrollbar виджетов.
     */
    public static final StyleKey<ColorView> THUMB_COLOR = StyleKey.of(StyleIds.Key.THUMB_COLOR, ColorView.class);

    /**
     * Радиус скругления бегунка в UI-пикселях; половина размера образует круг.
     */
    public static final StyleKey<Float> THUMB_RADIUS = StyleKey.of(StyleIds.Key.THUMB_RADIUS, Float.class);

    /**
     * Толщина дорожки ползунка в логических пикселях.
     */
    public static final StyleKey<Float> TRACK_THICKNESS = StyleKey.of(StyleIds.Key.TRACK_THICKNESS, Float.class);

    /**
     * Ширина бегунка Slider либо толщина тамба ScrollBar в логических пикселях.
     */
    public static final StyleKey<Float> THUMB_THICKNESS = StyleKey.of(StyleIds.Key.THUMB_THICKNESS, Float.class);

    /**
     * Цвет заливки прогресса HoldButton.
     */
    public static final StyleKey<ColorView> HOLD_COLOR = StyleKey.of(StyleIds.Key.HOLD_COLOR, ColorView.class);

    /**
     * Внешний размер индикатора RadioButton в логических пикселях.
     */
    public static final StyleKey<Float> RADIO_OUTER_SIZE = StyleKey.of(StyleIds.Key.RADIO_OUTER_SIZE, Float.class);

    /**
     * Размер внутренней отметки RadioButton в логических пикселях.
     */
    public static final StyleKey<Float> RADIO_INNER_SIZE = StyleKey.of(StyleIds.Key.RADIO_INNER_SIZE, Float.class);

    /**
     * Зазор между индикатором и подписью RadioButton в логических пикселях.
     */
    public static final StyleKey<Float> RADIO_TEXT_GAP = StyleKey.of(StyleIds.Key.RADIO_TEXT_GAP, Float.class);

    /**
     * Толщина рамки в UI-пикселях.
     */
    public static final StyleKey<Float> BORDER_WIDTH = StyleKey.of(StyleIds.Key.BORDER_WIDTH, Float.class);

    /**
     * Радиус скругления в UI-пикселях.
     */
    public static final StyleKey<Float> RADIUS = StyleKey.of(StyleIds.Key.RADIUS, Float.class);

    /**
     * Признак отрисовки Shape как обводки вместо заливки.
     */
    public static final StyleKey<Boolean> SHAPE_STROKE = StyleKey.of(StyleIds.Key.SHAPE_STROKE, Boolean.class);

    /**
     * Цвет заливки фигуры Shape.
     */
    public static final StyleKey<ColorView> SHAPE_COLOR = StyleKey.of(StyleIds.Key.SHAPE_COLOR, ColorView.class);

    /**
     * Тип геометрического примитива Shape: rect, rounded-rect, circle или line.
     */
    public static final StyleKey<String> SHAPE_TYPE = StyleKey.of(StyleIds.Key.SHAPE_TYPE, String.class);

    /**
     * Цвет заливки или обводки векторного Path.
     */
    public static final StyleKey<ColorView> PATH_COLOR = StyleKey.of(StyleIds.Key.PATH_COLOR, ColorView.class);

    /**
     * Признак отрисовки Path как обводки вместо заливки.
     */
    public static final StyleKey<Boolean> PATH_STROKE = StyleKey.of(StyleIds.Key.PATH_STROKE, Boolean.class);

    /**
     * Толщина обводки векторного Path.
     */
    public static final StyleKey<Float> PATH_STROKE_WIDTH = StyleKey.of(StyleIds.Key.PATH_STROKE_WIDTH, Float.class);

    /**
     * Длительность автоматического перехода style-свойств в секундах. Ноль отключает переходы.
     */
    public static final StyleKey<Float> TRANSITION_DURATION = StyleKey.of(StyleIds.Key.TRANSITION_DURATION, Float.class);

    /**
     * Стандартная easing-кривая автоматического перехода style-свойств.
     */
    public static final StyleKey<AnimationEasing> TRANSITION_EASING =
            StyleKey.of(StyleIds.Key.TRANSITION_EASING, AnimationEasing.class);

    private StyleKeys() {
    }
}
