package dev.sixik.unigui.widgets.dnd;

/**
 * Итог drag-жеста.
 */
public enum DragOutcome {
    /**
     * Отпускание над drop-зоной, которая приняла payload.
     */
    DROPPED,
    /**
     * Отпускание там, где нет подходящей drop-зоны.
     */
    NO_TARGET,
    /**
     * Жест отменён программно или источник пропал.
     */
    CANCELLED
}
