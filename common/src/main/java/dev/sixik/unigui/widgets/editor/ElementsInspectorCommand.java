package dev.sixik.unigui.widgets.editor;

/** Отменяемая операция ElementsInspector над живым runtime-деревом. */
public interface ElementsInspectorCommand {
    /** Короткое описание операции для будущего меню истории. */
    String description();

    /** Применяет операцию. */
    void apply();

    /** Отменяет ранее применённую операцию. */
    void undo();
}
