package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * История изменений графа для undo/redo.
 *
 * <p>Запись идёт только внутри транзакции ({@link #transact(String, Runnable)}): все изменения модели за время
 * транзакции складываются в одну запись и откатываются одним шагом. Вне транзакции граф ничего не запоминает,
 * поэтому построение графа в коде и восстановление snapshot не засоряют историю. Интерактивные действия
 * (перетаскивание, соединение проводов, удаление, сворачивание, вставка) открывают транзакции сами.</p>
 *
 * <p>Операции хранят ссылки на сами объекты (ноды, провода, регионы), а не их копии: удалённая нода при отмене
 * возвращается тем же экземпляром вместе с проводами, включая провода, вытесненные ёмкостью порта.</p>
 */
@AiDoc(
        summary = "Undo/redo history of a NodeGraph recorded in explicit transactions; each transaction is one undo step.",
        purpose = "Makes node graph editing reversible without serializing nodes: operations hold the original objects.",
        useWhen = {
                "Wrap programmatic edits in `graph.transact(name, ...)` to make them undoable as one step.",
                "Bind `undo()`/`redo()` to toolbar buttons and read `undoName()` for their tooltips."
        },
        criticalRules = {
                "Nothing is recorded outside a transaction.",
                "A new recorded step discards the redo stack.",
                "Undo and redo are ignored while a transaction is open.",
                "If replaying a step throws, the history is cleared and the exception is rethrown."
        },
        related = {"NodeGraph"})
@WikiPage(title = "NodeGraphHistory", slug = "node-graph-history", category = "<key:graph.category>", summary = "<key:node.graph.history.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.graph.history.overview>", order = 10)
public class NodeGraphHistory {
    /**
     * Обратимая операция над моделью графа.
     */
    interface Operation {
        void undo();

        void redo();

        static Operation of(Runnable undo, Runnable redo) {
            return new Operation() {
                @Override
                public void undo() {
                    undo.run();
                }

                @Override
                public void redo() {
                    redo.run();
                }
            };
        }
    }

    private record Entry(String name, List<Operation> operations) {
    }

    private final NodeGraph graph;
    private final ArrayDeque<Entry> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Entry> redoStack = new ArrayDeque<>();
    private List<Operation> open;
    private String openName = "";
    private int depth;
    private boolean replaying;
    private int limit = 200;

    NodeGraphHistory(NodeGraph graph) {
        this.graph = graph;
    }

    /**
     * Выполняет действие как одну запись истории. Вложенные транзакции сливаются во внешнюю.
     *
     * @param name   название шага для подсказок («Move node», «Delete»)
     * @param action изменения графа
     */
    public void transact(String name, Runnable action) {
        begin(name);
        try {
            action.run();
        } finally {
            commit();
        }
    }

    /**
     * @return {@code true}, если изменения сейчас записываются
     */
    public boolean recording() {
        return depth > 0 && !replaying;
    }

    /**
     * @return {@code true}, если есть что отменять
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * @return {@code true}, если есть что повторять
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * @return название шага, который отменит {@link #undo()}, или пустая строка
     */
    public String undoName() {
        return undoStack.isEmpty() ? "" : undoStack.peekLast().name();
    }

    /**
     * @return название шага, который повторит {@link #redo()}, или пустая строка
     */
    public String redoName() {
        return redoStack.isEmpty() ? "" : redoStack.peekLast().name();
    }

    /**
     * @return максимум шагов в истории
     */
    public int limit() {
        return limit;
    }

    /**
     * @param limit максимум шагов, не меньше 1; лишние самые старые шаги отбрасываются
     * @return эта история
     */
    public NodeGraphHistory limit(int limit) {
        this.limit = Math.max(1, limit);
        trim();
        return this;
    }

    /**
     * Забывает все шаги.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * Отменяет последний шаг.
     *
     * @return {@code true}, если шаг был отменён
     */
    public boolean undo() {
        if (depth > 0 || replaying || undoStack.isEmpty()) return false;
        Entry entry = undoStack.pollLast();
        replay(entry, false);
        redoStack.addLast(entry);
        return true;
    }

    /**
     * Повторяет последний отменённый шаг.
     *
     * @return {@code true}, если шаг был повторён
     */
    public boolean redo() {
        if (depth > 0 || replaying || redoStack.isEmpty()) return false;
        Entry entry = redoStack.pollLast();
        replay(entry, true);
        undoStack.addLast(entry);
        return true;
    }

    void record(Operation operation) {
        if (recording()) open.add(operation);
    }

    private void begin(String name) {
        if (replaying) return;
        if (depth == 0) {
            open = new ArrayList<>();
            openName = name == null ? "" : name;
        }
        depth++;
    }

    private void commit() {
        if (replaying) return;
        depth--;
        if (depth > 0) return;
        List<Operation> operations = open;
        open = null;
        if (operations == null || operations.isEmpty()) return;
        undoStack.addLast(new Entry(openName, operations));
        redoStack.clear();
        trim();
    }

    private void trim() {
        while (undoStack.size() > limit) {
            undoStack.pollFirst();
        }
    }

    private void replay(Entry entry, boolean forward) {
        replaying = true;
        try {
            List<Operation> operations = entry.operations();
            if (forward) {
                for (int i = 0; i < operations.size(); i++) operations.get(i).redo();
            } else {
                for (int i = operations.size() - 1; i >= 0; i--) operations.get(i).undo();
            }
        } catch (RuntimeException failure) {
            clear();
            throw failure;
        } finally {
            replaying = false;
            graph.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
    }
}
