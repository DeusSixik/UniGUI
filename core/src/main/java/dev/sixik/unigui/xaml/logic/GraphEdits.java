package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.widgets.graph.runtime.GraphModel;
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.load.XamlLoadException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Правки графа компонента как обычные {@link Edit}: добавить и удалить ноду, поменять её атрибут, передвинуть,
 * соединить и разъединить пины. Правки идут через {@code EditorSession.apply}, поэтому попадают в историю отмены и в
 * то же событие изменения документа, что и правки виджетов.
 *
 * <p>Каждая правка проверяется до применения: после неё граф должен компилироваться ({@link GraphCompiler#check}),
 * иначе правка отклоняется с сообщением, которое называет причину и позицию. Проверка не требует полноты: пока автор
 * собирает граф, обязательные входы могут быть не подключены. Это правило применяется только при запуске.</p>
 *
 * <p>Ноды и провода адресуются по идентификаторам, а не по объектам: правка заменяет содержимое {@code Component.Graph}
 * целиком, поэтому идентичность {@link UiNode} внутри графа не сохраняется.</p>
 *
 * @see GraphDefinition
 * @see ElementListEdit
 */
public final class GraphEdits {
    private static final String OWNER = "Component";
    private static final String MEMBER = "Graph";

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
        for (UiProperty property : componentRoot.properties()) {
            if (property.owner() != null && property.owner().local().equals(OWNER)
                    && property.name().local().equals(MEMBER)) {
                return GraphDefinition.read(property, (at, message) -> new XamlLoadException(message, at, null));
            }
        }
        return GraphDefinition.empty();
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
        for (UiContent item : ElementListEdit.items(componentRoot, OWNER, MEMBER)) {
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
        return edit(componentRoot, "Add node " + type, items -> {
            if (indexOfNode(items, id) >= 0) throw new IllegalArgumentException("Graph node '" + id + "' already exists.");
            UiNode node = UiNode.of("Node").set("Id", id).set("Type", type);
            attributes.forEach(node::set);
            node.set("X", number(x)).set("Y", number(y));
            items.add(node);
        }, true, null);
    }

    /**
     * Удаляет ноду вместе со всеми проводами, которые к ней подходят и от неё отходят.
     *
     * @param componentRoot корневой узел документа компонента
     * @param id            идентификатор ноды
     * @return правка
     */
    public static Edit removeNode(UiNode componentRoot, String id) {
        return edit(componentRoot, "Remove node " + id, items -> {
            int index = indexOfNode(items, id);
            if (index < 0) throw new IllegalArgumentException("No graph node '" + id + "'.");
            items.remove(index);
            items.removeIf(item -> item instanceof UiNode wire && wire.type().local().equals("Wire")
                    && (touches(wire.attributeText("From", ""), id) || touches(wire.attributeText("To", ""), id)));
        }, true, null);
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
        return edit(componentRoot, (value == null ? "Clear " : "Set ") + name + " on " + nodeId, items -> {
            int index = indexOfNode(items, nodeId);
            if (index < 0) throw new IllegalArgumentException("No graph node '" + nodeId + "'.");
            if (name.equals("Id") || name.equals("Type")) {
                throw new IllegalArgumentException("The " + name + " of a node cannot be changed; replace the node.");
            }
            UiNode copy = ((UiNode) items.get(index)).copy();
            if (value == null) copy.remove(null, UiName.of(name));
            else copy.set(name, value);
            items.set(index, copy);
        }, true, null);
    }

    /**
     * Передвигает ноду в редакторе. Подряд идущие сдвиги одной ноды сливаются в один шаг отмены; проверка при сдвиге
     * не нужна, потому что положение смысл графа не меняет.
     *
     * @param componentRoot корневой узел документа компонента
     * @param nodeId        идентификатор ноды
     * @param x             новое положение
     * @param y             новое положение
     * @return правка
     */
    public static Edit moveNode(UiNode componentRoot, String nodeId, float x, float y) {
        return edit(componentRoot, "Move node " + nodeId, items -> {
            int index = indexOfNode(items, nodeId);
            if (index < 0) throw new IllegalArgumentException("No graph node '" + nodeId + "'.");
            UiNode copy = ((UiNode) items.get(index)).copy();
            copy.set("X", number(x)).set("Y", number(y));
            items.set(index, copy);
        }, false, "move:" + nodeId);
    }

    /**
     * Соединяет выходной пин с входным. Если у входа данных уже был провод, он заменяется (вход данных принимает один
     * провод); входы потока принимают несколько.
     *
     * @param componentRoot корневой узел документа компонента
     * @param from          источник в виде {@code нода.пин}
     * @param to            приёмник в виде {@code нода.пин}
     * @return правка
     */
    public static Edit connect(UiNode componentRoot, String from, String to) {
        return edit(componentRoot, "Connect " + from + " to " + to, items -> {
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
        }, true, null);
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
        return edit(componentRoot, "Disconnect " + from + " from " + to, items -> {
            boolean removed = items.removeIf(item -> item instanceof UiNode wire && wire.type().local().equals("Wire")
                    && wire.attributeText("From", "").equals(from) && wire.attributeText("To", "").equals(to));
            if (!removed) throw new IllegalArgumentException("No wire from " + from + " to " + to + ".");
        }, true, null);
    }

    // ------------------------------------------------------------------ внутреннее

    private static Edit edit(UiNode root, String label, ElementListEdit.Change change, boolean verify, String mergeKey) {
        return ElementListEdit.of(root, OWNER, MEMBER, label, change, verify ? GraphEdits::verify : null, mergeKey);
    }

    /** После правки граф должен компилироваться в мягком режиме. */
    private static Optional<String> verify(UiNode root) {
        try {
            ComponentDefinition definition = ComponentDefinition.parse("edit", UiDocument.of(root));
            GraphCompiler.check("edit", definition.graph(), definition.contract());
            return Optional.empty();
        } catch (XamlLoadException failure) {
            return Optional.of(failure.getMessage());
        }
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
}
