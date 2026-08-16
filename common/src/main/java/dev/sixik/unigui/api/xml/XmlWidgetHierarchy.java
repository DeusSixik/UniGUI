package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Плоский снимок иерархии для дерева редактора поверх {@link XmlWidgetDocument}. */
public final class XmlWidgetHierarchy {
    private final List<Item> items;

    private XmlWidgetHierarchy(List<Item> items) {
        this.items = List.copyOf(items);
    }

    public static XmlWidgetHierarchy from(XmlWidgetDocument document) {
        if (document == null) throw new IllegalArgumentException("XML widget document must not be null");
        List<Item> items = new ArrayList<>();
        collect(document.root(), XmlWidgetNodePath.root(), 0, items);
        return new XmlWidgetHierarchy(items);
    }

    public List<Item> items() {
        return items;
    }

    public Optional<Item> item(XmlWidgetNodePath path) {
        if (path == null) return Optional.empty();
        return items.stream().filter(item -> item.path().equals(path)).findFirst();
    }

    public List<Item> elementItems() {
        return items.stream().filter(Item::element).toList();
    }

    private static void collect(XmlWidgetNode node, XmlWidgetNodePath path, int depth, List<Item> items) {
        items.add(Item.of(path, depth, node));
        if (node instanceof XmlWidgetElement element) {
            List<XmlWidgetNode> children = element.children();
            for (int i = 0; i < children.size(); i++) {
                collect(children.get(i), path.child(i), depth + 1, items);
            }
        }
    }

    public record Item(
            XmlWidgetNodePath path,
            int depth,
            XmlWidgetNode.Kind kind,
            String name,
            String id,
            boolean element,
            boolean propertyElement,
            int childCount,
            XmlWidgetNode node) {
        private static Item of(XmlWidgetNodePath path, int depth, XmlWidgetNode node) {
            if (node instanceof XmlWidgetElement element) {
                return new Item(
                        path,
                        depth,
                        node.kind(),
                        element.name(),
                        id(element),
                        true,
                        element.propertyElement(),
                        element.children().size(),
                        node);
            }
            if (node instanceof XmlWidgetComment comment) {
                return new Item(path, depth, node.kind(), "#comment", "", false, false, 0, comment);
            }
            return new Item(path, depth, node.kind(), "#text", "", false, false, 0, node);
        }

        public String label() {
            return id == null || id.isEmpty() ? name : name + "#" + id;
        }

        public Optional<XmlWidgetElement> asElement() {
            return node instanceof XmlWidgetElement element ? Optional.of(element) : Optional.empty();
        }

        private static String id(XmlWidgetElement element) {
            return element.attribute("id")
                    .or(() -> element.attribute("name"))
                    .or(() -> element.attribute("Name"))
                    .or(() -> element.attribute("x:Name"))
                    .orElse("");
        }
    }
}
