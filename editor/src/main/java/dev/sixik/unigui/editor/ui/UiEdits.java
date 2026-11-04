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

    /**
     * Добавляет новый узел в указанное место: в сам узел, если он принимает детей, иначе в ближайшего предка,
     * который принимает.
     *
     * @param session сессия
     * @param type    тип нового виджета
     * @param target  узел, на который бросили или который выбран
     * @return добавленный узел или пусто, если принять его некому
     */
    static Optional<UiNode> insertInto(EditorSession session, TypeInfo<?> type, UiNode target) {
        UiNode container = container(session, target).orElse(null);
        if (container == null) return Optional.empty();
        UiNode created = new UiNode(type.name());
        if (!session.apply(Edits.addChild(container, container.children().size(), created))) return Optional.empty();
        session.select(created);
        return Optional.of(created);
    }

    /**
     * Переносит узел на строку иерархии: внутрь, если строка принимает детей, иначе следом за ней.
     *
     * @param session сессия
     * @param node    переносимый узел
     * @param row     узел, на который бросили
     * @return {@code true}, если узел перенесён
     */
    static boolean moveOnto(EditorSession session, UiNode node, UiNode row) {
        UiNode root = session.document().root();
        if (node == root || node == row || isInside(node, row)) return false;
        UiNode parent;
        int index;
        Optional<TypeInfo<?>> type = session.schema().type(row.type());
        if (type.isPresent() && type.get().acceptsChildren() && !isComponentRoot(root, row)) {
            parent = row;
            index = row.children().size();
        } else {
            parent = Hierarchy.parentOf(session.document(), row).orElse(null);
            if (parent == null) return false;
            index = parent.children().indexOf(row) + 1;
        }
        Optional<UiNode> oldParent = Hierarchy.parentOf(session.document(), node);
        if (oldParent.isPresent() && oldParent.get() == parent && parent.children().indexOf(node) < index) index--;
        return session.apply(Edits.move(session.document(), node, parent, index));
    }

    /** @return {@code true}, если {@code inner} лежит внутри {@code outer} на любой глубине */
    private static boolean isInside(UiNode outer, UiNode inner) {
        for (UiNode child : outer.children()) {
            if (child == inner || isInside(child, inner)) return true;
        }
        return false;
    }

    /** Ближайший к выбору узел, принимающий детей: сам выбранный, затем его предки. */
    private static Optional<UiNode> container(EditorSession session) {
        return container(session, session.selected().orElse(session.document().root()));
    }

    /** Ближайший к узлу принимающий детей: сам узел, затем его предки. */
    private static Optional<UiNode> container(EditorSession session, UiNode start) {
        UiNode root = session.document().root();
        UiNode node = start;
        if (isComponentRoot(root, node)) {
            // Корень компонента — не виджет: без тела новый узел станет телом, с телом вставка идёт в тело.
            if (node.children().isEmpty()) return Optional.of(node);
            node = node.children().get(0);
        }
        while (node != null) {
            if (isComponentRoot(root, node)) return Optional.empty(); // поднялись выше тела: принять некому
            Optional<TypeInfo<?>> type = session.schema().type(node.type());
            if (type.isPresent() && type.get().acceptsChildren()) return Optional.of(node);
            node = Hierarchy.parentOf(session.document(), node).orElse(null);
        }
        return Optional.empty();
    }

    /** @return {@code true}, если узел — корень документа компонента ({@code <Component>}) */
    static boolean isComponentRoot(UiNode documentRoot, UiNode node) {
        return node == documentRoot && node.type().local().equals("Component");
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
