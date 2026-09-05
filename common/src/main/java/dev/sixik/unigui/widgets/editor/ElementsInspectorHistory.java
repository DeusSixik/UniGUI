package dev.sixik.unigui.widgets.editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** In-memory undo/redo history для изменений ElementsInspector. */
public final class ElementsInspectorHistory {
    private final Deque<ElementsInspectorCommand> undo = new ArrayDeque<>();
    private final Deque<ElementsInspectorCommand> redo = new ArrayDeque<>();
    private boolean replaying;

    public boolean execute(ElementsInspectorCommand command) {
        Objects.requireNonNull(command, "command");
        try {
            command.apply();
        } catch (RuntimeException failure) {
            return false;
        }
        if (!replaying) {
            undo.push(command);
            redo.clear();
        }
        return true;
    }

    public boolean undo() {
        ElementsInspectorCommand command = undo.pollFirst();
        if (command == null) return false;
        replaying = true;
        try {
            command.undo();
            redo.push(command);
            return true;
        } catch (RuntimeException failure) {
            undo.push(command);
            return false;
        } finally {
            replaying = false;
        }
    }

    public boolean redo() {
        ElementsInspectorCommand command = redo.pollFirst();
        if (command == null) return false;
        replaying = true;
        try {
            command.apply();
            undo.push(command);
            return true;
        } catch (RuntimeException failure) {
            redo.push(command);
            return false;
        } finally {
            replaying = false;
        }
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }

    public int undoCount() {
        return undo.size();
    }

    public int redoCount() {
        return redo.size();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }
}
