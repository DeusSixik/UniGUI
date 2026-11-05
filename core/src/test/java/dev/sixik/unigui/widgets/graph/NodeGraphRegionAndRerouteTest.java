package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.node.RerouteModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Типы через reroute, вложенные регионы и комментарий региона при наведении.
 */
class NodeGraphRegionAndRerouteTest {
    private static final float EPS = 1.0e-3f;

    private OverlayLayer layer;
    private NodeGraph graph;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph().selectionMode(NodeGraphSelectionMode.MULTIPLE);
        graph.portTypes().register(PortType.data("string", 0xFFFF55AA));
        graph.portTypes().register(PortType.data("int", 0xFF55FFAA));
        graph.portTypes().register(PortType.data("float", 0xFF55AAFF));
        graph.portTypes().allowConversion("int", "float");
        graph.nodeTypes()
                .register(NodeType.of("string.out", "String", "T", id -> NodeBuilder.node(id)
                        .header("String").output("out", "Out", "string").build()))
                .register(NodeType.of("int.out", "Int", "T", id -> NodeBuilder.node(id)
                        .header("Int").output("out", "Out", "int").build()))
                .register(NodeType.of("string.in", "StringIn", "T", id -> NodeBuilder.node(id)
                        .header("StringIn").input("in", "In", "string").build()))
                .register(NodeType.of("int.in", "IntIn", "T", id -> NodeBuilder.node(id)
                        .header("IntIn").input("in", "In", "int").build()))
                .register(NodeType.of("float.in", "FloatIn", "T", id -> NodeBuilder.node(id)
                        .header("FloatIn").input("in", "In", "float").build()))
                .register(NodeType.of("exec", "Exec", "T", id -> NodeBuilder.node(id)
                        .header("Exec").flowRow("exec-in", "then").build()));
        layer = new OverlayLayer();
        layer.content(graph);
        layer.applyQueuedMutations();
        graph.setUiContextInternal(new DefaultUIContext());
    }

    private void layout() {
        layer.measure(new LayoutContext(1000.0f, 800.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 1000.0f, 800.0f));
    }

    private NodeGraphItem reroute(String id) {
        NodeGraphItem node = RerouteModule.createNode(id, false);
        graph.addItem(node);
        return node;
    }

    private static NodeGraphPortRef out(String item) {
        return new NodeGraphPortRef(item, "out");
    }

    private static NodeGraphPortRef in(String item) {
        return new NodeGraphPortRef(item, "in");
    }

    private static NodeGraphPortRef rerouteIn(String item) {
        return new NodeGraphPortRef(item, RerouteModule.INPUT);
    }

    private static NodeGraphPortRef rerouteOut(String item) {
        return new NodeGraphPortRef(item, RerouteModule.OUTPUT);
    }

    // ---- reroute: типы ----

    @Test
    void reroute_doesNotLetAStringReachAnInt() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("int.in", "k", 600, 0);
        reroute("r");
        graph.addConnection("", out("s"), rerouteIn("r"));

        assertThrows(IllegalArgumentException.class, () -> graph.addConnection("", rerouteOut("r"), in("k")));
        assertFalse(graph.validateConnection(rerouteOut("r"), in("k")).valid());
        assertEquals("Port type mismatch", graph.validateConnection(rerouteOut("r"), in("k")).reason());
        assertEquals(1, graph.connections().size());
    }

    @Test
    void reroute_passesTheSameTypeAndFansOut() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("string.in", "k1", 600, 0);
        graph.spawn("string.in", "k2", 600, 100);
        reroute("r");

        graph.addConnection("", out("s"), rerouteIn("r"));
        graph.addConnection("", rerouteOut("r"), in("k1"));
        graph.addConnection("", rerouteOut("r"), in("k2"));

        assertEquals(3, graph.connections().size());
    }

    @Test
    void reroute_rejectsASecondSinkOfAnotherType() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("string.in", "k1", 600, 0);
        graph.spawn("int.in", "k2", 600, 100);
        reroute("r");
        graph.addConnection("", out("s"), rerouteIn("r"));
        graph.addConnection("", rerouteOut("r"), in("k1"));

        assertFalse(graph.validateConnection(rerouteOut("r"), in("k2")).valid());
    }

    @Test
    void reroute_withASinkRejectsASourceOfAnotherType() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("int.in", "k", 600, 0);
        reroute("r");
        graph.addConnection("", rerouteOut("r"), in("k"));

        assertFalse(graph.validateConnection(out("s"), rerouteIn("r")).valid(),
                "the string must be refused at the reroute input, the int sink is already behind it");
        graph.spawn("int.out", "i", 0, 100);
        assertTrue(graph.validateConnection(out("i"), rerouteIn("r")).valid());
    }

    @Test
    void reroute_floatingChainRefusesSinksThatNoSingleSourceCouldServe() {
        graph.spawn("string.in", "k1", 600, 0);
        graph.spawn("int.in", "k2", 600, 100);
        graph.spawn("float.in", "k3", 600, 200);
        reroute("r");
        graph.addConnection("", rerouteOut("r"), in("k1"));

        assertFalse(graph.validateConnection(rerouteOut("r"), in("k2")).valid(), "string and int share no source");

        graph.removeConnection(graph.connections().get(0));
        graph.addConnection("", rerouteOut("r"), in("k2"));
        assertTrue(graph.validateConnection(rerouteOut("r"), in("k3")).valid(), "an int source also fits a float sink");
    }

    @Test
    void reroute_followsTheAllowedConversionsOfTheTypeSystem() {
        graph.spawn("int.out", "i", 0, 0);
        graph.spawn("float.in", "f", 600, 0);
        graph.spawn("string.out", "s", 0, 100);
        reroute("r");
        graph.addConnection("", out("i"), rerouteIn("r"));

        assertTrue(graph.validateConnection(rerouteOut("r"), in("f")).valid(), "int -> float is allowed through the reroute");
        assertFalse(graph.validateConnection(out("s"), in("f")).valid(), "a string never fits a float");
    }

    @Test
    void reroute_floatToIntStaysForbiddenThroughTheChain() {
        graph.nodeTypes().register(NodeType.of("float.out", "FloatOut", "T", id -> NodeBuilder.node(id)
                .header("FloatOut").output("out", "Out", "float").build()));
        graph.spawn("float.out", "f", 0, 0);
        graph.spawn("int.in", "k", 600, 0);
        reroute("r");
        graph.addConnection("", out("f"), rerouteIn("r"));

        assertFalse(graph.validateConnection(rerouteOut("r"), in("k")).valid());
    }

    @Test
    void reroute_chainsCheckTheRealSourceAgainstTheRealSink() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("int.in", "k", 900, 0);
        reroute("r1");
        reroute("r2");
        graph.addConnection("", out("s"), rerouteIn("r1"));
        graph.addConnection("", rerouteOut("r1"), rerouteIn("r2"));

        assertFalse(graph.validateConnection(rerouteOut("r2"), in("k")).valid());
    }

    @Test
    void reroute_chainWithASinkAtTheEndRejectsAWrongSourceAtTheStart() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("int.in", "k", 900, 0);
        reroute("r1");
        reroute("r2");
        graph.addConnection("", rerouteOut("r1"), rerouteIn("r2"));
        graph.addConnection("", rerouteOut("r2"), in("k"));

        assertFalse(graph.validateConnection(out("s"), rerouteIn("r1")).valid());
    }

    @Test
    void reroute_replacingTheSourceIsCheckedAgainstTheSinks() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("string.in", "k", 600, 0);
        graph.spawn("int.out", "i", 0, 100);
        reroute("r");
        graph.addConnection("", out("s"), rerouteIn("r"));
        graph.addConnection("", rerouteOut("r"), in("k"));

        assertFalse(graph.validateConnection(out("i"), rerouteIn("r")).valid());
    }

    @Test
    void reroute_insertedIntoAWireKeepsItConnectable() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("string.in", "k", 600, 0);
        graph.addConnection("w", "s", "out", "k", "in");

        NodeGraphItem inserted = graph.insertReroute("w", 300.0f, 20.0f);

        assertNotNull(inserted);
        assertEquals(2, graph.connections().size());
    }

    @Test
    void execReroute_isNotAffectedByDataTypeChecks() {
        graph.spawn("exec", "a", 0, 0);
        graph.spawn("exec", "b", 400, 0);
        NodeGraphItem flow = RerouteModule.createNode("rf", true);
        graph.addItem(flow);

        graph.addConnection("", new NodeGraphPortRef("a", "then"), rerouteIn("rf"));
        graph.addConnection("", rerouteOut("rf"), new NodeGraphPortRef("b", "exec-in"));

        assertEquals(2, graph.connections().size());
    }

    @Test
    void draggingAWireFromAnIntoAWrongRerouteChainIsRefused() {
        graph.spawn("string.out", "s", 0, 0);
        graph.spawn("int.in", "k", 600, 0);
        reroute("r");
        graph.addConnection("", rerouteOut("r"), in("k"));

        NodeGraphConnectionValidation validation = graph.validateConnection(out("s"), rerouteIn("r"));

        assertFalse(validation.valid());
    }

    // ---- вложенные регионы ----

    private NodeGraphRegion outer;
    private NodeGraphRegion inner;

    private void buildNested() {
        outer = graph.addRegion("outer", "Outer", 50.0f, 50.0f, 600.0f, 400.0f);
        inner = graph.addRegion("inner", "Inner", 100.0f, 150.0f, 300.0f, 200.0f);
        graph.spawn("string.out", "n", 130.0f, 200.0f);
        layout();
    }

    private void press(float x, float y) {
        graph.handle(new PointerPressedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
    }

    private void move(float x, float y) {
        graph.handle(new PointerMovedEvent(graph, x, y, x, y, 1));
    }

    private void release(float x, float y) {
        graph.handle(new PointerReleasedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
    }

    private void drag(float fromX, float fromY, float toX, float toY) {
        press(fromX, fromY);
        move((fromX + toX) * 0.5f, (fromY + toY) * 0.5f);
        move(toX, toY);
        release(toX, toY);
    }

    @Test
    void region_containsAnotherRegionOnlyWhenItLiesWhollyInside() {
        buildNested();
        NodeGraphRegion straddling = graph.addRegion("s", "S", 500.0f, 300.0f, 300.0f, 300.0f);

        assertTrue(outer.containsRegion(inner));
        assertFalse(inner.containsRegion(outer));
        assertFalse(outer.containsRegion(straddling));
        assertFalse(outer.containsRegion(outer));
        assertEquals(List.of(inner), outer.containedRegions(graph));
    }

    @Test
    void groupingASelectedRegion_wrapsItInANewParentRegion() {
        NodeGraphRegion child = graph.addRegion("child", "Child", 100.0f, 100.0f, 200.0f, 150.0f);
        graph.selectRegion(child);

        NodeGraphRegion parent = graph.groupSelected("Parent");

        assertNotNull(parent);
        assertTrue(parent.containsRegion(child));
        assertEquals(2, graph.regions().size());
        assertSame(parent, graph.selectedRegion());
        assertTrue(parent.y() + NodeGraphRegion.HEADER_HEIGHT < child.y(), "the parent title bar sits above the child");
        assertEquals("Group nodes", graph.history().undoName());
        graph.undo();
        assertEquals(1, graph.regions().size());
    }

    @Test
    void groupingNodesTogetherWithARegion_coversBoth() {
        NodeGraphRegion child = graph.addRegion("child", "Child", 100.0f, 100.0f, 200.0f, 150.0f);
        NodeGraphItem far = graph.spawn("string.out", "far", 500.0f, 400.0f);
        layout();
        graph.selectItem(far, true);
        graph.selectRegion(child);

        NodeGraphRegion parent = graph.groupSelected("Parent");

        assertTrue(parent.containsRegion(child));
        assertTrue(parent.containsItem(far));
    }

    @Test
    void movingTheOuterRegion_carriesTheNestedRegionAndItsNodes() {
        buildNested();
        float nodeX = graph.item("n").x();
        float nodeY = graph.item("n").y();

        drag(300.0f, 60.0f, 340.0f, 90.0f);

        assertEquals(90.0f, outer.x(), EPS);
        assertEquals(80.0f, outer.y(), EPS);
        assertEquals(140.0f, inner.x(), EPS);
        assertEquals(180.0f, inner.y(), EPS);
        assertEquals(nodeX + 40.0f, graph.item("n").x(), EPS);
        assertEquals(nodeY + 30.0f, graph.item("n").y(), EPS);
        assertEquals("Move region", graph.history().undoName());

        graph.undo();

        assertEquals(50.0f, outer.x(), EPS);
        assertEquals(100.0f, inner.x(), EPS);
        assertEquals(150.0f, inner.y(), EPS);
        assertEquals(nodeX, graph.item("n").x(), EPS);
        graph.redo();
        assertEquals(140.0f, inner.x(), EPS);
    }

    @Test
    void movingTheInnerRegion_leavesTheOuterOneAlone() {
        buildNested();

        drag(250.0f, 160.0f, 270.0f, 170.0f);

        assertEquals(120.0f, inner.x(), EPS);
        assertEquals(160.0f, inner.y(), EPS);
        assertEquals(50.0f, outer.x(), EPS);
        assertEquals(50.0f, outer.y(), EPS);
    }

    @Test
    void clickingTheNestedTitleBar_selectsTheNestedRegionNotTheOuterOne() {
        buildNested();

        press(250.0f, 160.0f);
        release(250.0f, 160.0f);

        assertSame(inner, graph.selectedRegion());
    }

    // ---- комментарий региона ----

    private int count(DrawList list, DrawCommandType type) {
        int result = 0;
        for (DrawCommand command : list.commands()) {
            if (command.type() == type) result++;
        }
        return result;
    }

    private DrawList render() {
        DrawList list = new DrawList();
        graph.render(new DefaultRenderContext(list));
        return list;
    }

    @Test
    void regionComment_isNotDrawnUntilTheTitleBarIsHovered() {
        NodeGraphRegion region = graph.addRegion("r", "Setup", 100.0f, 100.0f, 400.0f, 250.0f).comment("Spawns the NPC");
        layout();

        DrawList idle = render();

        assertNull(graph.hoveredRegion());
        assertEquals(1, count(idle, DrawCommandType.TEXT), "only the title is drawn, the comment waits for the hover");

        move(200.0f, 110.0f);
        DrawList hovered = render();

        assertSame(region, graph.hoveredRegion());
        assertEquals(2, count(hovered, DrawCommandType.TEXT), "title plus the comment bubble");
        assertTrue(count(hovered, DrawCommandType.ROUNDED_RECT) > count(idle, DrawCommandType.ROUNDED_RECT),
                "the bubble has its own background and border");
    }

    @Test
    void regionComment_hidesAgainWhenThePointerLeavesTheTitleBar() {
        graph.addRegion("r", "Setup", 100.0f, 100.0f, 400.0f, 250.0f).comment("Spawns the NPC");
        layout();
        move(200.0f, 110.0f);

        move(200.0f, 300.0f);

        assertNull(graph.hoveredRegion());
        assertEquals(1, count(render(), DrawCommandType.TEXT));
    }

    @Test
    void regionWithoutAComment_showsNothingOnHover() {
        graph.addRegion("r", "Setup", 100.0f, 100.0f, 400.0f, 250.0f);
        layout();

        move(200.0f, 110.0f);

        assertEquals(1, count(render(), DrawCommandType.TEXT));
    }

    @Test
    void regionComment_isMarkedByAnIconInTheTitleBar() {
        graph.addRegion("r", "Setup", 100.0f, 100.0f, 400.0f, 250.0f);
        layout();
        int plain = count(render(), DrawCommandType.RECT) + count(render(), DrawCommandType.ROUNDED_RECT);

        graph.region("r").comment("note");
        layout();
        int marked = count(render(), DrawCommandType.RECT) + count(render(), DrawCommandType.ROUNDED_RECT);

        assertTrue(marked >= plain + 5, "bubble, tail and three dots, got " + plain + " -> " + marked);
    }

    @Test
    void regionComment_isNotShownWhileDraggingTheRegion() {
        graph.addRegion("r", "Setup", 100.0f, 100.0f, 400.0f, 250.0f).comment("Spawns the NPC");
        layout();
        move(200.0f, 110.0f);

        press(200.0f, 110.0f);
        move(220.0f, 120.0f);

        assertEquals(1, count(render(), DrawCommandType.TEXT));
        release(220.0f, 120.0f);
    }

    // ---- меню региона ----

    private List<ContextMenu> openMenus() {
        layer.applyQueuedMutations();
        List<ContextMenu> menus = new ArrayList<>();
        for (var child : layer.children()) {
            if (child instanceof ContextMenu menu && menu.opened()) menus.add(menu);
        }
        return menus;
    }

    @Test
    void rightClickOnARegionTitleBar_opensTheRegionMenuWithGroupAndDelete() {
        buildNested();

        graph.handle(new PointerPressedEvent(graph, 250.0f, 160.0f, 250.0f, 160.0f, 1, PointerButton.SECONDARY));

        assertSame(inner, graph.selectedRegion());
        List<ContextMenu> menus = openMenus();
        assertEquals(1, menus.size());
        ContextMenu menu = menus.get(0);
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < menu.itemCount(); i++) texts.add(menu.itemButton(i).text());
        assertEquals(List.of("Undo", "Redo", "Rename", "Comment...", "Group", "Delete"), texts);
        assertFalse(graph.nodePalette().opened());

        menu.itemButton(4).click();

        assertEquals(3, graph.regions().size());
        assertTrue(graph.selectedRegion().containsRegion(inner));
    }

    @Test
    void regionMenuDelete_removesTheRegionAsOneUndoStep() {
        buildNested();
        graph.handle(new PointerPressedEvent(graph, 250.0f, 160.0f, 250.0f, 160.0f, 1, PointerButton.SECONDARY));

        openMenus().get(0).itemButton(5).click();

        assertNull(graph.region("inner"));
        assertNotNull(graph.region("outer"));
        graph.undo();
        assertNotNull(graph.region("inner"));
    }
}
