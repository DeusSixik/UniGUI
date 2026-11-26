package dev.sixik.unigui.api.math;

/**
 * Чтение матрицы 4×4. Матрица умножается на вектор-столбец справа.
 */
public interface Mat4View {
    /**
     * Читает элемент матрицы.
     *
     * @param row    индекс строки от 0 до 3.
     * @param column индекс столбца от 0 до 3.
     * @return значение элемента.
     * @throws IndexOutOfBoundsException если индекс вне диапазона.
     */
    double get(int row, int column);
}
