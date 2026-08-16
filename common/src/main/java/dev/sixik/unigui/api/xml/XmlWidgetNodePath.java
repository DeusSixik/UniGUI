package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Стабильный путь исходного документа к XML-узлу, выраженный индексами дочерних узлов от корня. */
public record XmlWidgetNodePath(List<Integer> indexes) {
    public XmlWidgetNodePath {
        indexes = List.copyOf(indexes == null ? List.of() : indexes);
        for (Integer index : indexes) {
            if (index == null || index < 0) {
                throw new IllegalArgumentException("XML widget node path indexes must be non-negative");
            }
        }
    }

    public static XmlWidgetNodePath root() {
        return new XmlWidgetNodePath(List.of());
    }

    public static XmlWidgetNodePath of(int... indexes) {
        if (indexes == null || indexes.length == 0) return root();
        return new XmlWidgetNodePath(Arrays.stream(indexes).boxed().toList());
    }

    public boolean rootPath() {
        return indexes.isEmpty();
    }

    public int depth() {
        return indexes.size();
    }

    public XmlWidgetNodePath child(int childIndex) {
        if (childIndex < 0) throw new IllegalArgumentException("XML widget child index must be non-negative");
        List<Integer> next = new ArrayList<>(indexes);
        next.add(childIndex);
        return new XmlWidgetNodePath(next);
    }

    public Optional<XmlWidgetNodePath> parent() {
        if (indexes.isEmpty()) return Optional.empty();
        return Optional.of(new XmlWidgetNodePath(indexes.subList(0, indexes.size() - 1)));
    }

    public Optional<XmlWidgetNode> resolve(XmlWidgetDocument document) {
        if (document == null) return Optional.empty();
        return resolve(document.root());
    }

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

    public Optional<XmlWidgetElement> resolveElement(XmlWidgetDocument document) {
        return resolve(document).filter(XmlWidgetElement.class::isInstance).map(XmlWidgetElement.class::cast);
    }

    public Optional<XmlWidgetElement> resolveElement(XmlWidgetElement root) {
        return resolve(root).filter(XmlWidgetElement.class::isInstance).map(XmlWidgetElement.class::cast);
    }

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
