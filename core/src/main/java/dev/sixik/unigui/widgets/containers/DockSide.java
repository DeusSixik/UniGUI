package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockingManager;
import dev.sixik.unigui.widgets.docking.DockingRoot;

/**
 * Сторона, к которой {@link DockPanel} приклеивает дочерний виджет.
 *
 * <p>{@code DockSide} описывает только четыре edge-слота простого
 * layout-контейнера. Это не enum зон drop'а из IDE-style docking-системы:
 * для {@link DockingRoot}/{@link DockingManager} используется {@link DockArea}.</p>
 */
public enum DockSide {
    /** Прижать виджет к левой стороне оставшейся области. */
    LEFT,
    /** Прижать виджет к правой стороне оставшейся области. */
    RIGHT,
    /** Прижать виджет к верхней стороне оставшейся области. */
    TOP,
    /** Прижать виджет к нижней стороне оставшейся области. */
    BOTTOM
}
