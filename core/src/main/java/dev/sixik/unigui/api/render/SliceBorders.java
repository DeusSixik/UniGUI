package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.Objects;

/**
 * Immutable рамки nine-slice в пикселях текстуры.
 *
 * <p>Используется виджетами, когда {@code ImageFit.NINE_SLICE} выбран как fit
 * фоновой текстуры: углы рисуются без растяжения, края тянутся вдоль одной
 * оси, центр — по обеим. Нулевые рамки вырождаются в обычный stretch.</p>
 *
 * @param left левая рамка в пикселях текстуры
 * @param top верхняя рамка в пикселях текстуры
 * @param right правая рамка в пикселях текстуры
 * @param bottom нижняя рамка в пикселях текстуры
 * @see NineSlice
 * @see ImageFit#NINE_SLICE
 */
@AiDoc(
        summary = "Immutable nine-slice border inset metrics measured in native texture pixels.",
        purpose = "Specifies unscaled corner dimensions for nine-slice panel rendering.",
        useWhen = {
                "Define nine-slice border insets for widget visual surfaces via `SliceBorders.all(border)` or `SliceBorders.symmetric(h, v)`.",
                "Build a NineSlice directly from borders using `slice(TextureHandle)`."
        },
        criticalRules = {
                "Values are in raw texture pixels, not screen UI pixels.",
                "Negative insets are sanitized to 0.0.",
                "Zero borders (`isZero()`) degrade nine-slice rendering to a simple stretch."
        },
        related = {"NineSlice", "ImageFit", "TextureHandle", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Create uniform slice borders",
                        order = 10,
                        code = """
                                SliceBorders borders = SliceBorders.all(6.0f);
                                NineSlice slice = borders.slice(texture);
                                """)
        })
public record SliceBorders(float left, float top, float right, float bottom) {
    /** Нулевые рамки: nine-slice вырождается в stretch. */
    public static final SliceBorders ZERO = new SliceBorders(0.0f, 0.0f, 0.0f, 0.0f);

    public SliceBorders {
        left = sanitize(left);
        top = sanitize(top);
        right = sanitize(right);
        bottom = sanitize(bottom);
    }

    /**
     * Создаёт одинаковые рамки со всех сторон.
     *
     * @param border ширина рамки в пикселях текстуры
     * @return новые рамки
     */
    public static SliceBorders all(float border) {
        return new SliceBorders(border, border, border, border);
    }

    /**
     * Создаёт symmetric рамки: горизонталь и вертикаль.
     *
     * @param horizontal левая/правая рамки
     * @param vertical верхняя/нижняя рамки
     * @return новые рамки
     */
    public static SliceBorders symmetric(float horizontal, float vertical) {
        return new SliceBorders(horizontal, vertical, horizontal, vertical);
    }

    /**
     * Проверяет, все ли рамки нулевые.
     *
     * @return {@code true}, если nine-slice выродится в stretch
     */
    public boolean isZero() {
        return left == 0.0f && top == 0.0f && right == 0.0f && bottom == 0.0f;
    }

    /**
     * Строит {@link NineSlice} для текстуры с этими рамками.
     *
     * @param texture текстура-источник
     * @return nine-slice для {@link DrawScope#nineSlice}
     */
    public NineSlice slice(TextureHandle texture) {
        return NineSlice.of(Objects.requireNonNull(texture, "texture"), left, top, right, bottom);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
