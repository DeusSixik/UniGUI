package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.NodeTypeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Модель палитры нод без виджетов: дерево категорий или плоский результат поиска, выбор и окно прокрутки.
 *
 * <p>Без запроса палитра показывает дерево: путь категории {@code Math/Vector} даёт вложенные группы, группы можно
 * сворачивать и разворачивать, ноды без категории идут в корне после групп. Как только запрос не пуст, дерево
 * заменяется плоским списком из {@link NodeTypeRegistry#search(String)}: совпадения с начала названия идут первыми,
 * а категория остаётся подсказкой. Результаты дополнительно проходят фильтр, например «только ноды с входом,
 * совместимым с протянутым проводом».</p>
 */
@AiDoc(
        summary = "Widget-free model of the node palette: category tree or flat ranked search results, selection and a scrolling window.",
        purpose = "Keeps palette logic testable and reusable independently of how the list is drawn.",
        useWhen = {
                "Back a custom node picker with the same tree, ranking and keyboard navigation as NodePalette.",
                "Test palette behavior headless."
        },
        criticalRules = {
                "An empty query shows the category tree; any query shows a flat ranked list without category rows.",
                "Changing the query or the filter resets the selection to the first entry; toggling a category keeps it.",
                "`move` wraps around at both ends; the visible window always contains the selected entry.",
                "Categories without any matching node are never shown."
        },
        related = {"NodePalette", "NodeTypeRegistry", "NodeType"})
@WikiPage(title = "NodePaletteModel", slug = "node-palette-model", category = "<key:graph.category>", summary = "<key:node.palette.model.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.palette.model.overview>", order = 10)
public class NodePaletteModel {
    /**
     * Строка списка: группа категории или нода.
     */
    public sealed interface Entry permits CategoryEntry, NodeEntry {
        /**
         * @return глубина вложенности, от нуля
         */
        int depth();
    }

    /**
     * Группа категории в дереве.
     *
     * @param path     полный путь категории, например {@code Math/Vector}
     * @param title    последний сегмент пути
     * @param depth    глубина вложенности
     * @param expanded развёрнута ли группа
     * @param count    сколько нод лежит в группе вместе с вложенными
     */
    public record CategoryEntry(String path, String title, int depth, boolean expanded, int count) implements Entry {
    }

    /**
     * Нода в списке.
     *
     * @param type  тип ноды
     * @param depth глубина вложенности
     */
    public record NodeEntry(NodeType type, int depth) implements Entry {
    }

    private static final class Branch {
        private final String title;
        private final String path;
        private final TreeMap<String, Branch> children = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final List<NodeType> nodes = new ArrayList<>();
        private int count;

        private Branch(String title, String path) {
            this.title = title;
            this.path = path;
        }
    }

    private static final Comparator<NodeType> BY_TITLE =
            Comparator.comparing(NodeType::title, String.CASE_INSENSITIVE_ORDER);

    private final NodeTypeRegistry registry;
    private final int maxVisible;
    private final Set<String> expanded = new HashSet<>();
    private Predicate<NodeType> filter = type -> true;
    private boolean expandAll;
    private String query = "";
    private List<NodeType> results = List.of();
    private List<Entry> entries = List.of();
    private int selected;
    private int windowStart;

    /**
     * @param registry   каталог типов нод
     * @param maxVisible сколько строк показывает окно, не меньше 1
     */
    public NodePaletteModel(NodeTypeRegistry registry, int maxVisible) {
        this.registry = registry;
        this.maxVisible = Math.max(1, maxVisible);
        refresh(true);
    }

    /**
     * @return текущий запрос
     */
    public String query() {
        return query;
    }

    /**
     * Меняет запрос и сбрасывает выбор на первую строку.
     *
     * @param query строка поиска; {@code null} равен пустой
     * @return эта модель
     */
    public NodePaletteModel query(String query) {
        this.query = query == null ? "" : query;
        refresh(true);
        return this;
    }

    /**
     * Ограничивает результаты и сбрасывает выбор на первую строку.
     *
     * @param filter условие показа типа; {@code null} снимает ограничение
     * @return эта модель
     */
    public NodePaletteModel filter(Predicate<NodeType> filter) {
        this.filter = filter == null ? type -> true : filter;
        refresh(true);
        return this;
    }

    /**
     * @return {@code true}, если все группы показываются развёрнутыми
     */
    public boolean expandAll() {
        return expandAll;
    }

    /**
     * Показывает все группы развёрнутыми. Нужно для коротких отфильтрованных списков, где прятать ноды за
     * свёрнутыми группами незачем.
     *
     * @param expandAll {@code true}, чтобы развернуть всё
     * @return эта модель
     */
    public NodePaletteModel expandAll(boolean expandAll) {
        this.expandAll = expandAll;
        refresh(false);
        return this;
    }

    /**
     * @return найденные типы по убыванию релевантности, без группировки
     */
    public List<NodeType> results() {
        return Collections.unmodifiableList(results);
    }

    /**
     * @return все строки списка: дерево без запроса или плоский результат поиска
     */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * @return {@code true}, если показывать нечего
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @return сколько строк умещается в окне
     */
    public int maxVisible() {
        return maxVisible;
    }

    /**
     * @return индекс первой видимой строки в {@link #entries()}
     */
    public int windowStart() {
        return windowStart;
    }

    /**
     * @return строки окна
     */
    public List<Entry> visible() {
        int end = Math.min(entries.size(), windowStart + maxVisible);
        return Collections.unmodifiableList(entries.subList(windowStart, end));
    }

    /**
     * @return индекс выбранной строки в {@link #entries()} или {@code -1}, если список пуст
     */
    public int selectedIndex() {
        return entries.isEmpty() ? -1 : selected;
    }

    /**
     * @return выбранная строка или {@code null}, если список пуст
     */
    public Entry selected() {
        return entries.isEmpty() ? null : entries.get(selected);
    }

    /**
     * @return тип выбранной ноды или {@code null}, если выбрана группа или список пуст
     */
    public NodeType selectedType() {
        return selected() instanceof NodeEntry node ? node.type() : null;
    }

    /**
     * Сдвигает выбор с переходом через край.
     *
     * @param delta сдвиг строк, например {@code 1} вниз или {@code -1} вверх
     * @return эта модель
     */
    public NodePaletteModel move(int delta) {
        if (entries.isEmpty()) return this;
        selected = Math.floorMod(selected + delta, entries.size());
        keepSelectedVisible();
        return this;
    }

    /**
     * Выбирает строку по индексу в {@link #entries()}.
     *
     * @param index индекс; значение вне диапазона прижимается к границе
     * @return эта модель
     */
    public NodePaletteModel select(int index) {
        if (entries.isEmpty()) return this;
        selected = Math.max(0, Math.min(entries.size() - 1, index));
        keepSelectedVisible();
        return this;
    }

    /**
     * Прокручивает окно, не меняя выбор. Выбранная строка может уйти за край окна, пока не нажмут стрелку.
     *
     * @param delta на сколько строк сдвинуть окно, отрицательное — вверх
     * @return эта модель
     */
    public NodePaletteModel scroll(int delta) {
        windowStart = clampWindow(windowStart + delta);
        return this;
    }

    /**
     * Разворачивает или сворачивает группу, сохраняя выбор на той же строке.
     *
     * @param path путь категории
     * @return эта модель
     */
    public NodePaletteModel toggle(String path) {
        if (!expanded.remove(path)) expanded.add(path);
        refresh(false);
        return this;
    }

    /**
     * Разворачивает группу.
     *
     * @param path путь категории
     * @return эта модель
     */
    public NodePaletteModel expand(String path) {
        if (expanded.add(path)) refresh(false);
        return this;
    }

    /**
     * Сворачивает группу.
     *
     * @param path путь категории
     * @return эта модель
     */
    public NodePaletteModel collapse(String path) {
        if (expanded.remove(path)) refresh(false);
        return this;
    }

    /**
     * Разворачивает выбранную группу, если она свёрнута (стрелка вправо).
     */
    public NodePaletteModel expandSelected() {
        if (selected() instanceof CategoryEntry category && !category.expanded()) expand(category.path());
        return this;
    }

    /**
     * Стрелка влево: сворачивает выбранную развёрнутую группу, иначе переходит к родительской группе.
     */
    public NodePaletteModel collapseSelected() {
        Entry entry = selected();
        if (entry instanceof CategoryEntry category && category.expanded() && !expandAll) {
            collapse(category.path());
        } else if (entry != null && entry.depth() > 0) {
            for (int i = selected - 1; i >= 0; i--) {
                if (entries.get(i) instanceof CategoryEntry parent && parent.depth() == entry.depth() - 1) {
                    select(i);
                    break;
                }
            }
        }
        return this;
    }

    /**
     * Действие над выбранной строкой: группа разворачивается или сворачивается, нода возвращается для создания.
     *
     * @return тип выбранной ноды или {@code null}, если выбрана группа или список пуст
     */
    public NodeType activateSelected() {
        Entry entry = selected();
        if (entry instanceof CategoryEntry category) {
            toggle(category.path());
            return null;
        }
        return entry instanceof NodeEntry node ? node.type() : null;
    }

    private void refresh(boolean resetSelection) {
        Object keptKey = resetSelection ? null : selectedKey();
        List<NodeType> found = new ArrayList<>();
        for (NodeType type : registry.search(query)) {
            if (filter.test(type)) found.add(type);
        }
        results = found;
        entries = query.isBlank() ? buildTree(found) : buildFlat(found);
        if (resetSelection) {
            selected = 0;
            windowStart = 0;
            return;
        }
        int index = keptKey == null ? -1 : indexOfKey(keptKey);
        selected = index >= 0 ? index : Math.max(0, Math.min(selected, entries.size() - 1));
        keepSelectedVisible();
    }

    private Object selectedKey() {
        Entry entry = selected();
        if (entry instanceof CategoryEntry category) return "category:" + category.path();
        if (entry instanceof NodeEntry node) return "node:" + node.type().id();
        return null;
    }

    private int indexOfKey(Object key) {
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            Object candidate = entry instanceof CategoryEntry category
                    ? "category:" + category.path() : "node:" + ((NodeEntry) entry).type().id();
            if (Objects.equals(candidate, key)) return i;
        }
        return -1;
    }

    private static List<Entry> buildFlat(List<NodeType> found) {
        List<Entry> flat = new ArrayList<>(found.size());
        for (NodeType type : found) flat.add(new NodeEntry(type, 0));
        return flat;
    }

    private List<Entry> buildTree(List<NodeType> found) {
        Branch root = new Branch("", "");
        for (NodeType type : found) {
            Branch branch = root;
            StringBuilder path = new StringBuilder();
            for (String segment : type.category().split("/")) {
                String title = segment.trim();
                if (title.isEmpty()) continue;
                if (path.length() > 0) path.append('/');
                path.append(title);
                branch.count++;
                String fullPath = path.toString();
                branch = branch.children.computeIfAbsent(title, key -> new Branch(title, fullPath));
            }
            branch.nodes.add(type);
            branch.count++;
        }
        List<Entry> tree = new ArrayList<>();
        for (Branch child : root.children.values()) emit(child, 0, tree);
        root.nodes.stream().sorted(BY_TITLE).forEach(type -> tree.add(new NodeEntry(type, 0)));
        return tree;
    }

    private void emit(Branch branch, int depth, List<Entry> out) {
        boolean open = expandAll || expanded.contains(branch.path);
        out.add(new CategoryEntry(branch.path, branch.title, depth, open, branch.count));
        if (!open) return;
        for (Branch child : branch.children.values()) emit(child, depth + 1, out);
        branch.nodes.stream().sorted(BY_TITLE).forEach(type -> out.add(new NodeEntry(type, depth + 1)));
    }

    private void keepSelectedVisible() {
        if (selected < windowStart) {
            windowStart = selected;
        } else if (selected >= windowStart + maxVisible) {
            windowStart = selected - maxVisible + 1;
        }
        windowStart = clampWindow(windowStart);
    }

    private int clampWindow(int start) {
        return Math.max(0, Math.min(start, Math.max(0, entries.size() - maxVisible)));
    }
}
