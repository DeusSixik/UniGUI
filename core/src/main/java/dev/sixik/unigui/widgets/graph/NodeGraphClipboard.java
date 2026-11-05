package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.List;

/**
 * Снимок выделенных нод и проводов между ними для вставки.
 *
 * <p>В снимок попадают только ноды, чей {@link NodeGraphItem#typeId()} зарегистрирован в
 * {@code NodeGraph.nodeTypes()}: именно по типу ноды создаются заново. Ноды с произвольным content-виджетом и
 * неизвестными типами пропускаются. Значения редакторов входов входят в снимок через {@link NodeGraphItem#saveState()}.</p>
 */
@AiDoc(
        summary = "Immutable copy of selected modular nodes and the wires between them, pasteable into the same or another graph.",
        purpose = "Backs copy, paste and duplicate for node graphs.",
        useWhen = {
                "Copy a selection with `NodeGraph.copySelection()` and paste it with `paste(...)`.",
                "Move nodes between two graphs that share a NodeTypeRegistry."
        },
        criticalRules = {
                "Only nodes whose type is registered are copied.",
                "Only wires with both ends inside the copied set are kept.",
                "Values of input editors are copied with the node."
        },
        related = {"NodeGraph", "NodeType"})
@WikiPage(title = "NodeGraphClipboard", slug = "node-graph-clipboard", category = "<key:graph.category>", summary = "<key:node.graph.clipboard.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.graph.clipboard.overview>", order = 10)
public class NodeGraphClipboard {
    /**
     * Пустой буфер.
     */
    public static final NodeGraphClipboard EMPTY = new NodeGraphClipboard(List.of(), List.of());

    /**
     * Нода в буфере: тип, исходный id, положение, ширина и состояние сворачивания.
     */
    record Node(String typeId, String id, float x, float y, float width, boolean collapsed,
                java.util.Map<String, String> state) {
    }

    /**
     * Провод в буфере между портами скопированных нод.
     */
    record Wire(String fromNode, String fromPort, String toNode, String toPort) {
    }

    private final List<Node> nodes;
    private final List<Wire> wires;
    private final float minX;
    private final float minY;

    NodeGraphClipboard(List<Node> nodes, List<Wire> wires) {
        this.nodes = List.copyOf(nodes);
        this.wires = List.copyOf(wires);
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        for (Node node : nodes) {
            left = Math.min(left, node.x());
            top = Math.min(top, node.y());
        }
        this.minX = Float.isFinite(left) ? left : 0.0f;
        this.minY = Float.isFinite(top) ? top : 0.0f;
    }

    /**
     * @return {@code true}, если вставлять нечего
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * @return число скопированных нод
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * @return число скопированных проводов
     */
    public int wireCount() {
        return wires.size();
    }

    /**
     * @return X левого края группы в world-координатах на момент копирования
     */
    public float minX() {
        return minX;
    }

    /**
     * @return Y верхнего края группы в world-координатах на момент копирования
     */
    public float minY() {
        return minY;
    }

    List<Node> nodes() {
        return nodes;
    }

    List<Wire> wires() {
        return wires;
    }
}
