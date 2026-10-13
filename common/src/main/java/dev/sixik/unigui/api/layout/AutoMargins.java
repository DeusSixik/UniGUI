package dev.sixik.unigui.api.layout;

/**
 * Автоматические отступы по каждой стороне для flex- и slot-компоновки.
 *
 * <p>Аналог CSS {@code margin: auto}: автоматический отступ поглощает свободное место своей оси
 * вместо фиксированного сдвига. На главной оси автоматические отступы делят свободное место
 * и отменяют {@code justify-content}; на поперечной оси пара автоматических отступов
 * центрирует элемент, один — прижимает его к противоположной стороне.</p>
 *
 * <p>Фиксированные пиксельные отступы хранятся в {@link EdgeInsets} через {@code LayoutStyle.margin()};
 * этот тип лишь отмечает, какие стороны автоматические.</p>
 */
public record AutoMargins(boolean left, boolean top, boolean right, boolean bottom) {
    public static final AutoMargins NONE = new AutoMargins(false, false, false, false);
    public static final AutoMargins ALL = new AutoMargins(true, true, true, true);
    public static final AutoMargins HORIZONTAL = new AutoMargins(true, false, true, false);
    public static final AutoMargins VERTICAL = new AutoMargins(false, true, false, true);

    /**
     * Создаёт автоматические отступы для выбранных сторон.
     *
     * @param left автоматический отступ слева
     * @param top автоматический отступ сверху
     * @param right автоматический отступ справа
     * @param bottom автоматический отступ снизу
     * @return флаги отступов
     */
    public static AutoMargins of(boolean left, boolean top, boolean right, boolean bottom) {
        if (!left && !top && !right && !bottom) {
            return NONE;
        }
        if (left && top && right && bottom) {
            return ALL;
        }
        return new AutoMargins(left, top, right, bottom);
    }

    /**
     * Проверяет, есть ли хоть один автоматический отступ.
     *
     * @return {@code true}, если хотя бы одна сторона автоматическая
     */
    public boolean any() {
        return left || top || right || bottom;
    }

    /**
     * Подсчитывает автоматические отступы на горизонтальной оси.
     *
     * @return 0, 1 или 2
     */
    public int horizontalCount() {
        return (left ? 1 : 0) + (right ? 1 : 0);
    }

    /**
     * Подсчитывает автоматические отступы на вертикальной оси.
     *
     * @return 0, 1 или 2
     */
    public int verticalCount() {
        return (top ? 1 : 0) + (bottom ? 1 : 0);
    }
}
