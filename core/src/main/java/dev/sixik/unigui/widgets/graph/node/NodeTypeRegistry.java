package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Каталог типов нод с поиском для меню добавления.
 */
@AiDoc(
        summary = "Catalog of NodeTypes with category grouping and ranked text search.",
        purpose = "Backs 'add node' menus and snapshot restore for modular node editors.",
        useWhen = {
                "Register all node kinds of an editor once and reuse them in menus and persistence.",
                "Implement type-ahead search over node titles and categories."
        },
        criticalRules = {
                "Registering an id twice replaces the previous type.",
                "Search is case-insensitive; prefix matches rank before substring matches."
        },
        related = {"NodeType", "NodeBuilder"})
@WikiPage(title = "NodeTypeRegistry", slug = "node-type-registry", category = "<key:graph.category>", summary = "<key:node.type.registry.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.type.registry.overview>", order = 10)
public class NodeTypeRegistry {
    private final Map<String, NodeType> types = new LinkedHashMap<>();

    /**
     * @param type регистрируемый тип; {@code null} игнорируется
     * @return этот реестр
     */
    public NodeTypeRegistry register(NodeType type) {
        if (type != null) types.put(type.id(), type);
        return this;
    }

    /**
     * @param id id типа
     * @return тип или {@code null}
     */
    public NodeType get(String id) {
        return id == null ? null : types.get(id);
    }

    /**
     * @return все типы в порядке регистрации
     */
    public Collection<NodeType> all() {
        return Collections.unmodifiableCollection(types.values());
    }

    /**
     * @param category точная категория
     * @return типы категории в порядке регистрации
     */
    public List<NodeType> byCategory(String category) {
        List<NodeType> result = new ArrayList<>();
        for (NodeType type : types.values()) {
            if (type.category().equals(category == null ? "" : category)) result.add(type);
        }
        return result;
    }

    /**
     * Ищет типы по названию, категории и id. Совпадения с начала названия идут первыми.
     *
     * @param query строка поиска; пустая возвращает все типы
     * @return найденные типы по убыванию релевантности
     */
    public List<NodeType> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return new ArrayList<>(types.values());
        List<NodeType> prefix = new ArrayList<>();
        List<NodeType> contains = new ArrayList<>();
        for (NodeType type : types.values()) {
            String title = type.title().toLowerCase(Locale.ROOT);
            if (title.startsWith(needle)) {
                prefix.add(type);
            } else if (title.contains(needle)
                    || type.category().toLowerCase(Locale.ROOT).contains(needle)
                    || type.id().toLowerCase(Locale.ROOT).contains(needle)) {
                contains.add(type);
            }
        }
        prefix.addAll(contains);
        return prefix;
    }
}
