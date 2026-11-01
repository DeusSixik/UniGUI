package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphConnection;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPort;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Неизменяемая проекция графа без виджетов: ноды с типами и значениями входов, порты и провода.
 *
 * <p>Модель снимается с {@link NodeGraph} в момент вызова {@link #from(NodeGraph)} и дальше не зависит от UI:
 * её можно исполнять в любом потоке, проверять и тестировать без рендера. Значения входов — те, что отдают
 * редакторы ({@code NodeGraphItem.value}); у входа без редактора значения нет.</p>
 */
@AiDoc(
        summary = "Immutable widget-free snapshot of a node graph (nodes, ports, typed input values, wires) used for validation and execution.",
        purpose = "Decouples running a graph from the editor widget so graphs can be executed and tested headless.",
        useWhen = {
                "Build a model with `graph.model()` and pass it to GraphExecutor or GraphValidator.",
                "Assemble a model by hand in tests with `GraphModel.of(nodes, wires)`."
        },
        criticalRules = {
                "The model is a snapshot: later edits of the graph do not change it.",
                "Value arrays such as double[] are shared with the snapshot and must not be mutated.",
                "Flow ports are the ports whose type is a flow type of the graph's PortTypeRegistry."
        },
        related = {"GraphExecutor", "GraphValidator", "NodeGraph"})
@WikiPage(title = "GraphModel", slug = "graph-model", category = "<key:graph.runtime.category>", summary = "<key:graph.model.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:graph.model.overview>", order = 10)
public class GraphModel {
    /**
     * Порт ноды в модели.
     */
    public record Port(String id, NodeGraphPortKind kind, String type, boolean flow, boolean required) {
        /**
         * @return {@code true}, если порт принимает соединения
         */
        public boolean input() {
            return kind == NodeGraphPortKind.INPUT || kind == NodeGraphPortKind.BIDIRECTIONAL;
        }

        /**
         * @return {@code true}, если из порта выходят соединения
         */
        public boolean output() {
            return kind == NodeGraphPortKind.OUTPUT || kind == NodeGraphPortKind.BIDIRECTIONAL;
        }
    }

    /**
     * Нода в модели: id, id типа, значения входов по id порта и порты.
     */
    public record Node(String id, String typeId, Map<String, Object> values, List<Port> ports) {
        public Node {
            values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
            ports = ports == null ? List.of() : List.copyOf(ports);
        }

        /**
         * @return порт или {@code null}
         */
        public Port port(String portId) {
            for (Port port : ports) {
                if (port.id().equals(portId)) return port;
            }
            return null;
        }

        /**
         * @return {@code true}, если у ноды есть входной порт потока выполнения
         */
        public boolean hasFlowInput() {
            for (Port port : ports) {
                if (port.flow() && port.input()) return true;
            }
            return false;
        }

        /**
         * @return {@code true}, если у ноды есть выходной порт потока выполнения
         */
        public boolean hasFlowOutput() {
            for (Port port : ports) {
                if (port.flow() && port.output()) return true;
            }
            return false;
        }
    }

    /**
     * Провод между выходом одной ноды и входом другой.
     */
    public record Wire(String id, String fromNode, String fromPort, String toNode, String toPort) {
    }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final List<Wire> wires;
    private final Map<String, Wire> firstIncoming = new HashMap<>();
    private final Map<String, List<Wire>> outgoing = new HashMap<>();

    protected GraphModel(Collection<Node> nodes, Collection<Wire> wires) {
        for (Node node : nodes) {
            this.nodes.put(node.id(), node);
        }
        this.wires = List.copyOf(wires);
        for (Wire wire : this.wires) {
            firstIncoming.putIfAbsent(key(wire.toNode(), wire.toPort()), wire);
            outgoing.computeIfAbsent(key(wire.fromNode(), wire.fromPort()), k -> new ArrayList<>()).add(wire);
        }
    }

    /**
     * Снимает модель с графа.
     *
     * @param graph граф
     * @return модель, не зависящая от графа
     */
    public static GraphModel from(NodeGraph graph) {
        List<Node> nodes = new ArrayList<>();
        for (NodeGraphItem item : graph.items()) {
            List<Port> ports = new ArrayList<>();
            Map<String, Object> values = new LinkedHashMap<>();
            for (NodeGraphPort port : item.ports()) {
                boolean flow = graph.portTypes().get(port.type()).flow();
                ports.add(new Port(port.id(), port.kind(), port.type(), flow, port.required()));
                Object value = item.value(port.id());
                if (value != null) values.put(port.id(), value);
            }
            nodes.add(new Node(item.id(), item.typeId(), values, ports));
        }
        List<Wire> wires = new ArrayList<>();
        for (NodeGraphConnection connection : graph.connections()) {
            if (!connection.enabled()) continue;
            wires.add(new Wire(connection.id(), connection.fromItemId(), connection.fromPortId(),
                    connection.toItemId(), connection.toPortId()));
        }
        return new GraphModel(nodes, wires);
    }

    /**
     * Собирает модель вручную, например для тестов.
     *
     * @param nodes ноды
     * @param wires провода
     * @return модель
     */
    public static GraphModel of(Collection<Node> nodes, Collection<Wire> wires) {
        return new GraphModel(nodes, wires);
    }

    /**
     * @return нода или {@code null}
     */
    public Node node(String id) {
        return nodes.get(id);
    }

    /**
     * @return ноды в порядке графа
     */
    public Collection<Node> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * @return провода (отключённые провода в модель не попадают)
     */
    public List<Wire> wires() {
        return wires;
    }

    /**
     * @param nodeId id ноды
     * @param portId id входного порта
     * @return первый провод в порт или {@code null}
     */
    public Wire incoming(String nodeId, String portId) {
        return firstIncoming.get(key(nodeId, portId));
    }

    /**
     * @param nodeId id ноды
     * @param portId id выходного порта
     * @return провода из порта в порядке добавления
     */
    public List<Wire> outgoing(String nodeId, String portId) {
        List<Wire> found = outgoing.get(key(nodeId, portId));
        return found == null ? List.of() : Collections.unmodifiableList(found);
    }

    private static String key(String nodeId, String portId) {
        return nodeId + '\u0000' + portId;
    }
}
