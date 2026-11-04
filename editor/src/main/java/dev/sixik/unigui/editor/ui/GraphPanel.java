package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphConnection;
import dev.sixik.unigui.widgets.graph.NodeGraphConnectionPolicy;
import dev.sixik.unigui.widgets.graph.NodeGraphConnectionValidation;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPort;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.ValueEditors;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphCompiler;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphEdits;
import dev.sixik.unigui.xaml.logic.NodeCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Граф поведения компонента ({@code Component.Graph}) в виде полотна нод.
 *
 * <p>Документ остаётся единственным источником истины. Полотно — его отражение: любое жесты (перетащить ноду,
 * протянуть или убрать провод, изменить значение пина, удалить выбранное) превращаются в правки {@link GraphEdits} с
 * историей отмены, а отмена, повтор и правки текста перестраивают полотно по документу. Встроенные история, палитра и
 * клавиши копирования полотна отключены, чтобы они не расходились с документом. Правка, которую граф не принимает
 * (несовместимые типы, второй провод в вход данных…), откатывается вместе с жестом, а причина показывается под
 * полотном; провод, который не пройдёт проверку, не удаётся даже протянуть.</p>
 *
 * <p>Справа палитра нод по контракту ({@link NodeCatalog}). Идентификаторы виджетов для тестов и автоматизации:
 * {@code graph.view}, {@code graph.add.<тип>} у кнопок палитры, {@code graph.search}.</p>
 */
final class GraphPanel extends SessionPanel {
    /** Идентификатор панели в каркасе. */
    static final String ID = "graph";

    private static final MutableColor PROBLEM = MutableColor.rgba255(235, 100, 100, 255);
    private static final int ERROR_LEVEL = 2;

    private final NodeGraph graph = new NodeGraph();
    private final VBox palette = new VBox();
    private final TextField search = new TextField();
    private final Label problem = new Label("");
    private boolean syncing;
    private boolean applying;
    /** Причина последней отклонённой правки: переживает перестройку полотна, которая откатывает жест. */
    private String rejection = "";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Graph";
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.CENTER;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        VBox root = new VBox();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));

        graph.id("graph.view");
        graph.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).heightPercent(100.0f));
        // Документ — источник истины: собственная история, палитра и копирование полотна с ним разошлись бы.
        graph.keyboardEditingEnabled(false);
        graph.nodePaletteEnabled(false);
        graph.snapToGrid(true);
        graph.focusable(true);
        graph.connectionPolicy(this::policy);
        graph.onConnectionCreated(event -> {
            if (!syncing) connected(event.from(), event.to());
        });
        graph.onConnectionRemoved(event -> {
            if (!syncing) disconnected(event.from(), event.to());
        });
        graph.onItemRemoved(event -> {
            if (!syncing) removed(event.itemId());
        });
        graph.onItemMoveEnded(event -> {
            if (!syncing) moved(event.itemId());
        });
        graph.onValueChanged(event -> {
            if (!syncing) valueChanged(event.itemId(), event.portId(), event.serialized());
        });
        graph.on(KeyPressedEvent.TYPE, event -> {
            if (event.target() != graph) return;
            if (event.keyCode() == KeyCodes.DELETE || event.keyCode() == KeyCodes.BACKSPACE) {
                deleteSelection();
                event.cancel();
            }
        });

        VBox side = new VBox();
        side.spacing(4.0f);
        side.layout(style -> style.width(170.0f).flexShrink(0.0f).heightPercent(100.0f));
        search.id("graph.search");
        search.placeholder("Search nodes");
        search.onTextChanged(event -> refreshPalette());
        palette.spacing(1.0f);
        ScrollView scroll = new ScrollView(palette);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        side.addChild(search);
        side.addChild(scroll);
        side.applyQueuedMutations();

        HBox body = new HBox();
        body.spacing(4.0f);
        body.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        body.addChild(graph);
        body.addChild(side);
        body.applyQueuedMutations();

        problem.color(PROBLEM);
        problem.layout(style -> style.flexShrink(0.0f));
        root.addChild(body);
        root.addChild(problem);
        root.applyQueuedMutations();
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        rebuild();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED -> {
                if (!applying) {
                    rejection = "";
                    rebuild();
                }
            }
            case REVERTED -> {
                rejection = "";
                rebuild();
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ документ → полотно

    private UiNode root() {
        return session().map(session -> session.document().root()).orElse(null);
    }

    private boolean isComponent() {
        UiNode root = root();
        return root != null && root.type().local().equals("Component");
    }

    /** Контракт документа или {@code null}, если он сейчас не читается. */
    private Contract contract() {
        UiNode root = root();
        if (root == null || !isComponent()) return null;
        try {
            return ComponentDefinition.parse("graph", session().orElseThrow().document()).contract();
        } catch (XamlLoadException failure) {
            return null;
        }
    }

    private void rebuild() {
        syncing = true;
        try {
            graph.clearItems();
            refreshPalette();
            UiNode root = root();
            if (root == null || !isComponent()) {
                problem.text(root == null ? "" : "Not a component document");
                return;
            }
            Contract contract = contract();
            if (contract == null) {
                problem.text("The contract is not valid yet; fix it to edit the graph");
                return;
            }
            GraphDefinition definition = GraphEdits.read(root);
            List<String> errors = new ArrayList<>();
            for (GraphDefinition.Node node : definition.nodes()) {
                graph.addItem(item(contract, definition, node, errors));
            }
            int index = 0;
            for (GraphDefinition.Wire wire : definition.wires()) {
                NodeGraphPortRef from = new NodeGraphPortRef(wire.fromNode(), wire.fromPort());
                NodeGraphPortRef to = new NodeGraphPortRef(wire.toNode(), wire.toPort());
                if (graph.port(from) == null || graph.port(to) == null) continue; // нода с ошибкой пинов не имеет
                try {
                    graph.addConnection("w" + index++, from, to);
                } catch (IllegalArgumentException failure) {
                    errors.add("Wire " + wire.fromNode() + "." + wire.fromPort() + " -> " + wire.toNode() + "."
                            + wire.toPort() + ": " + failure.getMessage());
                }
            }
            problem.text(errors.isEmpty() ? rejection : errors.get(0));
            graph.history().clear();
        } finally {
            syncing = false;
        }
    }

    private NodeGraphItem item(Contract contract, GraphDefinition definition, GraphDefinition.Node node,
                               List<String> errors) {
        String title = NodeCatalog.titleOf(contract, node.type(), node.attributes());
        GraphCompiler.Shape shape;
        try {
            shape = GraphCompiler.shape(contract, definition, node);
        } catch (XamlLoadException failure) {
            // Нода, которую нельзя разобрать, остаётся на полотне с причиной, чтобы её можно было увидеть и удалить.
            NodeGraphItem broken = NodeBuilder.node(node.id()).at(node.x(), node.y()).type(node.type())
                    .color(0xFFB03A3A).header(title).build();
            broken.issues(ERROR_LEVEL, List.of(failure.getMessage()));
            errors.add(failure.getMessage());
            return broken;
        }
        NodeBuilder builder = NodeBuilder.node(node.id()).at(node.x(), node.y()).type(node.type())
                .color(colorOf(node.type())).header(title);

        for (String attribute : shape.attributes()) {
            if (shape.pins().stream().anyMatch(pin -> pin.input() && pin.id().equals(attribute))) continue;
            builder.widget("attr:" + attribute, attributeRow(node, attribute));
        }

        GraphCompiler.Pin execIn = null;
        List<GraphCompiler.Pin> execOut = new ArrayList<>();
        for (GraphCompiler.Pin pin : shape.pins()) {
            if (!pin.flow()) continue;
            if (pin.input()) execIn = pin;
            else execOut.add(pin);
        }
        if (execIn != null || !execOut.isEmpty()) {
            builder.flowRow(execIn == null ? null : execIn.id(), execOut.isEmpty() ? null : execOut.get(0).id());
            for (int i = 1; i < execOut.size(); i++) builder.flowRow(null, execOut.get(i).id());
        }
        for (GraphCompiler.Pin pin : shape.pins()) {
            if (pin.flow() || !pin.input()) continue;
            builder.input(pin.id(), pin.id(), pin.type(), ValueEditors.text(node.attributes().getOrDefault(pin.id(), "")));
        }
        for (GraphCompiler.Pin pin : shape.pins()) {
            if (!pin.flow() && !pin.input()) builder.output(pin.id(), pin.id(), pin.type());
        }
        return builder.build();
    }

    private Widget attributeRow(GraphDefinition.Node node, String attribute) {
        HBox row = new HBox();
        row.spacing(4.0f);
        Label name = new Label(attribute);
        TextField field = new TextField(node.attributes().getOrDefault(attribute, ""));
        field.id("graph.attr." + node.id() + "." + attribute);
        field.layout(style -> style.width(70.0f));
        field.onTextChanged(event -> {
            if (!syncing) attributeChanged(node.id(), attribute, field.text());
        });
        row.addChild(name);
        row.addChild(field);
        row.applyQueuedMutations();
        return row;
    }

    private static int colorOf(String type) {
        String group = type.contains(".") ? type.substring(0, type.indexOf('.')) : type;
        return switch (group) {
            case "Event" -> 0xFFD9534F;
            case "Flow" -> 0xFF5D6B7A;
            case "Math", "Logic", "Text", "Value" -> 0xFF8E44AD;
            case "Param", "Var" -> 0xFF3B6EA5;
            case "Raise" -> 0xFFE08A2E;
            case "Call" -> 0xFF2E8B57;
            default -> 0xFF4F6D7A;
        };
    }

    // ------------------------------------------------------------------ палитра

    private void refreshPalette() {
        palette.clearChildren();
        Contract contract = isComponent() ? contract() : null;
        if (contract != null) {
            String query = search.text().trim().toLowerCase(Locale.ROOT);
            String category = null;
            for (NodeCatalog.Entry entry : NodeCatalog.entries(contract)) {
                if (!query.isEmpty() && !entry.title().toLowerCase(Locale.ROOT).contains(query)
                        && !entry.type().toLowerCase(Locale.ROOT).contains(query)) continue;
                if (!entry.category().equals(category)) {
                    category = entry.category();
                    palette.addChild(new Label(category));
                }
                Button button = new Button("  " + entry.title());
                button.id("graph.add." + entry.type() + (entry.attributes().containsKey("Name")
                        ? "." + entry.attributes().get("Name") : ""));
                button.onClick(click -> addNode(entry));
                palette.addChild(button);
            }
        }
        palette.applyQueuedMutations();
    }

    private void addNode(NodeCatalog.Entry entry) {
        UiNode root = root();
        if (root == null) return;
        int count = GraphEdits.read(root).nodes().size();
        float x = 30.0f + (count % 6) * 40.0f;
        float y = 30.0f + (count % 6) * 40.0f + (count / 6) * 20.0f;
        String id = GraphEdits.freshId(root, entry.type());
        if (commit(GraphEdits.addNode(root, id, entry.type(), entry.attributes(), x, y))) {
            rebuild();
            graph.selectItem(id);
        }
    }

    // ------------------------------------------------------------------ полотно → документ

    private NodeGraphConnectionValidation policy(NodeGraph view, NodeGraphPortRef from, NodeGraphPortRef to) {
        if (from == null || to == null || from.empty() || to.empty()) {
            return NodeGraphConnectionValidation.invalid("Missing port");
        }
        NodeGraphPort source = view.port(from);
        NodeGraphPort target = view.port(to);
        if (source == null || target == null) return NodeGraphConnectionValidation.invalid("Port not found");
        if (syncing) return NodeGraphConnectionValidation.accepted(); // восстановление из документа не перепроверяется
        if (source.kind() != NodeGraphPortKind.OUTPUT || target.kind() != NodeGraphPortKind.INPUT) {
            return NodeGraphConnectionValidation.invalid("A wire goes from an output to an input");
        }
        if (view.hasConnection(from, to)) return NodeGraphConnectionValidation.invalid("Connection already exists");
        UiNode root = root();
        EditorSession session = session().orElse(null);
        if (root == null || session == null) return NodeGraphConnectionValidation.invalid("No document");
        Optional<String> reason = GraphEdits.connect(root, endpoint(from), endpoint(to)).check(session.schema());
        return reason.map(NodeGraphConnectionValidation::invalid).orElseGet(NodeGraphConnectionValidation::accepted);
    }

    private static String endpoint(NodeGraphPortRef ref) {
        return ref.itemId() + "." + ref.portId();
    }

    private void connected(NodeGraphPortRef from, NodeGraphPortRef to) {
        UiNode root = root();
        if (root == null) return;
        if (!commit(GraphEdits.connect(root, endpoint(from), endpoint(to)))) rebuild();
    }

    private void disconnected(NodeGraphPortRef from, NodeGraphPortRef to) {
        UiNode root = root();
        if (root == null) return;
        if (!commit(GraphEdits.disconnect(root, endpoint(from), endpoint(to)))) rebuild();
    }

    private void removed(String itemId) {
        UiNode root = root();
        if (root == null) return;
        if (!commit(GraphEdits.removeNode(root, itemId))) rebuild();
    }

    private void moved(String itemId) {
        UiNode root = root();
        NodeGraphItem item = graph.item(itemId);
        if (root == null || item == null) return;
        commit(GraphEdits.moveNode(root, itemId, item.x(), item.y()));
    }

    private void valueChanged(String itemId, String pin, String text) {
        UiNode root = root();
        if (root == null) return;
        commit(GraphEdits.setAttribute(root, itemId, pin, text == null || text.isEmpty() ? null : text));
    }

    /** Атрибут ноды изменён. Если от него зависят пины, полотно перестраивается. */
    private void attributeChanged(String itemId, String attribute, String text) {
        UiNode root = root();
        if (root == null) return;
        String before = pinSignature(root, itemId);
        if (!commit(GraphEdits.setAttribute(root, itemId, attribute, text.isEmpty() ? null : text))) return;
        if (!before.equals(pinSignature(root, itemId))) rebuild();
    }

    private String pinSignature(UiNode root, String itemId) {
        Contract contract = contract();
        if (contract == null) return "";
        GraphDefinition definition = GraphEdits.read(root);
        for (GraphDefinition.Node node : definition.nodes()) {
            if (!node.id().equals(itemId)) continue;
            try {
                return GraphCompiler.shape(contract, definition, node).pins().toString();
            } catch (XamlLoadException failure) {
                return "error";
            }
        }
        return "";
    }

    /** Удаляет выбранные провода и ноды правками документа. */
    private void deleteSelection() {
        UiNode root = root();
        if (root == null) return;
        List<String> wires = new ArrayList<>(graph.selectedConnectionIds());
        List<String> nodes = new ArrayList<>(graph.selectedItemIds());
        for (String id : wires) {
            NodeGraphConnection connection = graph.connection(id);
            if (connection != null) commit(GraphEdits.disconnect(root, endpoint(connection.from()), endpoint(connection.to())));
        }
        for (String id : nodes) commit(GraphEdits.removeNode(root, id));
        rebuild();
    }

    /** Применяет правку без перестройки полотна: оно уже показывает результат жеста. */
    private boolean commit(Edit edit) {
        EditorSession session = session().orElse(null);
        if (session == null) return false;
        applying = true;
        boolean done;
        try {
            done = session.apply(edit);
        } finally {
            applying = false;
        }
        if (done) {
            rejection = "";
        } else {
            var found = session.diagnostics(EditorSession.Channel.EDIT);
            rejection = found.isEmpty() ? "The edit was rejected" : found.get(0).message();
        }
        problem.text(rejection);
        return done;
    }

    /** Подсказка для автоматизации: сколько проводов и нод сейчас на полотне. */
    Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("nodes", graph.items().size());
        counts.put("wires", graph.connections().size());
        return counts;
    }

    /** @return полотно; нужно тестам */
    NodeGraph view() {
        return graph;
    }

    /** @return текст проблемы под полотном */
    String problemText() {
        return problem.text();
    }
}
