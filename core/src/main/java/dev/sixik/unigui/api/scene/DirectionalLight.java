package dev.sixik.unigui.api.scene;

/**
 * Свет с параллельными лучами. Для мировой единичной нормали n диффузный множитель Ламберта
 * равен {@code intensity * max(0, -dot(n, direction))}, где direction — направление лучей.
 *
 * @param x         единичная компонента X направления лучей в системе сцены.
 * @param y         единичная компонента Y направления лучей в системе сцены.
 * @param z         единичная компонента Z направления лучей в системе сцены.
 * @param colorRgb  цвет в младших 24 битах RGB.
 * @param intensity конечный неотрицательный множитель яркости, 0 выключает свет.
 */
public record DirectionalLight(float x, float y, float z, int colorRgb, float intensity) {
    /**
     * Нормализует направление делением компонент на {@code sqrt(x*x + y*y + z*z)}.
     *
     * @param x         мировая компонента X направления лучей.
     * @param y         мировая компонента Y направления лучей.
     * @param z         мировая компонента Z направления лучей.
     * @param colorRgb  цвет RGB, старший байт игнорируется.
     * @param intensity конечный неотрицательный множитель яркости.
     * @throws IllegalArgumentException если направление нулевое или не конечное либо интенсивность вне диапазона.
     */
    public DirectionalLight {
        double length = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
        if (!(length > 0.0) || !Double.isFinite(length) || !(intensity >= 0.0f) || !Float.isFinite(intensity)) {
            throw new IllegalArgumentException("Light requires a finite nonzero direction and nonnegative intensity");
        }
        x = (float) (x / length);
        y = (float) (y / length);
        z = (float) (z / length);
        colorRgb &= 0xFFFFFF;
    }
}
