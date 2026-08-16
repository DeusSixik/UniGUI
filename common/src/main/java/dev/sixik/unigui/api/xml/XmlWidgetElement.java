package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Изменяемый XML-элемент исходного документа для сценариев редактора и document workflow. */
public final class XmlWidgetElement implements XmlWidgetNode {
    private final String name;
    private final int line;
    private final int column;
    private final List<XmlWidgetAttribute> attributes = new ArrayList<>();
    private final List<XmlWidgetNode> children = new ArrayList<>();

    public XmlWidgetElement(String name) {
        this(name, -1, -1);
    }

    public XmlWidgetElement(String name, int line, int column) {
        this.name = normalizeName(name);
        this.line = line;
        this.column = column;
    }

    public String name() {
        return name;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public boolean hasLocation() {
        return line >= 0 && column >= 0;
    }

    public boolean propertyElement() {
        int dot = name.indexOf('.');
        return dot > 0 && dot < name.length() - 1;
    }

    public Optional<String> attribute(String name) {
        String normalized = normalizeName(name);
        for (XmlWidgetAttribute attribute : attributes) {
            if (attribute.name().equals(normalized)) return Optional.of(attribute.value());
        }
        return Optional.empty();
    }

    public String attributeOrDefault(String name, String fallback) {
        return attribute(name).orElse(fallback);
    }

    public XmlWidgetElement attribute(String name, String value) {
        setAttribute(name, value);
        return this;
    }

    public XmlWidgetElement attribute(String name, String value, int line, int column) {
        setAttribute(new XmlWidgetAttribute(name, value, line, column));
        return this;
    }

    public XmlWidgetElement setAttribute(String name, String value) {
        return setAttribute(new XmlWidgetAttribute(name, value));
    }

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

    public boolean removeAttribute(String name) {
        String normalized = normalizeName(name);
        return attributes.removeIf(attribute -> attribute.name().equals(normalized));
    }

    public List<XmlWidgetAttribute> attributes() {
        return java.util.Collections.unmodifiableList(attributes);
    }

    public List<XmlWidgetNode> children() {
        return java.util.Collections.unmodifiableList(children);
    }

    public List<XmlWidgetElement> elementChildren() {
        List<XmlWidgetElement> elements = new ArrayList<>();
        for (XmlWidgetNode child : children) {
            if (child instanceof XmlWidgetElement element) elements.add(element);
        }
        return List.copyOf(elements);
    }

    public XmlWidgetElement addChild(XmlWidgetNode child) {
        if (child != null) children.add(child);
        return this;
    }

    public XmlWidgetElement insertChild(int index, XmlWidgetNode child) {
        if (child == null) return this;
        children.add(Math.max(0, Math.min(index, children.size())), child);
        return this;
    }

    public Optional<XmlWidgetNode> removeChild(int index) {
        if (index < 0 || index >= children.size()) return Optional.empty();
        return Optional.of(children.remove(index));
    }

    public boolean moveChild(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= children.size()) return false;
        int normalizedTo = Math.max(0, Math.min(toIndex, children.size() - 1));
        if (fromIndex == normalizedTo) return true;
        XmlWidgetNode child = children.remove(fromIndex);
        children.add(normalizedTo, child);
        return true;
    }

    public XmlWidgetElement addElement(XmlWidgetElement child) {
        return addChild(child);
    }

    public XmlWidgetElement addText(String text) {
        return addChild(new XmlWidgetText(text));
    }

    public XmlWidgetElement addComment(String text) {
        return addChild(new XmlWidgetComment(text));
    }

    public XmlWidgetElement clearChildren() {
        children.clear();
        return this;
    }

    @Override
    public Kind kind() {
        return Kind.ELEMENT;
    }

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
