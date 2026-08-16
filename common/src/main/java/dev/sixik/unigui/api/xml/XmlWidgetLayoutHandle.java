package dev.sixik.unigui.api.xml;

/** Имена drag/resize handles для редактирования XML layout-атрибутов исходного документа. */
public enum XmlWidgetLayoutHandle {
    MOVE(false, false, false, false),
    NORTH(true, false, false, false),
    SOUTH(false, true, false, false),
    EAST(false, false, true, false),
    WEST(false, false, false, true),
    NORTH_EAST(true, false, true, false),
    NORTH_WEST(true, false, false, true),
    SOUTH_EAST(false, true, true, false),
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

    public boolean move() {
        return this == MOVE;
    }

    public boolean north() {
        return north;
    }

    public boolean south() {
        return south;
    }

    public boolean east() {
        return east;
    }

    public boolean west() {
        return west;
    }
}
