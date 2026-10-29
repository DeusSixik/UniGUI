package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Плоский снимок иерархии для дерева редактора поверх {@link XmlWidgetDocument}.
 *
 * <p>Hierarchy превращает source tree в список строк с depth/path metadata. Такой формат удобен
 * для tree view, поиска и selection sync: UI может отображать список, а операции редактирования
 * всё равно выполняются через стабильный {@link XmlWidgetNodePath}.</p>
 */
public final class XmlWidgetHierarchy {
    private final List<Item> items;

    private XmlWidgetHierarchy(List<Item> items) {
        this.items = List.copyOf(items);
    }

    /**
     * Создаёт hierarchy snapshot из документа.
     *
     * @param document исходный XML document; не может быть {@code null}
     * @return плоская hierarchy model
     */
    public static XmlWidgetHierarchy from(XmlWidgetDocument document) {
        if (document == null) throw new IllegalArgumentException("XML widget document must not be null");
        List<Item> items = new ArrayList<>();
        collect(document.root(), XmlWidgetNodePath.root(), 0, items);
        return new XmlWidgetHierarchy(items);
    }

    /**
     * Возвращает все строки hierarchy в preorder-порядке.
     *
     * @return immutable список items
     */
    public List<Item> items() {
        return items;
    }

    /**
     * Находит строку hierarchy по node path.
     *
     * @param path path узла
     * @return item или empty
     */
    public Optional<Item> item(XmlWidgetNodePath path) {
        if (path == null) return Optional.empty();
        return items.stream().filter(item -> item.path().equals(path)).findFirst();
    }

    /**
     * Возвращает только строки element nodes.
     *
     * @return список items, где {@link Item#element()} равен {@code true}
     */
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

    /**
     * Одна строка hierarchy tree.
     *
     * @param path стабильный path узла
     * @param depth глубина относительно root
     * @param kind тип source node
     * @param name display name узла
     * @param id значение id/name/x:Name, если задано
     * @param element является ли узел XML element-ом
     * @param propertyElement является ли element property-child тегом
     * @param childCount количество direct children для element node
     * @param node исходный node reference
     */
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

        /**
         * Возвращает подпись строки для tree view.
         *
         * @return {@code name} или {@code name#id}, если id задан
         */
        public String label() {
            return id == null || id.isEmpty() ? name : name + "#" + id;
        }

        /**
         * Возвращает node как {@link XmlWidgetElement}, если это element row.
         *
         * @return element или empty для text/comment rows
         */
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
