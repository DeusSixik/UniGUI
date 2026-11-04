package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.format.MarkupExtensions;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Фабрика стандартных правок документа.
 *
 * <p>Структурные правки (добавить, удалить, переместить) работают с прямыми детьми узла. Содержимое
 * property-элементов ({@code ScrollView.Content}) можно читать и менять в свойствах, но не переставлять:
 * такая правка отклоняется понятным сообщением, а не делается наполовину.</p>
 */
public final class Edits {
    private Edits() {
    }

    // ------------------------------------------------------------------ свойства

    /**
     * Задаёт значение атрибута. Текст разбирается как значение документа, поэтому {@code {Binding Path=x}}
     * становится выражением, а не строкой.
     *
     * @param node узел
     * @param name имя атрибута без префикса
     * @param text значение
     * @return правка
     */
    public static Edit setProperty(UiNode node, String name, String text) {
        return setProperty(node, null, UiName.of(name), MarkupExtensions.parse(text, null));
    }

    /**
     * Задаёт значение свойства, включая attached-свойства и готовые выражения.
     *
     * @param node  узел
     * @param owner владелец или {@code null}
     * @param name  имя члена
     * @param value значение
     * @return правка
     */
    public static Edit setProperty(UiNode node, UiName owner, UiName name, UiValue value) {
        return new SetProperty(node, owner, name, value);
    }

    /**
     * Удаляет атрибут, возвращая свойству значение по умолчанию.
     *
     * @param node узел
     * @param name имя атрибута
     * @return правка; если атрибута нет, ничего не меняет
     */
    public static Edit removeProperty(UiNode node, String name) {
        return new RemoveProperty(node, null, UiName.of(name));
    }

    /**
     * Меняет тип узла (например, {@code Box → VBox}), сохраняя свойства и детей.
     *
     * @param node    узел
     * @param newType новое имя типа
     * @return правка
     */
    public static Edit setType(UiNode node, UiName newType) {
        return new SetType(node, newType);
    }

    // ------------------------------------------------------------------ структура

    /**
     * Добавляет ребёнка.
     *
     * @param parent родитель
     * @param index  позиция среди дочерних узлов; слишком большое значение означает «в конец»
     * @param child  новый узел
     * @return правка
     */
    public static Edit addChild(UiNode parent, int index, UiNode child) {
        return new AddChild(parent, index, child);
    }

    /**
     * Удаляет узел из документа.
     *
     * @param document документ, в котором ищется родитель
     * @param node     удаляемый узел
     * @return правка; для корня или содержимого property-элемента проверка вернёт причину отказа
     */
    public static Edit remove(UiDocument document, UiNode node) {
        return new RemoveChild(document, node);
    }

    /**
     * Переносит узел к другому родителю.
     *
     * @param document  документ
     * @param node      переносимый узел
     * @param newParent новый родитель
     * @param index     позиция среди дочерних узлов нового родителя
     * @return правка
     */
    public static Edit move(UiDocument document, UiNode node, UiNode newParent, int index) {
        return new MoveChild(document, node, newParent, index);
    }

    /**
     * Объединяет правки в одну: применяются по порядку, откатываются в обратном.
     *
     * @param label описание для меню отмены
     * @param edits правки
     * @return составная правка
     */
    public static Edit group(String label, List<Edit> edits) {
        return new Group(label, edits);
    }

    // ------------------------------------------------------------------ вспомогательное

    /** Ищет родителя среди прямых детей; {@code null} для корня и содержимого property-элементов. */
    static UiNode parentOf(UiNode current, UiNode target) {
        for (UiNode child : current.children()) {
            if (child == target) return current;
            UiNode found = parentOf(child, target);
            if (found != null) return found;
        }
        return null;
    }

    private static boolean contains(UiNode ancestor, UiNode candidate) {
        if (ancestor == candidate) return true;
        for (UiNode child : ancestor.children()) if (contains(child, candidate)) return true;
        for (UiProperty property : ancestor.properties()) {
            if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) {
                    if (item instanceof UiNode node && contains(node, candidate)) return true;
                }
            }
        }
        return false;
    }

    /** Позиция в {@code content()}, перед которой окажется ребёнок с индексом {@code index} среди узлов. */
    private static int contentIndex(UiNode parent, int index) {
        List<UiContent> content = parent.content();
        int seen = 0;
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) instanceof UiNode) {
                if (seen == index) return i;
                seen++;
            }
        }
        return content.size();
    }

    private static int identityIndex(UiNode parent, UiNode child) {
        List<UiContent> content = parent.content();
        for (int i = 0; i < content.size(); i++) if (content.get(i) == child) return i;
        return -1;
    }

    private static Optional<String> childPolicy(Schema schema, UiNode parent, UiNode child) {
        TypeInfo<?> type = schema == null ? null : schema.type(parent.type()).orElse(null);
        if (type == null || type.acceptsChildren()) return Optional.empty();
        TypeInfo<?> childType = schema.type(child.type()).orElse(null);
        String childName = childType == null ? child.type().local() : childType.javaType().getSimpleName();
        return Optional.of("Widget " + type.name().local() + " cannot contain child " + childName + ".");
    }

    // ------------------------------------------------------------------ реализации

    private static final class SetProperty implements Edit {
        private final UiNode node;
        private final UiName owner;
        private final UiName name;
        private UiValue value;
        private UiProperty previous;

        SetProperty(UiNode node, UiName owner, UiName name, UiValue value) {
            this.node = node;
            this.owner = owner;
            this.name = name;
            this.value = value;
        }

        @Override
        public String label() {
            return "Set " + name.local();
        }

        @Override
        public void apply() {
            previous = node.property(owner, name).orElse(null);
            UiProperty next = previous != null
                    ? new UiProperty(previous.name(), previous.owner(), value, previous.form(), previous.location())
                    : new UiProperty(name, owner, value, UiProperty.Form.ATTRIBUTE, null);
            node.set(next);
        }

        @Override
        public void undo() {
            if (previous == null) node.remove(owner, name);
            else node.set(previous);
        }

        @Override
        public Edit mergeWith(Edit next) {
            if (next instanceof SetProperty other && other.node == node && other.name.sameAs(name)
                    && (owner == null ? other.owner == null : owner.sameAs(other.owner))) {
                value = other.value;
                return this;
            }
            return null;
        }
    }

    private static final class RemoveProperty implements Edit {
        private final UiNode node;
        private final UiName owner;
        private final UiName name;
        private UiProperty removed;
        private int index;

        RemoveProperty(UiNode node, UiName owner, UiName name) {
            this.node = node;
            this.owner = owner;
            this.name = name;
        }

        @Override
        public String label() {
            return "Reset " + name.local();
        }

        @Override
        public void apply() {
            index = node.propertyIndex(owner, name);
            removed = node.property(owner, name).orElse(null);
            if (removed != null) node.remove(owner, name);
        }

        @Override
        public void undo() {
            if (removed != null) node.insert(index, removed);
        }
    }

    private static final class SetType implements Edit {
        private final UiNode node;
        private final UiName next;
        private UiName previous;

        SetType(UiNode node, UiName next) {
            this.node = node;
            this.next = next;
        }

        @Override
        public String label() {
            return "Change type to " + next.local();
        }

        @Override
        public void apply() {
            previous = node.type();
            node.type(next);
        }

        @Override
        public void undo() {
            node.type(previous);
        }
    }

    private static final class AddChild implements Edit {
        private final UiNode parent;
        private final int index;
        private final UiNode child;
        private int at;

        AddChild(UiNode parent, int index, UiNode child) {
            this.parent = parent;
            this.index = index;
            this.child = child;
        }

        @Override
        public String label() {
            return "Add " + child.type().local();
        }

        @Override
        public Optional<String> check(Schema schema) {
            return childPolicy(schema, parent, child);
        }

        @Override
        public void apply() {
            at = contentIndex(parent, index);
            parent.add(at, child);
        }

        @Override
        public void undo() {
            parent.remove(child);
        }
    }

    private static final class RemoveChild implements Edit {
        private final UiDocument document;
        private final UiNode node;
        private UiNode parent;
        private int at;

        RemoveChild(UiDocument document, UiNode node) {
            this.document = document;
            this.node = node;
        }

        @Override
        public String label() {
            return "Remove " + node.type().local();
        }

        @Override
        public Optional<String> check(Schema schema) {
            if (node == document.root()) return Optional.of("The root node cannot be removed.");
            if (parentOf(document.root(), node) == null) {
                return Optional.of("Nodes inside property elements can be edited, but not removed or moved yet.");
            }
            return Optional.empty();
        }

        @Override
        public void apply() {
            parent = parentOf(document.root(), node);
            at = identityIndex(parent, node);
            parent.remove(node);
        }

        @Override
        public void undo() {
            parent.add(at, node);
        }
    }

    private static final class MoveChild implements Edit {
        private final UiDocument document;
        private final UiNode node;
        private final UiNode newParent;
        private final int index;
        private UiNode oldParent;
        private int oldAt;

        MoveChild(UiDocument document, UiNode node, UiNode newParent, int index) {
            this.document = document;
            this.node = node;
            this.newParent = newParent;
            this.index = index;
        }

        @Override
        public String label() {
            return "Move " + node.type().local();
        }

        @Override
        public Optional<String> check(Schema schema) {
            if (node == document.root()) return Optional.of("The root node cannot be moved.");
            if (parentOf(document.root(), node) == null) {
                return Optional.of("Nodes inside property elements can be edited, but not removed or moved yet.");
            }
            if (contains(node, newParent)) return Optional.of("A node cannot be moved into itself or its descendant.");
            return childPolicy(schema, newParent, node);
        }

        @Override
        public void apply() {
            oldParent = parentOf(document.root(), node);
            oldAt = identityIndex(oldParent, node);
            oldParent.remove(node);
            newParent.add(contentIndex(newParent, index), node);
        }

        @Override
        public void undo() {
            newParent.remove(node);
            oldParent.add(oldAt, node);
        }
    }

    private static final class Group implements Edit {
        private final String label;
        private final List<Edit> edits;

        Group(String label, List<Edit> edits) {
            this.label = label;
            this.edits = new ArrayList<>(edits);
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public Optional<String> check(Schema schema) {
            for (Edit edit : edits) {
                Optional<String> problem = edit.check(schema);
                if (problem.isPresent()) return problem;
            }
            return Optional.empty();
        }

        @Override
        public void apply() {
            for (Edit edit : edits) edit.apply();
        }

        @Override
        public void undo() {
            for (int i = edits.size() - 1; i >= 0; i--) edits.get(i).undo();
        }
    }
}
