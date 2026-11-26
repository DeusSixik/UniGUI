package dev.sixik.unigui.api.scene;

/**
 * Окружающий свет, одинаковый для всех ориентаций поверхности.
 *
 * @param colorRgb  цвет в младших 24 битах RGB.
 * @param intensity конечный неотрицательный множитель яркости, 0 выключает свет.
 */
public record AmbientLight(int colorRgb, float intensity) {
    /**
     * Нейтральный белый свет с единичной интенсивностью.
     */
    public static final AmbientLight WHITE = new AmbientLight(0xFFFFFF, 1.0f);

    /**
     * Сохраняет цвет и интенсивность.
     *
     * @param colorRgb  цвет RGB, старший байт игнорируется.
     * @param intensity конечный неотрицательный множитель яркости.
     * @throws IllegalArgumentException если интенсивность отрицательная или не конечна.
     */
    public AmbientLight {
        if (!(intensity >= 0.0f) || !Float.isFinite(intensity)) {
            throw new IllegalArgumentException("Light intensity must be finite and nonnegative");
        }
        colorRgb &= 0xFFFFFF;
    }
}
