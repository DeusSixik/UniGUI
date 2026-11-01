package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;

import java.util.Locale;

/**
 * Общие вычисления и отрисовка виджетов данных: «красивая» шкала осей, форматирование чисел, подсказка и поверхность.
 *
 * <p>Класс не хранит состояния и не выделяет память в горячих методах, кроме {@link #drawTooltip} (вызывается только
 * для наведённого элемента) и форматирования строк (вызывающий код кэширует результат).</p>
 */
public final class PlotSupport {
    /**
     * Горизонтальный внутренний отступ подсказки в UI-пикселях.
     */
    public static final float TOOLTIP_PADDING_X = 5.0f;

    /**
     * Вертикальный внутренний отступ подсказки в UI-пикселях.
     */
    public static final float TOOLTIP_PADDING_Y = 3.0f;

    /**
     * Радиус скругления подсказки в UI-пикселях.
     */
    public static final float TOOLTIP_RADIUS = 3.0f;

    /**
     * Допуск сравнения «значения совпадают» для вырожденного диапазона данных.
     */
    private static final float DEGENERATE_RANGE = 1.0e-6f;

    private PlotSupport() {
    }

    /**
     * Ограничивает значение отрезком.
     *
     * <p>Если {@code max < min} (отрезок вырожден), возвращается {@code min}: это нужно, когда подсказка шире
     * виджета и её нужно прижать к левому краю.</p>
     *
     * @param value проверяемое значение
     * @param min   нижняя граница
     * @param max   верхняя граница
     * @return {@code value}, прижатое к {@code [min, max]}
     */
    public static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Подбирает «красивое» число для шага оси (алгоритм Хекберта, Graphics Gems).
     *
     * <p>Формула:
     * <pre>
     *   e = floor(log10(x)),  f = x / 10^e
     *   round = true:   f &lt; 1.5 -&gt; 1,  f &lt; 3 -&gt; 2,  f &lt; 7 -&gt; 5,  иначе 10
     *   round = false:  f &le; 1  -&gt; 1,  f &le; 2 -&gt; 2,  f &le; 5 -&gt; 5,  иначе 10
     *   result = (nice f) * 10^e
     * </pre>
     *
     * <p>Где {@code x} — исходная величина, {@code e} — порядок, {@code f} — мантисса в [1, 10).</p>
     *
     * @param value положительная величина, для которой ищется красивое число
     * @param round {@code true} — округлять к ближайшему красивому, {@code false} — брать не меньше исходного
     * @return красивое число вида {1, 2, 5, 10} * 10^e; {@code 1}, если {@code value} не положительно или не конечно
     */
    public static float niceNumber(float value, boolean round) {
        if (!(value > 0.0f) || !Float.isFinite(value)) return 1.0f;
        double exponent = Math.floor(Math.log10(value));
        double power = Math.pow(10.0, exponent);
        double fraction = value / power;
        double nice;
        if (round) {
            if (fraction < 1.5) nice = 1.0;
            else if (fraction < 3.0) nice = 2.0;
            else if (fraction < 7.0) nice = 5.0;
            else nice = 10.0;
        } else {
            if (fraction <= 1.0) nice = 1.0;
            else if (fraction <= 2.0) nice = 2.0;
            else if (fraction <= 5.0) nice = 5.0;
            else nice = 10.0;
        }
        return (float) (nice * power);
    }

    /**
     * Строит шкалу оси: расширяет диапазон данных до границ, кратных красивому шагу.
     *
     * <p>Формулы:
     * <pre>
     *   range   = niceNumber(max - min, false)
     *   step    = niceNumber(range / (maxTicks - 1), true)
     *   niceMin = floor(min / step) * step
     *   niceMax = ceil (max / step) * step
     * </pre>
     *
     * <p>Вырожденный диапазон ({@code max - min} около нуля) раскрывается на {@code max(1, 10% от |значения|)}
     * в обе стороны, чтобы ряд из одинаковых значений рисовался по центру, а не прижатым к низу.</p>
     *
     * @param min      минимум данных; не конечное значение трактуется как {@code 0}
     * @param max      максимум данных; не конечное значение трактуется как {@code 1}
     * @param maxTicks желаемое максимальное число делений, не меньше 2
     * @param out      массив минимум из трёх элементов: {@code [niceMin, niceMax, step]}
     */
    public static void niceScale(float min, float max, int maxTicks, float[] out) {
        float low = Float.isFinite(min) ? min : 0.0f;
        float high = Float.isFinite(max) ? max : 1.0f;
        if (high < low) {
            float swap = low;
            low = high;
            high = swap;
        }
        if (high - low < DEGENERATE_RANGE) {
            float spread = Math.max(1.0f, Math.abs(low) * 0.1f);
            low -= spread;
            high += spread;
        }
        int ticks = Math.max(2, maxTicks);
        float range = niceNumber(high - low, false);
        float step = niceNumber(range / (ticks - 1), true);
        out[0] = (float) (Math.floor(low / (double) step) * step);
        out[1] = (float) (Math.ceil(high / (double) step) * step);
        out[2] = step;
    }

    /**
     * Форматирует число для подписи: целые без дробной части, остальные не более чем с двумя знаками.
     *
     * @param value форматируемое значение
     * @return строка без хвостовых нулей; {@code "NaN"} и бесконечности выводятся как у {@link Float#toString(float)}
     */
    public static String formatValue(float value) {
        if (!Float.isFinite(value)) return Float.toString(value);
        if (Math.abs(value) < 1.0e15f && value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        String text = String.format(Locale.ROOT, "%.2f", value);
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') end--;
        if (end > 0 && text.charAt(end - 1) == '.') end--;
        String trimmed = text.substring(0, end);
        // Малое отрицательное значение округляется до «-0.00»: показываем просто 0.
        return "-0".equals(trimmed) ? "0" : trimmed;
    }

    /**
     * Форматирует значение деления оси с числом знаков, достаточным для шага шкалы.
     *
     * <p>Число знаков {@code d} — наименьшее из {@code 0..6}, при котором шаг, умноженный на {@code 10^d}, целый
     * (с допуском {@code 1e-3}):
     * <pre>
     *   d = min { d in [0, 6] : | step * 10^d - round(step * 10^d) | &lt; 1e-3 }
     * </pre>
     * Так шаг 0.25 даёт подписи {@code 0.00, 0.25, 0.50}, шаг 10 — {@code 0, 10, 20}, а накопленная ошибка float
     * ({@code 0.30000001}) не попадает в подпись.
     *
     * @param value значение деления
     * @param step  шаг шкалы; должен быть положительным (иначе знаков нет)
     * @return подпись деления; отрицательный ноль выводится как {@code 0}
     */
    public static String formatTick(float value, float step) {
        if (!Float.isFinite(value)) return Float.toString(value);
        int decimals = 0;
        if (step > 0.0f && Float.isFinite(step)) {
            double scaled = step;
            while (decimals < 6 && Math.abs(scaled - Math.rint(scaled)) >= 1.0e-3) {
                decimals++;
                scaled *= 10.0;
            }
        }
        String text = String.format(Locale.ROOT, "%." + decimals + "f", value);
        if (text.charAt(0) == '-') {
            boolean zero = true;
            for (int i = 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c != '0' && c != '.') {
                    zero = false;
                    break;
                }
            }
            if (zero) text = text.substring(1);
        }
        return text;
    }

    /**
     * Создаёт одностильный rich-text заданного размера шрифтом по умолчанию.
     *
     * @param text      текст; {@code null} трактуется как пустая строка
     * @param pixelSize размер шрифта в UI-пикселях
     * @return rich-text для замера и отрисовки
     */
    public static RichText text(String text, float pixelSize) {
        return RichText.of(text == null ? "" : text, null, pixelSize);
    }

    /**
     * Измеряет ширину строки в UI-пикселях тем же способом, каким она будет нарисована.
     *
     * @param context контекст отрисовки (даёт метрики шрифта бэкенда)
     * @param text    измеряемый текст
     * @return ширина строки; {@code 0} для пустого текста
     */
    public static float textWidth(RenderContext context, RichText text) {
        return text == null || text.isEmpty() ? 0.0f : TextEngine.measureLineWidth(context, text);
    }

    /**
     * Измеряет высоту текста в UI-пикселях тем же способом, каким он будет нарисован.
     *
     * @param context контекст отрисовки (даёт метрики шрифта бэкенда)
     * @param text    измеряемый текст
     * @return высота текста; {@code 0} для пустого текста
     */
    public static float textHeight(RenderContext context, RichText text) {
        return text == null || text.isEmpty() ? 0.0f : TextEngine.measureTextHeight(context, text);
    }

    /**
     * Рисует текст в прямоугольнике с выравниванием.
     *
     * @param draw  область рисования
     * @param text  текст
     * @param x     левый край прямоугольника
     * @param y     верхний край прямоугольника
     * @param width ширина прямоугольника
     * @param height высота прямоугольника
     * @param color цвет текста
     * @param horizontal выравнивание по горизонтали
     * @param vertical   выравнивание по вертикали
     */
    public static void drawText(DrawScope draw, RichText text, float x, float y, float width, float height,
                                ColorView color, Alignment horizontal, Alignment vertical) {
        if (text == null || text.isEmpty() || width <= 0.0f || height <= 0.0f) return;
        TextEngine.draw(draw.context(), text, x, y, width, height, Paint.fill(color), draw.transform(),
                horizontal, vertical);
    }

    /**
     * Рисует фон и рамку виджета.
     *
     * <p>Рамка рисуется внутри границ: контур смещён на половину толщины, чтобы не выходить за прямоугольник
     * виджета и не обрезаться родителем.</p>
     *
     * @param draw          область рисования
     * @param bounds        границы виджета
     * @param background    цвет фона или {@code null}, если фон не нужен
     * @param border        цвет рамки или {@code null}, если рамка не нужна
     * @param borderWidth   толщина рамки в UI-пикселях
     * @param radius        радиус скругления в UI-пикселях
     */
    public static void drawSurface(DrawScope draw, RectView bounds, ColorView background, ColorView border,
                                   float borderWidth, float radius) {
        if (background != null) {
            draw.roundedRectFilled(bounds.x(), bounds.y(), bounds.width(), bounds.height(), radius, background);
        }
        if (border != null && borderWidth > 0.0f) {
            float half = borderWidth * 0.5f;
            draw.roundedRectStroke(bounds.x() + half, bounds.y() + half,
                    Math.max(0.0f, bounds.width() - borderWidth), Math.max(0.0f, bounds.height() - borderWidth),
                    Math.max(0.0f, radius - half), border, borderWidth);
        }
    }

    /**
     * Рисует подсказку рядом с точкой привязки и прижимает её к границам виджета.
     *
     * <p>Размещение, по порядку предпочтения:
     * <ol>
     *   <li>над точкой по центру: {@code y = anchorY - gap - h};</li>
     *   <li>если сверху нет места — под точкой: {@code y = anchorY + gap};</li>
     *   <li>если нет места и снизу — сбоку от точки (справа, а при нехватке места слева), по центру точки по высоте.</li>
     * </ol>
     * Горизонтальная координата ограничивается {@code [bounds.x, bounds.right - w]}; если подсказка шире виджета,
     * она прижимается к левому краю.
     *
     * <p>Размеры: {@code w = textWidth + 2 * TOOLTIP_PADDING_X}, {@code h = textHeight + 2 * TOOLTIP_PADDING_Y}.</p>
     *
     * @param draw       область рисования
     * @param text       текст подсказки (замеряется и рисуется одним и тем же размером шрифта)
     * @param anchorX    X точки привязки в координатах виджета-рендера
     * @param anchorY    Y точки привязки
     * @param gap        зазор между точкой и подсказкой в UI-пикселях
     * @param bounds     границы виджета, внутри которых размещается подсказка
     * @param background цвет фона подсказки
     * @param border     цвет рамки подсказки
     * @param textColor  цвет текста
     */
    public static void drawTooltip(DrawScope draw, RichText text, float anchorX, float anchorY, float gap,
                                   RectView bounds, ColorView background, ColorView border, ColorView textColor) {
        if (text == null || text.isEmpty()) return;
        RenderContext context = draw.context();
        float textW = textWidth(context, text);
        float textH = textHeight(context, text);
        float width = textW + TOOLTIP_PADDING_X * 2.0f;
        float height = textH + TOOLTIP_PADDING_Y * 2.0f;

        float left = bounds.x();
        float top = bounds.y();
        float right = bounds.x() + bounds.width();
        float bottom = bounds.y() + bounds.height();

        float x = clamp(anchorX - width * 0.5f, left, right - width);
        float y = anchorY - gap - height;
        if (y < top) {
            y = anchorY + gap;
            if (y + height > bottom) {
                // Сверху и снизу тесно (низкий виджет): ставим сбоку, чтобы не закрывать саму точку.
                y = clamp(anchorY - height * 0.5f, top, bottom - height);
                x = anchorX + gap + width <= right ? anchorX + gap : anchorX - gap - width;
                x = clamp(x, left, right - width);
            }
        }
        draw.roundedRectFilled(x, y, width, height, TOOLTIP_RADIUS, background);
        draw.roundedRectStroke(x + 0.5f, y + 0.5f, width - 1.0f, height - 1.0f, TOOLTIP_RADIUS, border, 1.0f);
        drawText(draw, text, x + TOOLTIP_PADDING_X, y + TOOLTIP_PADDING_Y, textW, textH, textColor,
                Alignment.START, Alignment.CENTER);
    }
}
