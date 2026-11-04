package dev.sixik.unigui.xaml.edit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * История правок с отменой и повтором.
 *
 * <p>Применение новой правки очищает стек повтора. Подряд идущие правки одного и того же свойства сливаются в
 * одну ({@link Edit#mergeWith}), поэтому непрерывное изменение значения даёт один шаг отмены. Глубина ограничена,
 * самые старые правки забываются первыми.</p>
 */
public final class EditHistory {
    /** Глубина истории по умолчанию. */
    public static final int DEFAULT_LIMIT = 256;

    private final Deque<Edit> undo = new ArrayDeque<>();
    private final Deque<Edit> redo = new ArrayDeque<>();
    private final int limit;

    /** Создаёт историю с глубиной {@link #DEFAULT_LIMIT}. */
    public EditHistory() {
        this(DEFAULT_LIMIT);
    }

    /**
     * Создаёт историю.
     *
     * @param limit максимальное число шагов отмены; положительное
     */
    public EditHistory(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        this.limit = limit;
    }

    /**
     * Применяет правку и записывает её в историю.
     *
     * @param edit правка
     */
    public void execute(Edit edit) {
        edit.apply();
        redo.clear();
        Edit last = undo.peekLast();
        Edit merged = last == null ? null : last.mergeWith(edit);
        if (merged != null) {
            undo.pollLast();
            undo.addLast(merged);
            return;
        }
        undo.addLast(edit);
        while (undo.size() > limit) undo.pollFirst();
    }

    /**
     * Отменяет последнюю правку.
     *
     * @return отменённая правка или пусто, если отменять нечего
     */
    public Optional<Edit> undo() {
        Edit edit = undo.pollLast();
        if (edit == null) return Optional.empty();
        edit.undo();
        redo.addLast(edit);
        return Optional.of(edit);
    }

    /**
     * Повторяет последнюю отменённую правку.
     *
     * @return повторённая правка или пусто, если повторять нечего
     */
    public Optional<Edit> redo() {
        Edit edit = redo.pollLast();
        if (edit == null) return Optional.empty();
        edit.apply();
        undo.addLast(edit);
        return Optional.of(edit);
    }

    /** @return число шагов, которые можно отменить */
    public int undoCount() {
        return undo.size();
    }

    /** @return число шагов, которые можно повторить */
    public int redoCount() {
        return redo.size();
    }

    /** @return описание правки, которая будет отменена, или пусто */
    public Optional<String> undoLabel() {
        return Optional.ofNullable(undo.peekLast()).map(Edit::label);
    }

    /** @return описание правки, которая будет повторена, или пусто */
    public Optional<String> redoLabel() {
        return Optional.ofNullable(redo.peekLast()).map(Edit::label);
    }

    /** Забывает всю историю (например, после замены документа целиком). */
    public void clear() {
        undo.clear();
        redo.clear();
    }
}
