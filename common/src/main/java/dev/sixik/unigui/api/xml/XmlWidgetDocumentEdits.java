package dev.sixik.unigui.api.xml;

import java.util.Optional;

/**
 * Фабрики типовых undoable-команд редактора XML-документа.
 *
 * <p>Класс покрывает минимальный набор операций, из которых строятся inspector и hierarchy editor:
 * установка атрибута, добавление, удаление и перемещение дочерних узлов. Команды работают через
 * {@link XmlWidgetNodePath}, поэтому их можно хранить отдельно от конкретных object references.</p>
 */
public final class XmlWidgetDocumentEdits {
    private XmlWidgetDocumentEdits() {
    }

    /**
     * Создаёт команду установки или замены атрибута у элемента.
     *
     * @param elementPath path целевого элемента; {@code null} означает root
     * @param name имя атрибута
     * @param value новое значение атрибута
     * @return undoable edit
     */
    public static XmlWidgetDocumentEdit setAttribute(XmlWidgetNodePath elementPath, String name, String value) {
        return new SetAttributeEdit(elementPath, new XmlWidgetAttribute(name, value));
    }

    /**
     * Создаёт команду удаления атрибута у элемента.
     *
     * @param elementPath path целевого элемента; {@code null} означает root
     * @param name имя атрибута
     * @return undoable edit
     */
    public static XmlWidgetDocumentEdit removeAttribute(XmlWidgetNodePath elementPath, String name) {
        return new RemoveAttributeEdit(elementPath, name);
    }

    /**
     * Создаёт команду добавления child node к элементу.
     *
     * <p>Child копируется при создании команды и ещё раз при apply, чтобы повторное применение
     * не переиспользовало mutable subtree.</p>
     *
     * @param parentPath path родительского элемента; {@code null} означает root
     * @param index желаемая позиция вставки
     * @param child узел, который нужно добавить
     * @return undoable edit
     */
    public static XmlWidgetDocumentEdit addChild(XmlWidgetNodePath parentPath, int index, XmlWidgetNode child) {
        return new AddChildEdit(parentPath, index, child);
    }

    /**
     * Создаёт команду удаления child node.
     *
     * @param childPath path удаляемого узла; root удалить нельзя
     * @return undoable edit
     */
    public static XmlWidgetDocumentEdit removeChild(XmlWidgetNodePath childPath) {
        return new RemoveChildEdit(childPath);
    }

    /**
     * Создаёт команду перемещения child node внутри одного родителя.
     *
     * @param parentPath path родительского элемента; {@code null} означает root
     * @param fromIndex текущий индекс child node
     * @param toIndex целевой индекс child node
     * @return undoable edit
     */
    public static XmlWidgetDocumentEdit moveChild(XmlWidgetNodePath parentPath, int fromIndex, int toIndex) {
        return new MoveChildEdit(parentPath, fromIndex, toIndex);
    }

    private static XmlWidgetElement element(XmlWidgetDocument document, XmlWidgetNodePath path) {
        return path.resolveElement(document)
                .orElseThrow(() -> new XmlWidgetLoadException("XML document edit target '" + path + "' is not an element."));
    }

    private static final class SetAttributeEdit implements XmlWidgetDocumentEdit {
        private final XmlWidgetNodePath elementPath;
        private final XmlWidgetAttribute next;
        private XmlWidgetAttribute previous;
        private boolean hadPrevious;

        private SetAttributeEdit(XmlWidgetNodePath elementPath, XmlWidgetAttribute next) {
            this.elementPath = elementPath == null ? XmlWidgetNodePath.root() : elementPath;
            this.next = next;
        }

        @Override
        public String description() {
            return "Set attribute " + next.name();
        }

        @Override
        public void apply(XmlWidgetDocument document) {
            XmlWidgetElement element = element(document, elementPath);
            Optional<XmlWidgetAttribute> existing = element.attributes().stream()
                    .filter(attribute -> attribute.name().equals(next.name()))
                    .findFirst();
            hadPrevious = existing.isPresent();
            previous = existing.orElse(null);
            element.setAttribute(next);
        }

        @Override
        public void undo(XmlWidgetDocument document) {
            XmlWidgetElement element = element(document, elementPath);
            if (hadPrevious) {
                element.setAttribute(previous);
            } else {
                element.removeAttribute(next.name());
            }
        }
    }

    private static final class RemoveAttributeEdit implements XmlWidgetDocumentEdit {
        private final XmlWidgetNodePath elementPath;
        private final String name;
        private XmlWidgetAttribute previous;
        private boolean hadPrevious;

        private RemoveAttributeEdit(XmlWidgetNodePath elementPath, String name) {
            this.elementPath = elementPath == null ? XmlWidgetNodePath.root() : elementPath;
            this.name = new XmlWidgetAttribute(name, "").name();
        }

        @Override
        public String description() {
            return "Remove attribute " + name;
        }

        @Override
        public void apply(XmlWidgetDocument document) {
            XmlWidgetElement element = element(document, elementPath);
            Optional<XmlWidgetAttribute> existing = element.attributes().stream()
                    .filter(attribute -> attribute.name().equals(name))
                    .findFirst();
            hadPrevious = existing.isPresent();
            previous = existing.orElse(null);
            element.removeAttribute(name);
        }

        @Override
        public void undo(XmlWidgetDocument document) {
            if (hadPrevious && previous != null) {
                element(document, elementPath).setAttribute(previous);
            }
        }
    }

    private static final class AddChildEdit implements XmlWidgetDocumentEdit {
        private final XmlWidgetNodePath parentPath;
        private final int index;
        private final XmlWidgetNode child;
        private int appliedIndex = -1;

        private AddChildEdit(XmlWidgetNodePath parentPath, int index, XmlWidgetNode child) {
            if (child == null) throw new IllegalArgumentException("XML child edit node must not be null");
            this.parentPath = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
            this.index = index;
            this.child = child.copy();
        }

        @Override
        public String description() {
            return "Add child";
        }

        @Override
        public void apply(XmlWidgetDocument document) {
            XmlWidgetElement parent = element(document, parentPath);
            appliedIndex = Math.max(0, Math.min(index, parent.children().size()));
            parent.insertChild(appliedIndex, child.copy());
        }

        @Override
        public void undo(XmlWidgetDocument document) {
            if (appliedIndex < 0) return;
            element(document, parentPath).removeChild(appliedIndex);
        }
    }

    private static final class RemoveChildEdit implements XmlWidgetDocumentEdit {
        private final XmlWidgetNodePath childPath;
        private XmlWidgetNode removed;

        private RemoveChildEdit(XmlWidgetNodePath childPath) {
            if (childPath == null || childPath.rootPath()) {
                throw new IllegalArgumentException("XML remove-child edit requires a non-root child path");
            }
            this.childPath = childPath;
        }

        @Override
        public String description() {
            return "Remove child";
        }

        @Override
        public void apply(XmlWidgetDocument document) {
            XmlWidgetNodePath parentPath = childPath.parent().orElseThrow();
            XmlWidgetElement parent = element(document, parentPath);
            int index = childPath.indexes().get(childPath.indexes().size() - 1);
            removed = parent.removeChild(index)
                    .orElseThrow(() -> new XmlWidgetLoadException("XML document edit child '" + childPath + "' was not found."));
        }

        @Override
        public void undo(XmlWidgetDocument document) {
            if (removed == null) return;
            XmlWidgetNodePath parentPath = childPath.parent().orElseThrow();
            int index = childPath.indexes().get(childPath.indexes().size() - 1);
            element(document, parentPath).insertChild(index, removed);
        }
    }

    private static final class MoveChildEdit implements XmlWidgetDocumentEdit {
        private final XmlWidgetNodePath parentPath;
        private final int fromIndex;
        private final int toIndex;

        private MoveChildEdit(XmlWidgetNodePath parentPath, int fromIndex, int toIndex) {
            this.parentPath = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        @Override
        public String description() {
            return "Move child";
        }

        @Override
        public void apply(XmlWidgetDocument document) {
            if (!element(document, parentPath).moveChild(fromIndex, toIndex)) {
                throw new XmlWidgetLoadException("XML document edit move source index " + fromIndex + " was not found.");
            }
        }

        @Override
        public void undo(XmlWidgetDocument document) {
            if (!element(document, parentPath).moveChild(toIndex, fromIndex)) {
                throw new XmlWidgetLoadException("XML document edit move target index " + toIndex + " was not found.");
            }
        }
    }
}
