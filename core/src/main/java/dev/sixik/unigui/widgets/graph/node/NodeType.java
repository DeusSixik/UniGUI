package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;

import java.util.Objects;
import java.util.function.Function;

/**
 * Описание типа ноды: id, название, категория и фабрика, собирающая ноду из модулей.
 *
 * <p>Тип — единица каталога ({@link NodeTypeRegistry}): по нему строится меню «добавить ноду», поиск и
 * восстановление нод из сохранения по {@link NodeGraphItem#typeId()}.</p>
 */
@AiDoc(
        summary = "Catalog entry for a node kind: id, title, category and a factory assembling the node from modules.",
        purpose = "Separates what a node is from where it is placed, enabling palettes, search and save/restore.",
        useWhen = {
                "Register Blueprint-like node kinds (events, functions, variables) with categories.",
                "Spawn a node by type id from a context menu or when restoring a snapshot."
        },
        criticalRules = {
                "The factory receives the new item id and must return a fresh node on every call.",
                "`create` stamps the node with this type's id."
        },
        related = {"NodeTypeRegistry", "NodeBuilder", "NodeGraphItem"})
public final class NodeType {
    private final String id;
    private final String title;
    private final String category;
    private final String description;
    private final Function<String, NodeGraphItem> factory;

    private NodeType(String id, String title, String category, String description,
                     Function<String, NodeGraphItem> factory) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = title == null || title.isBlank() ? id : title;
        this.category = category == null ? "" : category;
        this.description = description == null ? "" : description;
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    /**
     * @param id       уникальный id типа
     * @param title    название для меню и поиска
     * @param category категория, допустим путь через {@code /}
     * @param factory  фабрика ноды по id экземпляра
     * @return новый тип
     */
    public static NodeType of(String id, String title, String category, Function<String, NodeGraphItem> factory) {
        return new NodeType(id, title, category, "", factory);
    }

    /**
     * @param description описание для подсказки
     * @return копия с описанием
     */
    public NodeType description(String description) {
        return new NodeType(id, title, category, description, factory);
    }

    /**
     * Создаёт ноду этого типа.
     *
     * @param itemId id создаваемой ноды
     * @return новая нода с проставленным {@link NodeGraphItem#typeId()}
     */
    public NodeGraphItem create(String itemId) {
        NodeGraphItem item = Objects.requireNonNull(factory.apply(itemId), "factory returned null for " + id);
        item.typeId(id);
        return item;
    }

    /**
     * @return id типа
     */
    public String id() {
        return id;
    }

    /**
     * @return название
     */
    public String title() {
        return title;
    }

    /**
     * @return категория
     */
    public String category() {
        return category;
    }

    /**
     * @return описание
     */
    public String description() {
        return description;
    }
}
