package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.edit.Hierarchy;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.List;
import java.util.Optional;

/**
 * Структурные правки документа, которыми пользуются палитра, иерархия и меню Edit. Каждая правка идёт через
 * {@link EditorSession#apply}, поэтому попадает в историю отмены и в событие изменения документа.
 */
final class UiEdits {
    private UiEdits() {
    }

    /**
     * Добавляет в документ новый узел указанного типа. Узел попадает в выбранный контейнер; если выбранный узел
     * детей не принимает, поднимаемся к ближайшему предку, который принимает.
     *
     * @param session сессия
     * @param type    тип нового виджета
     * @return добавленный узел или пусто, если принять его некому
     */
    static Optional<UiNode> insert(EditorSession session, TypeInfo<?> type) {
        UiNode target = container(session).orElse(null);
        if (target == null) return Optional.empty();
        UiNode created = new UiNode(type.name());
        if (!session.apply(Edits.addChild(target, target.children().size(), created))) return Optional.empty();
        session.select(created);
        return Optional.of(created);
    }

    /** Ближайший к выбору узел, принимающий детей: сам выбранный, затем его предки. */
    private static Optional<UiNode> container(EditorSession session) {
        UiNode node = session.selected().orElse(session.document().root());
        while (node != null) {
            Optional<TypeInfo<?>> type = session.schema().type(node.type());
            if (type.isPresent() && type.get().acceptsChildren()) return Optional.of(node);
            node = Hierarchy.parentOf(session.document(), node).orElse(null);
        }
        return Optional.empty();
    }

    /**
     * Удаляет выбранный узел; корень удалить нельзя.
     *
     * @param session сессия
     * @return {@code true}, если узел удалён
     */
    static boolean deleteSelected(EditorSession session) {
        UiNode node = session.selected().orElse(null);
        if (node == null || node == session.document().root()) return false;
        UiNode parent = Hierarchy.parentOf(session.document(), node).orElse(null);
        boolean done = session.apply(Edits.remove(session.document(), node));
        if (done && parent != null) session.select(parent);
        return done;
    }

    /**
     * Создаёт копию выбранного узла сразу после него.
     *
     * @param session сессия
     * @return {@code true}, если копия добавлена
     */
    static boolean duplicateSelected(EditorSession session) {
        UiNode node = session.selected().orElse(null);
        if (node == null || node == session.document().root()) return false;
        UiNode parent = Hierarchy.parentOf(session.document(), node).orElse(null);
        if (parent == null) return false;
        UiNode copy = node.copy();
        int index = parent.children().indexOf(node) + 1;
        boolean done = session.apply(Edits.addChild(parent, index, copy));
        if (done) session.select(copy);
        return done;
    }

    /**
     * Сдвигает выбранный узел среди соседей.
     *
     * @param session сессия
     * @param delta   на сколько позиций: -1 выше, 1 ниже
     * @return {@code true}, если узел сдвинут
     */
    static boolean moveSelected(EditorSession session, int delta) {
        UiNode node = session.selected().orElse(null);
        if (node == null || node == session.document().root()) return false;
        UiNode parent = Hierarchy.parentOf(session.document(), node).orElse(null);
        if (parent == null) return false;
        List<UiNode> siblings = parent.children();
        int index = siblings.indexOf(node);
        int target = index + delta;
        if (target < 0 || target >= siblings.size()) return false;
        return session.apply(Edits.move(session.document(), node, parent, target));
    }

    /**
     * @param session сессия
     * @return {@code true}, если выбран узел, который можно сдвинуть, копировать или удалить
     */
    static boolean hasMovableSelection(EditorSession session) {
        UiNode node = session.selected().orElse(null);
        return node != null && node != session.document().root();
    }
}
