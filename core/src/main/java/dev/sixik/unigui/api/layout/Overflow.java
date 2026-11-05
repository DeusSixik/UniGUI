package dev.sixik.unigui.api.layout;

/**
 * Управляет обрезкой дочернего содержимого и прокруткой по каждой оси.
 */
public enum Overflow {
    /**
     * Содержимое может отрисовываться за пределами границ виджета.
     */
    VISIBLE,
    /**
     * Содержимое обрезается без прокрутки.
     */
    HIDDEN,
    /**
     * {@code ScrollView} всегда включает прокрутку по этой оси.
     */
    SCROLL,
    /**
     * {@code ScrollView} включает прокрутку только когда содержимое превышает область просмотра.
     */
    AUTO;

    /**
     * Вычисляет эффективное overflow по горизонтали с учётом комбинационного правила.
     *
     * <p>По spec если одна ось {@code visible}, а другая нет, {@code visible}
     * вычисляется в {@code auto}.</p>
     *
     * @param overflowX overflow по X
     * @param overflowY overflow по Y
     * @return эффективный overflow по X
     */
    public static Overflow effectiveHorizontal(Overflow overflowX, Overflow overflowY) {
        Overflow x = overflowX == null ? VISIBLE : overflowX;
        Overflow y = overflowY == null ? VISIBLE : overflowY;
        return x == VISIBLE && y != VISIBLE ? AUTO : x;
    }

    /**
     * Вычисляет эффективное overflow по вертикали с учётом комбинационного правила.
     *
     * @param overflowX overflow по X
     * @param overflowY overflow по Y
     * @return эффективный overflow по Y
     */
    public static Overflow effectiveVertical(Overflow overflowX, Overflow overflowY) {
        Overflow x = overflowX == null ? VISIBLE : overflowX;
        Overflow y = overflowY == null ? VISIBLE : overflowY;
        return y == VISIBLE && x != VISIBLE ? AUTO : y;
    }
}
