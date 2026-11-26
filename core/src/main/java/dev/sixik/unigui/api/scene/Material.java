package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.render.TextureHandle;

import java.util.Objects;

/**
 * Неизменяемые настройки отрисовки поверхности меша.
 *
 * @param colorArgb   цвет ARGB, умножаемый на цвет вершины и цвет текстуры.
 * @param texture     текстура бэкенда или null для однотонной поверхности.
 * @param shading     режим освещения.
 * @param alphaMode   режим обработки альфа-канала.
 * @param alphaCutoff минимальная альфа, оставляемая при {@link AlphaMode#CUTOUT}, от 0 до 1.
 * @param culling     отсекаемая сторона треугольников.
 */
public record Material(int colorArgb, TextureHandle texture, Shading shading,
                       AlphaMode alphaMode, float alphaCutoff, Culling culling) {
    /**
     * Белая непрозрачная поверхность без освещения с отсечением обратных граней.
     */
    public static final Material DEFAULT = new Material(0xFFFFFFFF, null, Shading.UNLIT,
            AlphaMode.OPAQUE, 0.5f, Culling.BACK);

    /**
     * Проверяет и сохраняет параметры отрисовки.
     *
     * @param colorArgb   цвет ARGB.
     * @param texture     текстура бэкенда или null.
     * @param shading     режим освещения.
     * @param alphaMode   режим обработки альфы.
     * @param alphaCutoff порог альфы от 0 до 1.
     * @param culling     отсекаемая сторона.
     * @throws NullPointerException     если не задан режим освещения, альфы или отсечения.
     * @throws IllegalArgumentException если порог альфы вне диапазона.
     */
    public Material {
        Objects.requireNonNull(shading, "shading");
        Objects.requireNonNull(alphaMode, "alphaMode");
        Objects.requireNonNull(culling, "culling");
        if (!(alphaCutoff >= 0.0f && alphaCutoff <= 1.0f)) {
            throw new IllegalArgumentException("Alpha cutoff must be between 0 and 1");
        }
    }

    /**
     * Влияние источников света на цвет поверхности.
     */
    public enum Shading {
        /**
         * Использует цвет материала, вершин и текстуры.
         */
        UNLIT,
        /**
         * Добавляет вклад окружающего и направленного света.
         */
        LIT
    }

    /**
     * Обработка альфа-канала поверхности.
     */
    public enum AlphaMode {
        /**
         * Рисует непрозрачную поверхность.
         */
        OPAQUE,
        /**
         * Отбрасывает фрагменты с альфой ниже порога.
         */
        CUTOUT,
        /**
         * Смешивает цвет с фоном по альфе.
         */
        TRANSLUCENT
    }

    /**
     * Отсечение граней по стороне треугольника.
     */
    public enum Culling {
        /**
         * Рисует обе стороны.
         */
        NONE,
        /**
         * Отбрасывает обратные грани.
         */
        BACK,
        /**
         * Отбрасывает лицевые грани.
         */
        FRONT
    }
}
