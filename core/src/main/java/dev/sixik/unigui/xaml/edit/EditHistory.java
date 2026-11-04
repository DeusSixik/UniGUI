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
    /** Размер стека отмены в начале группы или -1 вне группы: ниже этой отметки правки не сливаются. */
    private int groupFloor = -1;

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
        boolean mergeable = groupFloor < 0 || undo.size() > groupFloor;
        Edit merged = last == null || !mergeable ? null : last.mergeWith(edit);
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

    /**
     * Начинает группу: правки, применённые до {@link #endGroup}, станут одним шагом отмены.
     *
     * @throws IllegalStateException если группа уже начата
     */
    public void beginGroup() {
        if (groupFloor >= 0) throw new IllegalStateException("A group is already open");
        groupFloor = undo.size();
    }

    /**
     * Закрывает группу, заменяя её правки одной.
     *
     * @param label описание шага отмены
     * @return составная правка или пусто, если в группе правок не было
     */
    public Optional<Edit> endGroup(String label) {
        if (groupFloor < 0) throw new IllegalStateException("No group is open");
        java.util.List<Edit> members = new java.util.ArrayList<>();
        while (undo.size() > groupFloor) members.add(0, undo.pollLast());
        groupFloor = -1;
        if (members.isEmpty()) return Optional.empty();
        Edit group = members.size() == 1 ? members.get(0) : new Group(label, members);
        undo.addLast(group);
        return Optional.of(group);
    }

    /** Закрывает группу и откатывает всё, что в ней применено; в историю повтора ничего не попадает. */
    public void abortGroup() {
        if (groupFloor < 0) throw new IllegalStateException("No group is open");
        while (undo.size() > groupFloor) undo.pollLast().undo();
        groupFloor = -1;
    }

    /** Несколько правок как одна: применяются по порядку, откатываются в обратном. */
    private static final class Group implements Edit {
        private final String label;
        private final java.util.List<Edit> members;

        private Group(String label, java.util.List<Edit> members) {
            this.label = label;
            this.members = java.util.List.copyOf(members);
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public void apply() {
            for (Edit member : members) member.apply();
        }

        @Override
        public void undo() {
            for (int i = members.size() - 1; i >= 0; i--) members.get(i).undo();
        }
    }

    /** Забывает всю историю (например, после замены документа целиком). */
    public void clear() {
        undo.clear();
        redo.clear();
        groupFloor = -1;
    }
}
