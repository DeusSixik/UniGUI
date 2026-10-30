package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Изменяемый XML-элемент исходного документа для сценариев редактора и document workflow.
 *
 * <p>Элемент хранит имя тега, исходную позицию в XML, ordered attributes и ordered children.
 * Модель намеренно mutable: inspector, drag/drop дерево и undoable edits меняют её напрямую,
 * а затем сериализуют обратно через {@link XmlWidgetDocument#toXmlString()}.</p>
 */
public final class XmlWidgetElement implements XmlWidgetNode {
    private final String name;
    private final int line;
    private final int column;
    private final List<XmlWidgetAttribute> attributes = new ArrayList<>();
    private final List<XmlWidgetNode> children = new ArrayList<>();

    /**
     * Создаёт элемент без информации о source location.
     *
     * @param name XML tag name
     */
    public XmlWidgetElement(String name) {
        this(name, -1, -1);
    }

    /**
     * Создаёт элемент с позицией из исходного XML.
     *
     * @param name XML tag name
     * @param line номер строки или {@code -1}, если позиция неизвестна
     * @param column номер колонки или {@code -1}, если позиция неизвестна
     */
    public XmlWidgetElement(String name, int line, int column) {
        this.name = normalizeName(name);
        this.line = line;
        this.column = column;
    }

    /**
     * Возвращает имя XML-тега.
     *
     * @return нормализованное имя элемента
     */
    public String name() {
        return name;
    }

    /**
     * Возвращает исходную строку элемента.
     *
     * @return номер строки или {@code -1}, если позиция неизвестна
     */
    public int line() {
        return line;
    }

    /**
     * Возвращает исходную колонку элемента.
     *
     * @return номер колонки или {@code -1}, если позиция неизвестна
     */
    public int column() {
        return column;
    }

    /**
     * Проверяет, есть ли у элемента source location.
     *
     * @return {@code true}, если line и column заданы
     */
    public boolean hasLocation() {
        return line >= 0 && column >= 0;
    }

    /**
     * Проверяет, является ли элемент property-child тегом вида {@code Panel.Children}.
     *
     * @return {@code true}, если имя содержит точку не в начале и не в конце
     */
    public boolean propertyElement() {
        int dot = name.indexOf('.');
        return dot > 0 && dot < name.length() - 1;
    }

    /**
     * Возвращает значение атрибута по имени.
     *
     * @param name XML attribute name
     * @return значение атрибута или {@link Optional#empty()}, если его нет
     */
    public Optional<String> attribute(String name) {
        String normalized = normalizeName(name);
        for (XmlWidgetAttribute attribute : attributes) {
            if (attribute.name().equals(normalized)) return Optional.of(attribute.value());
        }
        return Optional.empty();
    }

    /**
     * Возвращает значение атрибута или fallback.
     *
     * @param name XML attribute name
     * @param fallback значение по умолчанию
     * @return значение атрибута, если он задан, иначе fallback
     */
    public String attributeOrDefault(String name, String fallback) {
        return attribute(name).orElse(fallback);
    }

    /**
     * Fluent-алиас для установки атрибута без source location.
     *
     * @param name XML attribute name
     * @param value XML attribute value; {@code null} станет пустой строкой
     * @return этот элемент
     */
    public XmlWidgetElement attribute(String name, String value) {
        setAttribute(name, value);
        return this;
    }

    /**
     * Fluent-алиас для установки атрибута с source location.
     *
     * @param name XML attribute name
     * @param value XML attribute value; {@code null} станет пустой строкой
     * @param line номер строки атрибута
     * @param column номер колонки атрибута
     * @return этот элемент
     */
    public XmlWidgetElement attribute(String name, String value, int line, int column) {
        setAttribute(new XmlWidgetAttribute(name, value, line, column));
        return this;
    }

    /**
     * Добавляет или заменяет атрибут по имени.
     *
     * @param name XML attribute name
     * @param value XML attribute value; {@code null} станет пустой строкой
     * @return этот элемент
     */
    public XmlWidgetElement setAttribute(String name, String value) {
        return setAttribute(new XmlWidgetAttribute(name, value));
    }

    /**
     * Добавляет или заменяет готовый attribute node.
     *
     * <p>Порядок существующего атрибута сохраняется при замене; новый атрибут добавляется в конец.</p>
     *
     * @param next новый attribute node; {@code null} игнорируется
     * @return этот элемент
     */
    public XmlWidgetElement setAttribute(XmlWidgetAttribute next) {
        if (next == null) return this;
        for (int i = 0; i < attributes.size(); i++) {
            if (attributes.get(i).name().equals(next.name())) {
                attributes.set(i, next);
                return this;
            }
        }
        attributes.add(next);
        return this;
    }

    /**
     * Удаляет атрибут по имени.
     *
     * @param name XML attribute name
     * @return {@code true}, если атрибут был найден и удалён
     */
    public boolean removeAttribute(String name) {
        String normalized = normalizeName(name);
        return attributes.removeIf(attribute -> attribute.name().equals(normalized));
    }

    /**
     * Возвращает атрибуты в порядке документа.
     *
     * @return read-only view атрибутов
     */
    public List<XmlWidgetAttribute> attributes() {
        return java.util.Collections.unmodifiableList(attributes);
    }

    /**
     * Возвращает дочерние узлы в порядке документа.
     *
     * @return read-only view children
     */
    public List<XmlWidgetNode> children() {
        return java.util.Collections.unmodifiableList(children);
    }

    /**
     * Возвращает только дочерние XML-элементы.
     *
     * @return immutable список element children без text/comment nodes
     */
    public List<XmlWidgetElement> elementChildren() {
        List<XmlWidgetElement> elements = new ArrayList<>();
        for (XmlWidgetNode child : children) {
            if (child instanceof XmlWidgetElement element) elements.add(element);
        }
        return List.copyOf(elements);
    }

    /**
     * Добавляет дочерний узел в конец.
     *
     * @param child дочерний узел; {@code null} игнорируется
     * @return этот элемент
     */
    public XmlWidgetElement addChild(XmlWidgetNode child) {
        if (child != null) children.add(child);
        return this;
    }

    /**
     * Вставляет дочерний узел по индексу.
     *
     * <p>Индекс clamp'ится в диапазон {@code [0, children.size()]}.</p>
     *
     * @param index желаемая позиция вставки
     * @param child дочерний узел; {@code null} игнорируется
     * @return этот элемент
     */
    public XmlWidgetElement insertChild(int index, XmlWidgetNode child) {
        if (child == null) return this;
        children.add(Math.max(0, Math.min(index, children.size())), child);
        return this;
    }

    /**
     * Удаляет дочерний узел по индексу.
     *
     * @param index индекс child node
     * @return удалённый узел или empty, если индекс вне диапазона
     */
    public Optional<XmlWidgetNode> removeChild(int index) {
        if (index < 0 || index >= children.size()) return Optional.empty();
        return Optional.of(children.remove(index));
    }

    /**
     * Перемещает дочерний узел внутри children list.
     *
     * @param fromIndex текущий индекс узла
     * @param toIndex желаемый индекс после перемещения
     * @return {@code true}, если исходный индекс существовал
     */
    public boolean moveChild(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= children.size()) return false;
        int normalizedTo = Math.max(0, Math.min(toIndex, children.size() - 1));
        if (fromIndex == normalizedTo) return true;
        XmlWidgetNode child = children.remove(fromIndex);
        children.add(normalizedTo, child);
        return true;
    }

    /**
     * Добавляет дочерний element node.
     *
     * @param child дочерний элемент
     * @return этот элемент
     */
    public XmlWidgetElement addElement(XmlWidgetElement child) {
        return addChild(child);
    }

    /**
     * Добавляет текстовый child node.
     *
     * @param text текст узла; {@code null} станет пустой строкой
     * @return этот элемент
     */
    public XmlWidgetElement addText(String text) {
        return addChild(new XmlWidgetText(text));
    }

    /**
     * Добавляет XML comment child node.
     *
     * @param text текст комментария без {@code <!-- -->}
     * @return этот элемент
     */
    public XmlWidgetElement addComment(String text) {
        return addChild(new XmlWidgetComment(text));
    }

    /**
     * Удаляет все дочерние узлы.
     *
     * @return этот элемент
     */
    public XmlWidgetElement clearChildren() {
        children.clear();
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Kind kind() {
        return Kind.ELEMENT;
    }

    /**
     * Создаёт глубокую копию элемента, его атрибутов и детей.
     *
     * @return независимая копия element subtree
     */
    @Override
    public XmlWidgetElement copy() {
        XmlWidgetElement copy = new XmlWidgetElement(name, line, column);
        copy.attributes.addAll(attributes);
        for (XmlWidgetNode child : children) {
            copy.children.add(child.copy());
        }
        return copy;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("XML element name must not be blank");
        }
        return normalized;
    }
}
