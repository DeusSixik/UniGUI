package dev.sixik.unigui.api.style;

/**
 * Набор линий сетки, рисуемых {@code VirtualTableView} между ячейками.
 */
public enum TableGridLines {
    /**
     * Горизонтальные и вертикальные разделители.
     */
    BOTH,
    /**
     * Только горизонтальные разделители между строками.
     */
    HORIZONTAL,
    /**
     * Только вертикальные разделители между колонками.
     */
    VERTICAL,
    /**
     * Сетка не рисуется; строки различаются только фоном.
     */
    NONE;

    /**
     * Возвращает признак отрисовки горизонтальных разделителей строк.
     *
     * @return {@code true}, если между строками нужна линия
     */
    public boolean horizontal() {
        return this == BOTH || this == HORIZONTAL;
    }

    /**
     * Возвращает признак отрисовки вертикальных разделителей колонок.
     *
     * @return {@code true}, если между колонками нужна линия
     */
    public boolean vertical() {
        return this == BOTH || this == VERTICAL;
    }
}
