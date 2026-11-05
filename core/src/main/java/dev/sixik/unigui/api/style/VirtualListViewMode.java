package dev.sixik.unigui.api.style;

/**
 * Способ размещения виртуальных элементов {@code VirtualListView}.
 */
public enum VirtualListViewMode {
    /**
     * Элементы занимают отдельные строки на всю доступную ширину.
     */
    LIST,
    /**
     * Элементы размещаются в адаптивной или фиксированной сетке.
     */
    GRID
}
