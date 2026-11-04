package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.widgets.graph.runtime.GraphModel;
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Правки графа компонента как обычные {@link Edit}: добавить и удалить ноду, поменять её атрибут, передвинуть,
 * соединить и разъединить пины. Правки идут через {@code EditorSession.apply}, поэтому попадают в историю отмены и
 * в то же событие изменения документа, что и правки виджетов.
 *
 * <p>Каждая правка проверяется до применения: после неё граф должен компилироваться ({@link GraphCompiler#check}),
 * иначе правка отклоняется с сообщением, которое называет причину и позицию. Проверка не требует полноты: пока
 * автор собирает граф, обязательные входы могут быть не подключены. Это правило применяется только при запуске.</p>
 *
 * <p>Ноды и провода адресуются по идентификаторам, а не по объектам: правка заменяет содержимое {@code Component.Graph}
 * целиком, поэтому идентичность {@link UiNode} внутри графа не сохраняется.</p>
 *
 * @see GraphDefinition
 */
public final class GraphEdits {
    private static final UiName GRAPH = UiName.of("Graph");

    private GraphEdits() {
    }

    // ------------------------------------------------------------------ чтение

    /**
     * Читает граф корня компонента без проверок типов.
     *
     * @param componentRoot корневой узел документа компонента
     * @return граф или пустой граф, если {@code Component.Graph} нет
     */
    public static GraphDefinition read(UiNode componentRoot) {
        UiProperty property = graphProperty(componentRoot).orElse(null);
        if (property == null) return GraphDefinition.empty();
        return GraphDefinition.read(property, (at, message) -> new XamlLoadException(message, at, null));
    }

    /**
     * Подбирает свободный идентификатор ноды вида {@code Math_Add_1}.
     *
     * @param componentRoot корневой узел документа компонента
     * @param type          тип ноды
     * @return идентификатор, которого в графе ещё нет
     */
    public static String freshId(UiNode componentRoot, String type) {
        String base = type.replaceAll("[^A-Za-z0-9_]", "_");
        List<String> used = new ArrayList<>();
        for (UiContent item : elements(componentRoot)) {
            if (item instanceof UiNode node && node.type().local().equals("Node")) {
                node.attributeText("Id").ifPresent(used::add);
            }
        }
        for (int i = 1; ; i++) {
            String candidate = base + "_" + i;
            if (!used.contains(candidate)) return candidate;
        }
    }

    // ------------------------------------------------------------------ правки

    /**
     * Добавляет ноду.
     *
     * @param componentRoot корневой узел документа компонента
     * @param id            идентификатор ноды
     * @param type          тип ({@code Math.Add}, {@code Event.Buy}…)
     * @param attributes    атрибуты ноды: значения по умолчанию пинов и параметры типа
     * @param x             положение в редакторе
     * @param y             положение в редакторе
     * @return правка
     */
    public static Edit addNode(UiNode componentRoot, String id, String type, Map<String, String> attributes,
                               float x, float y) {
        return build(componentRoot, "Add node " + type, items -> {
            if (indexOfNode(items, id) >= 0) throw new IllegalArgumentException("Graph node '" + id + "' already exists.");
            UiNode node = UiNode.of("Node").set("Id", id).set("Type", type);
            attributes.forEach(node::set);
            node.set("X", number(x)).set("Y", number(y));
            items.add(node);
        });
    }

    /**
     * Удаляет ноду вместе со всеми проводами, которые к ней подходят и от неё отходят.
     *
     * @param componentRoot корневой узел документа компонента
     * @param id            идентификатор ноды
     * @return правка
     */
    public static Edit removeNode(UiNode componentRoot, String id) {
        return build(componentRoot, "Remove node " + id, items -> {
            int index = indexOfNode(items, id);
            if (index < 0) throw new IllegalArgumentException("No graph node '" + id + "'.");
            items.remove(index);
            items.removeIf(item -> item instanceof UiNode wire && wire.type().local().equals("Wire")
                    && (touches(wire.attributeText("From", ""), id) || touches(wire.attributeText("To", ""), id)));
        });
    }

    /**
     * Задаёт или снимает атрибут ноды.
     *
     * @param componentRoot корневой узел документа компонента
     * @param nodeId        идентификатор ноды
     * @param name          имя атрибута
     * @param value         значение или {@code null}, чтобы снять
     * @return правка
     */
    public static Edit setAttribute(UiNode componentRoot, String nodeId, String name, String value) {
        return build(componentRoot, (value == null ? "Clear " : "Set ") + name + " on " + nodeId, items -> {
            int index = indexOfNode(items, nodeId);
            if (index < 0) throw new IllegalArgumentException("No graph node '" + nodeId + "'.");
            if (name.equals("Id") || name.equals("Type")) {
                throw new IllegalArgumentException("The " + name + " of a node cannot be changed; replace the node.");
            }
            UiNode copy = ((UiNode) items.get(index)).copy();
            if (value == null) copy.remove(null, UiName.of(name));
            else copy.set(name, value);
            items.set(index, copy);
        });
    }

    /**
     * Передвигает ноду в редакторе. Подряд идущие сдвиги одной ноды сливаются в один шаг отмены.
     *
     * @param componentRoot корневой узел документа компонента
     * @param nodeId        идентификатор ноды
     * @param x             новое положение
     * @param y             новое положение
     * @return правка
     */
    public static Edit moveNode(UiNode componentRoot, String nodeId, float x, float y) {
        GraphEdit edit = (GraphEdit) build(componentRoot, "Move node " + nodeId, items -> {
            int index = indexOfNode(items, nodeId);
            if (index < 0) throw new IllegalArgumentException("No graph node '" + nodeId + "'.");
            UiNode copy = ((UiNode) items.get(index)).copy();
            copy.set("X", number(x)).set("Y", number(y));
            items.set(index, copy);
        });
        edit.mergeKey = "move:" + nodeId;
        edit.layoutOnly = true;
        return edit;
    }

    /**
     * Соединяет выходной пин с входным. Если у входа данных уже был провод, он заменяется (вход данных принимает
     * один провод); входы потока принимают несколько.
     *
     * @param componentRoot корневой узел документа компонента
     * @param from          источник в виде {@code нода.пин}
     * @param to            приёмник в виде {@code нода.пин}
     * @return правка
     */
    public static Edit connect(UiNode componentRoot, String from, String to) {
        return build(componentRoot, "Connect " + from + " to " + to, items -> {
            for (UiContent item : items) {
                if (item instanceof UiNode wire && wire.type().local().equals("Wire")
                        && wire.attributeText("From", "").equals(from) && wire.attributeText("To", "").equals(to)) {
                    throw new IllegalArgumentException("Pins " + from + " and " + to + " are already connected.");
                }
            }
            // Вход данных принимает один провод: прежний уходит. Тип пина узнаём из текущего графа.
            if (isDataInput(componentRoot, to)) {
                items.removeIf(item -> item instanceof UiNode wire && wire.type().local().equals("Wire")
                        && wire.attributeText("To", "").equals(to));
            }
            items.add(UiNode.of("Wire").set("From", from).set("To", to));
        });
    }

    /**
     * Убирает провод.
     *
     * @param componentRoot корневой узел документа компонента
     * @param from          источник
     * @param to            приёмник
     * @return правка
     */
    public static Edit disconnect(UiNode componentRoot, String from, String to) {
        return build(componentRoot, "Disconnect " + from + " from " + to, items -> {
            boolean removed = items.removeIf(item -> item instanceof UiNode wire && wire.type().local().equals("Wire")
                    && wire.attributeText("From", "").equals(from) && wire.attributeText("To", "").equals(to));
            if (!removed) throw new IllegalArgumentException("No wire from " + from + " to " + to + ".");
        });
    }

    // ------------------------------------------------------------------ внутреннее

    private interface Change {
        void apply(List<UiContent> items);
    }

    private static Edit build(UiNode root, String label, Change change) {
        UiProperty before = graphProperty(root).orElse(null);
        List<UiContent> items = new ArrayList<>(elements(root));
        try {
            change.apply(items);
        } catch (IllegalArgumentException failure) {
            return new Rejected(label, failure.getMessage());
        }
        UiProperty after = new UiProperty(before == null ? GRAPH : before.name(),
                before == null ? root.type() : before.owner(), new UiValue.Elements(items),
                UiProperty.Form.ELEMENT, before == null ? null : before.location());
        return new GraphEdit(root, label, before, after);
    }

    private static boolean touches(String endpoint, String nodeId) {
        return endpoint.startsWith(nodeId + ".");
    }

    private static int indexOfNode(List<UiContent> items, String id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof UiNode node && node.type().local().equals("Node")
                    && id.equals(node.attributeText("Id", ""))) return i;
        }
        return -1;
    }

    private static String number(float value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Float.toString(value);
    }

    private static Optional<UiProperty> graphProperty(UiNode root) {
        for (UiProperty property : root.properties()) {
            if (property.owner() != null && property.owner().local().equals("Component")
                    && property.name().local().equals("Graph")) return Optional.of(property);
        }
        return Optional.empty();
    }

    private static List<UiContent> elements(UiNode root) {
        UiProperty property = graphProperty(root).orElse(null);
        if (property != null && property.value() instanceof UiValue.Elements elements) return elements.content();
        return List.of();
    }

    /** Вход данных? Решается по компиляции текущего графа; если он ещё не собирается, вход считается входом данных. */
    private static boolean isDataInput(UiNode root, String endpoint) {
        int dot = endpoint.indexOf('.');
        if (dot <= 0) return true;
        try {
            ComponentDefinition definition = ComponentDefinition.parse("edit", UiDocument.of(root));
            GraphModel model = GraphCompiler.check("edit", definition.graph(), definition.contract()).model();
            GraphModel.Node node = model.node(endpoint.substring(0, dot));
            GraphModel.Port port = node == null ? null : node.port(endpoint.substring(dot + 1));
            return port == null || !port.flow();
        } catch (RuntimeException failure) {
            return true;
        }
    }

    /** Правка, которую нельзя применить: причина доходит до диагностики редактора. */
    private record Rejected(String label, String reason) implements Edit {
        @Override
        public Optional<String> check(Schema schema) {
            return Optional.of(reason);
        }

        @Override
        public void apply() {
            throw new IllegalStateException(reason);
        }

        @Override
        public void undo() {
            throw new IllegalStateException(reason);
        }
    }

    private static final class GraphEdit implements Edit {
        private final UiNode root;
        private final String label;
        private final UiProperty before;
        private UiProperty after;
        private String mergeKey;
        private boolean layoutOnly;

        private GraphEdit(UiNode root, String label, UiProperty before, UiProperty after) {
            this.root = root;
            this.label = label;
            this.before = before;
            this.after = after;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public Optional<String> check(Schema schema) {
            // Сдвиг ноды не меняет смысл графа: компиляция ради него не нужна (и не должна мешать тащить ноду).
            if (layoutOnly) return Optional.empty();
            install(after);
            try {
                ComponentDefinition definition = ComponentDefinition.parse("edit", UiDocument.of(root));
                GraphCompiler.check("edit", definition.graph(), definition.contract());
                return Optional.empty();
            } catch (XamlLoadException failure) {
                return Optional.of(failure.getMessage());
            } finally {
                install(before);
            }
        }

        @Override
        public void apply() {
            install(after);
        }

        @Override
        public void undo() {
            install(before);
        }

        @Override
        public Edit mergeWith(Edit next) {
            if (mergeKey == null || !(next instanceof GraphEdit later) || !mergeKey.equals(later.mergeKey)
                    || later.root != root) {
                return null;
            }
            GraphEdit merged = new GraphEdit(root, label, before, later.after);
            merged.mergeKey = mergeKey;
            merged.layoutOnly = layoutOnly;
            return merged;
        }

        private void install(UiProperty property) {
            if (property != null) {
                root.set(property);
            } else if (after != null) {
                root.remove(after.owner(), after.name());
            }
        }
    }
}
