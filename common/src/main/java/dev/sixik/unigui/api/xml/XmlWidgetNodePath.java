package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Стабильный путь исходного документа к XML-узлу, выраженный индексами дочерних узлов от корня.
 *
 * <p>Path не зависит от object identity и поэтому подходит для selection model, undoable edits
 * и сообщений инспектора. Корень документа представлен пустым списком индексов и строкой {@code /}.</p>
 *
 * @param indexes индексы children от root element до целевого узла
 */
public record XmlWidgetNodePath(List<Integer> indexes) {
    public XmlWidgetNodePath {
        indexes = List.copyOf(indexes == null ? List.of() : indexes);
        for (Integer index : indexes) {
            if (index == null || index < 0) {
                throw new IllegalArgumentException("XML widget node path indexes must be non-negative");
            }
        }
    }

    /**
     * Возвращает path корневого XML-элемента.
     *
     * @return root path
     */
    public static XmlWidgetNodePath root() {
        return new XmlWidgetNodePath(List.of());
    }

    /**
     * Создаёт path из последовательности child indexes.
     *
     * @param indexes индексы от root к целевому узлу
     * @return node path или root path для пустого массива
     */
    public static XmlWidgetNodePath of(int... indexes) {
        if (indexes == null || indexes.length == 0) return root();
        return new XmlWidgetNodePath(Arrays.stream(indexes).boxed().toList());
    }

    /**
     * Проверяет, что path указывает на корневой элемент.
     *
     * @return {@code true}, если список индексов пуст
     */
    public boolean rootPath() {
        return indexes.isEmpty();
    }

    /**
     * Возвращает глубину path.
     *
     * @return количество индексов от root до узла
     */
    public int depth() {
        return indexes.size();
    }

    /**
     * Возвращает path дочернего узла относительно текущего path.
     *
     * @param childIndex индекс child node; должен быть неотрицательным
     * @return новый path с добавленным индексом
     */
    public XmlWidgetNodePath child(int childIndex) {
        if (childIndex < 0) throw new IllegalArgumentException("XML widget child index must be non-negative");
        List<Integer> next = new ArrayList<>(indexes);
        next.add(childIndex);
        return new XmlWidgetNodePath(next);
    }

    /**
     * Возвращает parent path.
     *
     * @return parent path или empty для root path
     */
    public Optional<XmlWidgetNodePath> parent() {
        if (indexes.isEmpty()) return Optional.empty();
        return Optional.of(new XmlWidgetNodePath(indexes.subList(0, indexes.size() - 1)));
    }

    /**
     * Находит узел внутри документа.
     *
     * @param document XML document
     * @return найденный узел или empty, если path больше невалиден
     */
    public Optional<XmlWidgetNode> resolve(XmlWidgetDocument document) {
        if (document == null) return Optional.empty();
        return resolve(document.root());
    }

    /**
     * Находит узел относительно указанного root element.
     *
     * @param root корневой element node
     * @return найденный узел или empty, если path выходит за границы дерева
     */
    public Optional<XmlWidgetNode> resolve(XmlWidgetElement root) {
        if (root == null) return Optional.empty();
        XmlWidgetNode current = root;
        for (int index : indexes) {
            if (!(current instanceof XmlWidgetElement element)) return Optional.empty();
            if (index >= element.children().size()) return Optional.empty();
            current = element.children().get(index);
        }
        return Optional.of(current);
    }

    /**
     * Находит узел и возвращает его только если это element node.
     *
     * @param document XML document
     * @return найденный element или empty
     */
    public Optional<XmlWidgetElement> resolveElement(XmlWidgetDocument document) {
        return resolve(document).filter(XmlWidgetElement.class::isInstance).map(XmlWidgetElement.class::cast);
    }

    /**
     * Находит узел относительно root и возвращает его только если это element node.
     *
     * @param root корневой element node
     * @return найденный element или empty
     */
    public Optional<XmlWidgetElement> resolveElement(XmlWidgetElement root) {
        return resolve(root).filter(XmlWidgetElement.class::isInstance).map(XmlWidgetElement.class::cast);
    }

    /**
     * Возвращает path в компактном slash-формате.
     *
     * @return {@code /} для root или последовательность вида {@code /0/2/1}
     */
    @Override
    public String toString() {
        if (indexes.isEmpty()) return "/";
        StringBuilder builder = new StringBuilder();
        for (int index : indexes) {
            builder.append('/').append(index);
        }
        return builder.toString();
    }
}
