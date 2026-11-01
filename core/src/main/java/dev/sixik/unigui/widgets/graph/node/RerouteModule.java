package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPort;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.NodeGraphPortSide;

/**
 * Узел-точка на проводе: вход слева, выход справа, без заголовка и подписей.
 *
 * <p>Через reroute провод огибает ноды и расходится веером без лишних кривых. Тип проводов после reroute
 * берётся от подключённого источника (граф раскрашивает их по {@code effectiveType}); сам узел принимает любой
 * data-тип либо поток выполнения, смотря какой вариант создан: {@link #dataType()} или {@link #flowType()}.</p>
 */
@AiDoc(
        summary = "Compact pass-through node used to bend and fan out wires; has an input on the left and an output on the right.",
        purpose = "Lets users route wires around nodes and split one output without crossing node bodies.",
        useWhen = {
                "Insert a reroute into a wire with `NodeGraph.insertReroute`.",
                "Create one directly with `RerouteModule.createNode`."
        },
        criticalRules = {
                "A data reroute accepts any data type, an exec reroute only flow; the two never mix.",
                "Reroute input accepts one wire, data output is unlimited, exec output accepts one."
        },
        related = {"NodeModule", "NodeGraph", "NodeType"})
@WikiPage(title = "RerouteModule", slug = "reroute-module", category = "<key:graph.category>", summary = "<key:reroute.module.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:reroute.module.overview>", order = 10)
public class RerouteModule implements NodeModule {
    /**
     * Id модуля и порт-id входа/выхода.
     */
    public static final String ID = "reroute";
    public static final String INPUT = "in";
    public static final String OUTPUT = "out";
    /**
     * Id встроенного типа data-reroute.
     */
    public static final String DATA_TYPE_ID = "reroute";
    /**
     * Id встроенного типа exec-reroute.
     */
    public static final String FLOW_TYPE_ID = "reroute.flow";

    private static final float WIDTH = 36.0f;
    private static final float HEIGHT = 20.0f;

    private final String portType;
    private NodeGraphPort input;
    private NodeGraphPort output;
    private NodeGraphItem item;

    /**
     * @param portType id типа обоих портов: {@code "any"} для данных или {@code "flow"} для потока
     */
    public RerouteModule(String portType) {
        this.portType = portType == null || portType.isEmpty() ? PortTypeRegistry.ANY.id() : portType;
    }

    /**
     * Собирает готовую reroute-ноду.
     *
     * @param id   id ноды
     * @param flow {@code true} для exec-reroute
     * @return нода с одним модулем и компактным стилем
     */
    public static NodeGraphItem createNode(String id, boolean flow) {
        NodeGraphItem node = new NodeGraphItem(id);
        node.style(new NodeStyle().minWidth(WIDTH).cornerRadius(HEIGHT * 0.5f).padding(0.0f).bodyColor(0xFF303034));
        node.addModule(new RerouteModule(flow ? PortTypeRegistry.FLOW.id() : PortTypeRegistry.ANY.id()));
        node.typeId(flow ? FLOW_TYPE_ID : DATA_TYPE_ID);
        return node;
    }

    /**
     * @return встроенный тип data-reroute для каталога
     */
    public static NodeType dataType() {
        return NodeType.of(DATA_TYPE_ID, "Reroute", "Utility", id -> createNode(id, false));
    }

    /**
     * @return встроенный тип exec-reroute для каталога
     */
    public static NodeType flowType() {
        return NodeType.of(FLOW_TYPE_ID, "Reroute (exec)", "Utility", id -> createNode(id, true));
    }

    /**
     * @return {@code true}, если этот reroute пропускает поток выполнения
     */
    public boolean flow() {
        return PortTypeRegistry.FLOW.id().equals(portType);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void attach(NodeGraphItem item) {
        this.item = item;
        boolean flow = flow();
        input = new NodeGraphPort(INPUT, NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.5f)
                .type(portType).capacity(flow ? 0 : 1);
        output = new NodeGraphPort(OUTPUT, NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.5f)
                .type(portType).capacity(flow ? 1 : 0);
        item.addPort(input, this);
        item.addPort(output, this);
    }

    @Override
    public void detach(NodeGraphItem item) {
        item.removePort(INPUT);
        item.removePort(OUTPUT);
        input = null;
        output = null;
        this.item = null;
    }

    @Override
    public float measureWidth(NodeLayoutContext context) {
        return WIDTH;
    }

    @Override
    public float measureHeight(NodeLayoutContext context, float width) {
        return HEIGHT;
    }

    @Override
    public void arrange(NodeLayoutContext context, float x, float y, float width, float height) {
        if (input != null) context.anchor(input, 0.0f, y + height * 0.5f);
        if (output != null) context.anchor(output, width, y + height * 0.5f);
    }
}
