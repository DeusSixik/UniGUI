package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Плоское представление дерева документа для панели «Иерархия».
 *
 * <p>Строки идут в порядке обхода (родитель перед детьми), поэтому панель может показывать их списком с отступом
 * {@link Row#depth()}. Содержимое property-элементов входит в иерархию, но помечено
 * {@link Row#structureEditable()} {@code = false}: его свойства редактируются, а переставлять его нельзя.</p>
 */
public final class Hierarchy {
    private Hierarchy() {
    }

    /**
     * Строка иерархии.
     *
     * @param node              узел документа
     * @param path              адрес узла
     * @param depth             глубина; у корня 0
     * @param typeName          имя типа так, как оно записано в документе
     * @param id                {@code id} или {@code x:Name}; пустая строка, если не задан
     * @param known             {@code true}, если тип есть в схеме
     * @param structureEditable {@code false} для содержимого property-элементов
     * @param childCount        число вложенных узлов
     */
    public record Row(UiNode node, NodePath path, int depth, String typeName, String id, boolean known,
                      boolean structureEditable, int childCount) {
        /** @return подпись строки: {@code Тип} или {@code Тип #id} */
        public String label() {
            return id.isEmpty() ? typeName : typeName + " #" + id;
        }
    }

    /**
     * Строит строки для всего документа.
     *
     * @param document документ
     * @param schema   реестр типов (определяет {@link Row#known()}); может быть {@code null}
     * @return строки в порядке обхода
     */
    public static List<Row> rows(UiDocument document, Schema schema) {
        List<Row> rows = new ArrayList<>();
        collect(document.root(), NodePath.ROOT, 0, true, schema, rows);
        return rows;
    }

    /** Контракт и граф компонента — данные, а не виджеты: у них свои панели, в дереве им делать нечего. */
    private static boolean shown(UiNode node, UiProperty property) {
        if (!(property.value() instanceof UiValue.Elements)) return false;
        String slot = property.name().local();
        return !(node.type().local().equals("Component") && (slot.equals("Contract") || slot.equals("Graph")));
    }

    private static void collect(UiNode node, NodePath path, int depth, boolean editable, Schema schema, List<Row> rows) {
        List<NodePath.Step> base = path.steps();
        int slotCount = 0;
        for (UiProperty property : node.properties()) {
            if (shown(node, property)) slotCount += NodePath.nodesAt(node, property.name().local()).size();
        }
        rows.add(new Row(node, path, depth, node.type().qualified(), idOf(node),
                schema != null && schema.contains(node.type()), editable, node.children().size() + slotCount));

        for (UiProperty property : node.properties()) {
            if (!shown(node, property)) continue;
            String slot = property.name().local();
            List<UiNode> nodes = NodePath.nodesAt(node, slot);
            for (int i = 0; i < nodes.size(); i++) {
                collect(nodes.get(i), child(base, new NodePath.Step(slot, i)), depth + 1, false, schema, rows);
            }
        }
        List<UiNode> children = node.children();
        for (int i = 0; i < children.size(); i++) {
            collect(children.get(i), child(base, new NodePath.Step(null, i)), depth + 1, editable, schema, rows);
        }
    }

    private static NodePath child(List<NodePath.Step> base, NodePath.Step step) {
        List<NodePath.Step> steps = new ArrayList<>(base.size() + 1);
        steps.addAll(base);
        steps.add(step);
        return new NodePath(steps);
    }

    /**
     * Фильтр для поля поиска: оставляет строки, у которых тип или id содержат запрос (без учёта регистра).
     *
     * @param rows  строки
     * @param query запрос; пустой возвращает все строки
     * @return подходящие строки в прежнем порядке
     */
    public static List<Row> filter(List<Row> rows, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return rows;
        List<Row> matched = new ArrayList<>();
        for (Row row : rows) {
            if (row.typeName().toLowerCase(Locale.ROOT).contains(needle)
                    || row.id().toLowerCase(Locale.ROOT).contains(needle)) {
                matched.add(row);
            }
        }
        return matched;
    }

    /**
     * Находит родителя среди прямых детей.
     *
     * @param document документ
     * @param node     узел
     * @return родитель или пусто для корня и содержимого property-элементов
     */
    public static Optional<UiNode> parentOf(UiDocument document, UiNode node) {
        return Optional.ofNullable(Edits.parentOf(document.root(), node));
    }

    /** {@code id}, {@code name}, {@code Name} или {@code x:Name} узла; пустая строка, если не задан. */
    static String idOf(UiNode node) {
        for (UiProperty property : node.properties()) {
            String local = property.name().local();
            if (property.owner() == null && (local.equals("id") || local.equals("name") || local.equals("Name"))
                    && property.value() instanceof UiValue.Literal literal) {
                return literal.text().trim();
            }
        }
        return "";
    }
}
