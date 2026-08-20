package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;

/**
 * Описывает заливку текста, которую backend применяет к glyph'ам вместо обычного RGB-цвета.
 *
 * <p>Когда brush задан, его RGB считается основным цветом текста: {@link TextRun#color()} и paint color
 * больше не перемножают цвет заливки, а участвуют только своей alpha-компонентой. Это позволяет делать
 * чистые градиенты и будущие shader/text effects без случайного смешивания с цветом виджета.</p>
 *
 * <p>На backend'ах без поддержки brush значение деградирует до обычного цвета paint/run.
 * Minecraft backend поддерживает {@link LinearGradientTextBrush} для SDF и vanilla font path.</p>
 */
public interface TextBrush {
    /**
     * Создаёт независимую копию brush'а для immutable rich-text snapshot'ов.
     *
     * @return копия brush'а
     */
    TextBrush copy();

    /**
     * Создаёт solid brush. Обычно достаточно {@link TextRun#color()}, но этот вариант удобен
     * для API, где все варианты заливки представлены одним типом.
     */
    static TextBrush solid(ColorView color) {
        return new SolidTextBrush(color);
    }

    /**
     * Создаёт линейный градиент, растянутый по bounds текстовой команды.
     *
     * @param startColor цвет в начале направления
     * @param endColor цвет в конце направления
     * @param angleDegrees угол направления в градусах; {@code 0} идёт слева направо,
     *                     {@code 90} — сверху вниз
     * @return gradient brush
     */
    static TextBrush linearGradient(ColorView startColor, ColorView endColor, float angleDegrees) {
        return new LinearGradientTextBrush(startColor, endColor, angleDegrees);
    }

    /**
     * Разбирает XML/XAML-строку brush'а.
     *
     * <p>Поддерживаются {@code solid(#RRGGBB)}, bare {@code #RRGGBB} и
     * {@code linear-gradient(#RRGGBB, #RRGGBB, angle)}. Пустая строка и {@code none}
     * возвращают {@code null}, что сбрасывает brush.</p>
     *
     * @param value строковое описание brush'а
     * @return brush или {@code null}
     */
    static TextBrush parse(String value) {
        return TextBrushParser.parse(value);
    }

    /**
     * Делает immutable snapshot цвета, чтобы rich-text не зависел от дальнейших изменений MutableColor.
     */
    static ColorView snapshot(ColorView color) {
        return color == null ? null : new ColorSnapshot(color.r(), color.g(), color.b(), color.a());
    }

    /**
     * Делает snapshot цвета или возвращает opaque white как нейтральный цвет заливки.
     */
    static ColorView snapshotOrWhite(ColorView color) {
        return color == null ? new ColorSnapshot(1.0f, 1.0f, 1.0f, 1.0f) : snapshot(color);
    }

    /** Immutable snapshot цвета brush'а. */
    record ColorSnapshot(float r, float g, float b, float a) implements ColorView {
    }
}