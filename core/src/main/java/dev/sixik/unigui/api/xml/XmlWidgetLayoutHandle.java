package dev.sixik.unigui.api.xml;

/**
 * Имена drag/resize handles для редактирования XML layout-атрибутов исходного документа.
 *
 * <p>Enum описывает, какие стороны рамки участвуют в изменении. {@link #MOVE} не меняет размер,
 * а только сдвигает frame целиком.</p>
 */
public enum XmlWidgetLayoutHandle {
    /** Перемещение frame без resize. */
    MOVE(false, false, false, false),
    /** Resize верхней стороны. */
    NORTH(true, false, false, false),
    /** Resize нижней стороны. */
    SOUTH(false, true, false, false),
    /** Resize правой стороны. */
    EAST(false, false, true, false),
    /** Resize левой стороны. */
    WEST(false, false, false, true),
    /** Resize верхней и правой сторон. */
    NORTH_EAST(true, false, true, false),
    /** Resize верхней и левой сторон. */
    NORTH_WEST(true, false, false, true),
    /** Resize нижней и правой сторон. */
    SOUTH_EAST(false, true, true, false),
    /** Resize нижней и левой сторон. */
    SOUTH_WEST(false, true, false, true);

    private final boolean north;
    private final boolean south;
    private final boolean east;
    private final boolean west;

    XmlWidgetLayoutHandle(boolean north, boolean south, boolean east, boolean west) {
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }

    /**
     * Проверяет, что handle означает перемещение, а не resize.
     *
     * @return {@code true} только для {@link #MOVE}
     */
    public boolean move() {
        return this == MOVE;
    }

    /** @return {@code true}, если handle двигает верхнюю сторону */
    public boolean north() {
        return north;
    }

    /** @return {@code true}, если handle двигает нижнюю сторону */
    public boolean south() {
        return south;
    }

    /** @return {@code true}, если handle двигает правую сторону */
    public boolean east() {
        return east;
    }

    /** @return {@code true}, если handle двигает левую сторону */
    public boolean west() {
        return west;
    }
}
