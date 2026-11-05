package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.graph.node.HeaderModule;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortRowModule;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.node.PortTypeRegistry;
import dev.sixik.unigui.widgets.graph.node.WidgetModule;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeGraphModularTest {
    private static final float EPS = 1.0e-3f;

    private NodeGraph graph;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph();
        graph.portTypes()
                .register(PortType.data("float", 0xFF98E27A))
                .register(PortType.data("vec3", 0xFFF5D547))
                .register(PortType.data("string", 0xFFE38BD2))
                .allowConversion("float", "vec3");
    }

    private NodeGraphItem functionNode(String id, Widget inline) {
        return NodeBuilder.node(id)
                .color(0xFF4CAF50)
                .header("Create")
                .flowRow("exec-in", "exec-out")
                .input("pos", "Position", "vec3", inline)
                .input("name", "Name", "string")
                .output("result", "Result", "vec3")
                .build();
    }

    private void layout() {
        graph.measure(new LayoutContext(800.0f, 600.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 800.0f, 600.0f));
    }

    @Test
    void portTypes_flowNeverMixesWithData() {
        PortTypeRegistry types = graph.portTypes();

        assertTrue(types.canConnect("flow", "flow"));
        assertFalse(types.canConnect("flow", "float"));
        assertFalse(types.canConnect("vec3", "flow"));
    }

    @Test
    void portTypes_conversionsAreDirectedAndUnknownTypesMismatch() {
        PortTypeRegistry types = graph.portTypes();

        assertTrue(types.canConnect("float", "float"));
        assertTrue(types.canConnect("float", "vec3"));
        assertFalse(types.canConnect("vec3", "float"));
        assertFalse(types.canConnect("item", "fluid"));
        assertTrue(types.canConnect("", "float"));
        assertTrue(types.canConnect("any", "vec3"));
    }

    @Test
    void builder_assemblesModulesAndPorts() {
        NodeGraphItem node = functionNode("spawn", null);

        assertTrue(node.modular());
        assertNull(node.content());
        assertEquals(5, node.modules().size());
        assertNotNull(node.module(HeaderModule.class));
        assertEquals(NodeGraphPortKind.INPUT, node.port("pos").kind());
        assertEquals(NodeGraphPortSide.LEFT, node.port("pos").side());
        assertEquals(NodeGraphPortKind.OUTPUT, node.port("result").kind());
        assertEquals(NodeGraphPortSide.RIGHT, node.port("result").side());
        assertEquals("flow", node.port("exec-in").type());
        assertEquals(1, node.port("pos").capacity());
        assertEquals(1, node.port("exec-out").capacity());
        assertEquals(0, node.port("result").capacity());
        assertFalse(node.resizable());
    }

    @Test
    void layout_stacksModulesTopToBottomAndAnchorsPortsToRowEdges() {
        NodeGraphItem node = functionNode("spawn", null);
        graph.addItem(node);

        layout();

        assertTrue(node.arrangedWidth() >= node.style().minWidth());
        assertTrue(node.arrangedHeight() > node.style().headerHeight());
        NodeGraphPort pos = node.port("pos");
        NodeGraphPort result = node.port("result");
        NodeGraphPort execOut = node.port("exec-out");
        assertTrue(pos.anchored());
        assertEquals(0.0f, pos.anchorX(), EPS);
        assertEquals(node.arrangedWidth(), result.anchorX(), EPS);
        assertEquals(node.arrangedWidth(), execOut.anchorX(), EPS);
        assertTrue(execOut.anchorY() > node.style().headerHeight(),
                "exec pins sit below the header, not inside it");
        assertTrue(execOut.anchorY() < pos.anchorY(), "exec row is between the header and the fields");
        assertEquals(node.port("exec-in").anchorY(), execOut.anchorY(), EPS);
        assertTrue(pos.anchorY() > node.style().headerHeight());
        assertTrue(node.port("name").anchorY() > pos.anchorY());
        assertTrue(result.anchorY() > node.port("name").anchorY());
    }

    @Test
    void collapse_hidesBodyAndPullsWiresToHeaderEdges() {
        NodeGraphItem node = functionNode("spawn", null);
        graph.addItem(node);
        layout();
        float expandedHeight = node.arrangedHeight();
        List<String> events = new ArrayList<>();
        graph.onItemCollapsed(event -> events.add(event.itemId() + ":" + event.collapsed()));

        node.collapsed(true);
        layout();

        assertTrue(node.arrangedHeight() < expandedHeight);
        assertEquals(node.style().headerHeight(), node.arrangedHeight(), EPS);
        assertFalse(node.portShown(node.port("pos")));
        assertFalse(node.portShown(node.port("exec-out")), "the flow row is hidden together with the body");
        assertEquals(node.style().headerHeight() * 0.5f, node.port("exec-out").anchorY(), EPS);
        assertEquals(node.arrangedWidth(), node.port("exec-out").anchorX(), EPS);
        assertEquals(node.style().headerHeight() * 0.5f, node.port("pos").anchorY(), EPS);
        assertEquals(0.0f, node.port("pos").anchorX(), EPS);
        assertEquals(node.arrangedWidth(), node.port("result").anchorX(), EPS);
        assertEquals(List.of("spawn:true"), events);

        node.toggleCollapsed();
        layout();

        assertEquals(expandedHeight, node.arrangedHeight(), EPS);
        assertTrue(node.portShown(node.port("pos")));
    }

    @Test
    void collapsedNode_keepsConnectionsAlive() {
        NodeGraphItem source = functionNode("a", null);
        NodeGraphItem sink = NodeBuilder.node("b").header("Sink").input("in", "In", "vec3").build();
        graph.addItem(source).addItem(sink);
        graph.addConnection("wire", "a", "result", "b", "in");

        source.collapsed(true);
        layout();

        assertEquals(1, graph.connections().size());
        assertEquals(1, source.port("result").connectionCount());
        assertTrue(source.port("result").anchored());
    }

    @Test
    void inlineEditor_hiddenWhileInputConnected() {
        Label editor = new Label("0 0 0");
        NodeGraphItem source = NodeBuilder.node("src").header("Src").output("out", "Out", "vec3").build();
        NodeGraphItem target = functionNode("target", editor);
        graph.addItem(source).addItem(target);

        assertTrue(target.hostedWidgets().contains(editor));

        graph.addConnection("wire", "src", "out", "target", "pos");

        assertFalse(target.hostedWidgets().contains(editor));
        assertTrue(target.allWidgets().contains(editor));
        assertTrue(target.port("pos").connected());

        graph.removeConnection("wire");

        assertTrue(target.hostedWidgets().contains(editor));
        assertFalse(target.port("pos").connected());
    }

    @Test
    void dataInput_capacityOne_replacesOldestConnection() {
        NodeGraphItem a = NodeBuilder.node("a").header("A").output("out", "Out", "vec3").build();
        NodeGraphItem b = NodeBuilder.node("b").header("B").output("out", "Out", "vec3").build();
        NodeGraphItem sink = NodeBuilder.node("sink").header("Sink").input("in", "In", "vec3").build();
        graph.addItem(a).addItem(b).addItem(sink);

        graph.addConnection("first", "a", "out", "sink", "in");
        graph.addConnection("second", "b", "out", "sink", "in");

        assertNull(graph.connection("first"));
        assertNotNull(graph.connection("second"));
        assertEquals(1, sink.port("in").connectionCount());
        assertEquals(0, a.port("out").connectionCount());
    }

    @Test
    void dataOutput_unlimitedFanOut() {
        NodeGraphItem source = NodeBuilder.node("src").header("Src").output("out", "Out", "float").build();
        NodeGraphItem x = NodeBuilder.node("x").header("X").input("in", "In", "float").build();
        NodeGraphItem y = NodeBuilder.node("y").header("Y").input("in", "In", "float").build();
        graph.addItem(source).addItem(x).addItem(y);

        graph.addConnection("c1", "src", "out", "x", "in");
        graph.addConnection("c2", "src", "out", "y", "in");

        assertEquals(2, source.port("out").connectionCount());
    }

    @Test
    void connectionPolicy_enforcesTypeSystem() {
        NodeGraphItem source = NodeBuilder.node("src")
                .header("Src").output("f", "F", "float").output("v", "V", "vec3").build();
        NodeGraphItem sink = NodeBuilder.node("sink")
                .header("Sink").input("vec", "Vec", "vec3").input("flt", "Flt", "float").build();
        graph.addItem(source).addItem(sink);

        assertTrue(graph.validateConnection(new NodeGraphPortRef("src", "f"), new NodeGraphPortRef("sink", "vec")).valid());
        assertFalse(graph.validateConnection(new NodeGraphPortRef("src", "v"), new NodeGraphPortRef("sink", "flt")).valid());
        assertThrows(IllegalArgumentException.class,
                () -> graph.addConnection("bad", "src", "v", "sink", "flt"));
    }

    @Test
    void flowPorts_connectOnlyToFlowPorts() {
        NodeGraphItem event = NodeBuilder.node("event").header("Event").flowRow(null, "then").build();
        NodeGraphItem action = functionNode("action", null);
        graph.addItem(event).addItem(action);

        assertTrue(graph.validateConnection(new NodeGraphPortRef("event", "then"),
                new NodeGraphPortRef("action", "exec-in")).valid());
        assertFalse(graph.validateConnection(new NodeGraphPortRef("event", "then"),
                new NodeGraphPortRef("action", "pos")).valid());
    }

    @Test
    void removingNode_clearsConnectionCountsOnTheOtherEnd() {
        NodeGraphItem source = NodeBuilder.node("src").header("Src").output("out", "Out", "vec3").build();
        NodeGraphItem sink = NodeBuilder.node("sink").header("Sink").input("in", "In", "vec3").build();
        graph.addItem(source).addItem(sink);
        graph.addConnection("wire", "src", "out", "sink", "in");

        graph.removeItem("sink");

        assertEquals(0, source.port("out").connectionCount());
    }

    @Test
    void customModule_participatesInLayoutAndPorts() {
        NodeGraphItem node = NodeBuilder.node("custom")
                .header("Custom")
                .module(PortRowModule.row("a", "A", "float", "b", "B", "float"))
                .build();
        graph.addItem(node);

        layout();

        assertNotNull(node.module("row:a"));
        assertEquals(0.0f, node.port("a").anchorX(), EPS);
        assertEquals(node.arrangedWidth(), node.port("b").anchorX(), EPS);
        assertEquals(node.port("a").anchorY(), node.port("b").anchorY(), EPS);
    }

    @Test
    void removeModule_dropsItsPorts() {
        NodeGraphItem node = functionNode("spawn", null);

        node.removeModule("row:name");

        assertNull(node.port("name"));
        assertEquals(4, node.modules().size());
    }

    @Test
    void duplicateModuleId_isRejected() {
        NodeGraphItem node = functionNode("spawn", null);

        assertThrows(IllegalArgumentException.class, () -> node.addModule(PortRowModule.input("pos", "Again", "vec3")));
    }

    @Test
    void contentWidgetNode_rejectsModules() {
        NodeGraphItem legacy = new NodeGraphItem("legacy", new Label("x"), 0.0f, 0.0f);

        assertFalse(legacy.modular());
        assertThrows(IllegalStateException.class, () -> legacy.addModule(PortRowModule.input("a", "A", "float")));
    }

    @Test
    void nodeTypes_spawnWithUniqueIdsAndStampTypeId() {
        graph.nodeTypes().register(NodeType.of("math.add", "Add", "Math",
                id -> NodeBuilder.node(id).header("Add").input("a", "A", "float").output("r", "R", "float").build()));

        NodeGraphItem first = graph.spawn("math.add", 10.0f, 20.0f);
        NodeGraphItem second = graph.spawn("math.add", 30.0f, 40.0f);

        assertNotNull(first);
        assertEquals("math.add", first.typeId());
        assertEquals(10.0f, first.x(), EPS);
        assertFalse(first.id().equals(second.id()));
        assertNull(graph.spawn("missing", 0.0f, 0.0f));
        assertEquals(2, graph.items().size());
    }

    @Test
    void nodeTypeRegistry_searchRanksPrefixFirst() {
        graph.nodeTypes()
                .register(NodeType.of("a", "Print String", "Utility", id -> NodeBuilder.node(id).build()))
                .register(NodeType.of("b", "Sprint", "Movement", id -> NodeBuilder.node(id).build()))
                .register(NodeType.of("c", "Add", "Math", id -> NodeBuilder.node(id).build()));

        List<NodeType> found = graph.nodeTypes().search("pri");

        assertEquals(List.of("a", "b"), found.stream().map(NodeType::id).toList());
        assertEquals(1, graph.nodeTypes().byCategory("Math").size());
    }

    @Test
    void regions_addRemoveAndDuplicateIds() {
        List<String> kinds = new ArrayList<>();
        graph.onRegionChanged(event -> kinds.add(event.kind().name()));

        NodeGraphRegion region = graph.addRegion("r1", "Setup", 0.0f, 0.0f, 300.0f, 200.0f);
        region.comment("Initialises the NPC");

        assertEquals("Setup", region.title());
        assertEquals("Initialises the NPC", region.comment());
        assertSameRegion(region, graph.region("r1"));
        assertThrows(IllegalArgumentException.class, () -> graph.addRegion("r1", "Dup", 0.0f, 0.0f, 100.0f, 100.0f));

        graph.removeRegion("r1");

        assertNull(graph.region("r1"));
        assertEquals(List.of("ADDED", "REMOVED"), kinds);
    }

    private static void assertSameRegion(NodeGraphRegion expected, NodeGraphRegion actual) {
        assertTrue(expected == actual);
    }

    @Test
    void regions_enforceMinimumSizeAndAutoIds() {
        NodeGraphRegion region = graph.addRegion("", "Tiny", 0.0f, 0.0f, 1.0f, 1.0f);

        assertEquals(NodeGraphRegion.MIN_WIDTH, region.width(), EPS);
        assertEquals(NodeGraphRegion.MIN_HEIGHT, region.height(), EPS);
        assertFalse(region.id().isEmpty());
    }

    @Test
    void region_carriesOnlyNodesFullyInsideIt() {
        NodeGraphItem inside = functionNode("inside", null);
        inside.position(20.0f, 40.0f);
        NodeGraphItem outside = functionNode("outside", null);
        outside.position(900.0f, 40.0f);
        NodeGraphItem straddling = functionNode("straddling", null);
        straddling.position(250.0f, 40.0f);
        graph.addItem(inside).addItem(outside).addItem(straddling);
        layout();
        NodeGraphRegion region = graph.addRegion("r", "Group", 0.0f, 0.0f, 300.0f, 400.0f);

        List<NodeGraphItem> carried = region.containedItems(graph);

        assertEquals(List.of(inside), carried);
        assertTrue(region.containsItem(inside));
        assertFalse(region.containsItem(straddling));
    }

    @Test
    void snapshot_roundTripsModularNodesCollapseAndRegions() {
        graph.nodeTypes().register(NodeType.of("fn.create", "Create", "Function",
                id -> functionNode(id, null)));
        NodeGraphItem created = graph.spawn("fn.create", "n1", 50.0f, 60.0f);
        NodeGraphItem sink = graph.spawn("fn.create", "n2", 400.0f, 60.0f);
        graph.addConnection("w", "n1", "result", "n2", "pos");
        created.collapsed(true);
        graph.addRegion("r", "Spawn setup", 10.0f, 10.0f, 500.0f, 300.0f).comment("hello").color(0xFF336699);

        NodeGraphSnapshot snapshot = graph.snapshot();
        NodeGraph restored = new NodeGraph();
        restored.nodeTypes().register(graph.nodeTypes().get("fn.create"));
        restored.portTypes().register(PortType.data("vec3", 0xFFF5D547)).register(PortType.data("string", 0xFFE38BD2));
        restored.restoreSnapshot(snapshot, null);

        NodeGraphItem n1 = restored.item("n1");
        assertNotNull(n1);
        assertTrue(n1.modular());
        assertTrue(n1.collapsed());
        assertEquals("fn.create", n1.typeId());
        assertEquals(50.0f, n1.x(), EPS);
        assertNotNull(restored.item("n2"));
        assertEquals(1, restored.connections().size());
        NodeGraphRegion region = restored.region("r");
        assertNotNull(region);
        assertEquals("Spawn setup", region.title());
        assertEquals("hello", region.comment());
        assertEquals(500.0f, region.width(), EPS);
        assertEquals(0.2f, region.color().r(), 0.01f);
        assertEquals(sink.id(), restored.item("n2").id());
    }

    @Test
    void render_fullBlueprintScene_emitsChromeWiresRegionsAndMinimap() {
        graph.minimapEnabled(true);
        graph.addRegion("r", "Group", 10.0f, 10.0f, 400.0f, 300.0f).comment("note");
        NodeGraphItem source = NodeBuilder.node("src").header("Src").flowRow(null, "then")
                .output("out", "Out", "vec3").build();
        NodeGraphItem sink = functionNode("sink", new Label("0 0 0"));
        source.position(20.0f, 60.0f);
        sink.position(260.0f, 60.0f);
        graph.addItem(source).addItem(sink);
        graph.addConnection("data", "src", "out", "sink", "pos");
        graph.addConnection("flow", "src", "then", "sink", "exec-in");
        layout();
        DrawList list = new DrawList();

        graph.render(new DefaultRenderContext(list));

        int roundedRects = 0;
        int paths = 0;
        for (DrawCommand command : list.commands()) {
            if (command.type() == DrawCommandType.ROUNDED_RECT) roundedRects++;
            if (command.type() == DrawCommandType.PATH) paths++;
        }
        assertTrue(roundedRects >= 4, "body + header + border per node plus minimap frame, got " + roundedRects);
        assertTrue(paths >= 2, "two wires drawn as curves, got " + paths);
    }

    @Test
    void render_collapsedNode_drawsLessThanExpanded() {
        NodeGraphItem node = functionNode("spawn", new Label("0 0 0"));
        graph.addItem(node);
        layout();
        DrawList expanded = new DrawList();
        graph.render(new DefaultRenderContext(expanded));

        node.collapsed(true);
        layout();
        DrawList collapsed = new DrawList();
        graph.render(new DefaultRenderContext(collapsed));

        assertTrue(collapsed.size() < expanded.size());
    }

    @Test
    void headerArrow_pressTogglesCollapse() {
        NodeGraphItem node = functionNode("spawn", null);
        graph.addItem(node);
        layout();
        float arrowX = node.arrangedWidth() - node.style().padding() - 4.0f;
        float headerY = node.style().headerHeight() * 0.5f;

        assertFalse(node.pressModule(10.0f, headerY));
        assertFalse(node.collapsed());
        assertTrue(node.pressModule(arrowX, headerY));
        assertTrue(node.collapsed());
        assertFalse(node.pressModule(arrowX, node.style().headerHeight() + 30.0f));
    }

    @Test
    void inlineTextField_typingNeverChangesNodeSize() {
        TextField field = new TextField("5");
        NodeGraphItem node = NodeBuilder.node("cmp").header("Less or equal")
                .input("a", "A", "number")
                .input("b", "B", "number", field)
                .output("result", "Result", "bool")
                .build();
        graph.addItem(node);
        layout();
        float width = node.arrangedWidth();
        float height = node.arrangedHeight();

        for (int length : new int[]{1, 10, 80, 600}) {
            field.text("9".repeat(length));
            layout();
            assertEquals(width, node.arrangedWidth(), EPS, "width after typing " + length + " chars");
            assertEquals(height, node.arrangedHeight(), EPS, "height after typing " + length + " chars");
        }
    }

    @Test
    void inlineTextField_staysInsideItsNodeWhateverTheText() {
        TextField field = new TextField("");
        NodeGraphItem node = NodeBuilder.node("cmp").header("Less or equal")
                .input("b", "B", "number", field)
                .output("result", "Result", "bool")
                .build();
        graph.addItem(node);

        field.text("9".repeat(600));
        layout();

        float nodeRight = node.x() + node.arrangedWidth();
        assertTrue(field.layoutBounds().x() + field.layoutBounds().width() <= nodeRight + EPS,
                "editor right edge " + (field.layoutBounds().x() + field.layoutBounds().width()) + " vs node " + nodeRight);
    }

    @Test
    void widgetModule_textEditorsKeepFixedSize() {
        TextField field = new TextField("");
        TextArea area = new TextArea();
        NodeGraphItem node = NodeBuilder.node("editors").header("Editors")
                .widget("field", field)
                .widget("area", area)
                .build();
        graph.addItem(node);
        layout();
        float width = node.arrangedWidth();
        float height = node.arrangedHeight();

        field.text("w".repeat(500));
        area.text("line\n".repeat(60));
        layout();

        assertEquals(width, node.arrangedWidth(), EPS);
        assertEquals(height, node.arrangedHeight(), EPS);
    }

    @Test
    void widgetModule_explicitSizeOverridesPreferred() {
        Label label = new Label("a long preview caption that would normally widen the node");
        NodeGraphItem node = NodeBuilder.node("preview").header("Preview")
                .module(new WidgetModule("caption", label).size(100.0f, 40.0f))
                .build();
        graph.addItem(node);

        layout();

        assertEquals(node.style().minWidth(), node.arrangedWidth(), EPS);
        assertEquals(node.style().headerHeight() + 40.0f + 4.0f + 4.0f, node.arrangedHeight(), EPS);
    }

    @Test
    void minimap_mapsWorldAndScreenBothWays() {
        NodeGraphMinimap map = new NodeGraphMinimap(100.0f, 200.0f, 160.0f, 100.0f,
                0.0f, 0.0f, 1000.0f, 500.0f);

        float screenX = map.toScreenX(400.0f);
        float screenY = map.toScreenY(250.0f);

        assertTrue(map.contains(screenX, screenY));
        assertEquals(400.0f, map.toWorldX(screenX), EPS);
        assertEquals(250.0f, map.toWorldY(screenY), EPS);
        assertTrue(map.toScreenX(0.0f) >= 100.0f);
        assertTrue(map.toScreenX(1000.0f) <= 260.0f);
        assertTrue(map.toScreenY(500.0f) <= 300.0f);
    }

    private static float maxTurnDegrees(float[] points, int count) {
        double worst = 0.0;
        for (int i = 2; i < count; i++) {
            double ax = points[(i - 1) * 2] - points[(i - 2) * 2];
            double ay = points[(i - 1) * 2 + 1] - points[(i - 2) * 2 + 1];
            double bx = points[i * 2] - points[(i - 1) * 2];
            double by = points[i * 2 + 1] - points[(i - 1) * 2 + 1];
            double la = Math.hypot(ax, ay);
            double lb = Math.hypot(bx, by);
            if (la < 0.7 || lb < 0.7) continue; // субпиксельные отрезки не видны и угла не имеют
            double cos = Math.max(-1.0, Math.min(1.0, (ax * bx + ay * by) / (la * lb)));
            worst = Math.max(worst, Math.toDegrees(Math.acos(cos)));
        }
        return (float) worst;
    }

    @Test
    void wires_leaveAndEnterHorizontallyAndPassThroughMidpoint() {
        float[] points = new float[NodeGraphWires.BUFFER_SIZE];

        int count = NodeGraphWires.build(0.0f, 0.0f, NodeGraphPortSide.RIGHT,
                200.0f, 100.0f, NodeGraphPortSide.LEFT, 1.0f, points);

        assertEquals(points[1], points[3], EPS, "first segment leaves the port horizontally");
        assertTrue(points[2] > points[0]);
        assertEquals(points[(count - 1) * 2 + 1], points[(count - 2) * 2 + 1], EPS, "last segment enters horizontally");
        assertTrue(points[(count - 2) * 2] < points[(count - 1) * 2]);
        assertEquals(0.0f, NodeGraphWires.distance(100.0f, 50.0f, points, count), 0.5f);
        assertTrue(NodeGraphWires.distance(100.0f, 150.0f, points, count) > 40.0f);
    }

    @Test
    void wires_closeNodes_stayInsideTheGapAndStaySmooth() {
        float[] points = new float[NodeGraphWires.BUFFER_SIZE];
        float stub = 14.0f;

        for (float gap : new float[]{0.0f, 6.0f, 20.0f, 40.0f, 60.0f}) {
            for (float dy : new float[]{0.0f, 8.0f, 44.0f}) {
                int count = NodeGraphWires.build(100.0f, 50.0f, NodeGraphPortSide.RIGHT,
                        100.0f + gap, 50.0f + dy, NodeGraphPortSide.LEFT, 1.0f, points);

                // Зазор шире двух хвостиков: цель впереди, провод строго внутри зазора, без выбросов.
                // Уже зазор — разворот: дуга выходит за кончик хвостика не больше чем на половину своего диаметра (допуск 16 px).
                boolean forward = gap >= stub * 2.0f;
                float tolerance = forward ? 0.01f : 16.0f;
                for (int i = 0; i < count; i++) {
                    float x = points[i * 2];
                    float low = forward ? 100.0f : 100.0f - stub;
                    float high = forward ? 100.0f + gap : 100.0f + gap + stub;
                    assertTrue(x >= low - tolerance && x <= high + tolerance,
                            "gap " + gap + " dy " + dy + ": x " + x + " leaves [" + low + ", " + high + "]");
                }
                assertTrue(count >= 12, "gap " + gap + ": at least a dozen points, got " + count);
                assertTrue(maxTurnDegrees(points, count) < 45.0f,
                        "gap " + gap + " dy " + dy + ": sharpest turn " + maxTurnDegrees(points, count));
            }
        }
    }

    @Test
    void wires_targetBehindSource_makesASmoothUTurn() {
        float[] points = new float[NodeGraphWires.BUFFER_SIZE];

        for (float behind : new float[]{4.0f, 60.0f, 300.0f}) {
            int count = NodeGraphWires.build(300.0f, 50.0f, NodeGraphPortSide.RIGHT,
                    300.0f - behind, 130.0f, NodeGraphPortSide.LEFT, 1.0f, points);

            assertTrue(maxTurnDegrees(points, count) < 30.0f,
                    "behind " + behind + ": sharpest turn " + maxTurnDegrees(points, count));
            assertEquals(300.0f - behind, points[(count - 1) * 2], EPS);
        }
    }

    @Test
    void wires_farNodes_stayWithinTheBuffer() {
        float[] points = new float[NodeGraphWires.BUFFER_SIZE];

        int count = NodeGraphWires.build(0.0f, 0.0f, NodeGraphPortSide.RIGHT,
                5000.0f, 3000.0f, NodeGraphPortSide.LEFT, 1.0f, points);

        assertTrue(count <= NodeGraphWires.MAX_POINTS);
        assertEquals(5000.0f, points[(count - 1) * 2], EPS);
        assertEquals(3000.0f, points[(count - 1) * 2 + 1], EPS);
    }

    @Test
    void wires_hitTestFollowsTheDrawnPolyline() {
        float[] points = new float[NodeGraphWires.BUFFER_SIZE];
        int count = NodeGraphWires.build(0.0f, 0.0f, NodeGraphPortSide.RIGHT,
                120.0f, 0.0f, NodeGraphPortSide.LEFT, 1.0f, points);

        for (int i = 0; i < count; i++) {
            assertEquals(0.0f, NodeGraphWires.distance(points[i * 2], points[i * 2 + 1], points, count), EPS);
        }
        assertTrue(NodeGraphWires.distance(60.0f, 30.0f, points, count) > 25.0f);
    }
}
