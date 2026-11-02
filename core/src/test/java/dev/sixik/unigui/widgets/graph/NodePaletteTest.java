package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.node.RerouteModule;
import dev.sixik.unigui.widgets.graph.node.ValueEditors;
import dev.sixik.unigui.widgets.interaction.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Палитра нод: открытие, поиск, выбор, закрытие и интеграция с графом (ПКМ, Space, провод в пустое место). */
class NodePaletteTest {
    private static final float EPS = 1.0e-3f;

    private OverlayLayer layer;
    private NodeGraph graph;
    private NodePalette palette;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph().selectionMode(NodeGraphSelectionMode.MULTIPLE);
        graph.portTypes().register(PortType.data("vec3", 0xFFF5D547)).register(PortType.data("number", 0xFF5CD6A9));
        graph.nodeTypes()
                .register(NodeType.of("src", "Source", "Test", id -> NodeBuilder.node(id)
                        .header("Source").output("out", "Out", "vec3").build()))
                .register(NodeType.of("sink", "Sink", "Test", id -> NodeBuilder.node(id)
                        .header("Sink").input("in", "In", "vec3").build()))
                .register(NodeType.of("math", "Math add", "Math", id -> NodeBuilder.node(id)
                        .header("Math add").input("a", "A", "number").output("sum", "Sum", "number").build()))
                .register(NodeType.of("event", "Event", "Flow", id -> NodeBuilder.node(id)
                        .header("Event").flowRow(null, "then").build()));
        layer = new OverlayLayer();
        layer.content(graph);
        layer.applyQueuedMutations();
        palette = graph.nodePalette();
    }

    private void layout() {
        layer.measure(new LayoutContext(900.0f, 700.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));
    }

    private void press(float x, float y, PointerButton button) {
        graph.handle(new PointerPressedEvent(graph, x, y, x, y, 1, button));
    }

    private void releasePrimary(float x, float y) {
        graph.handle(new PointerReleasedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
    }

    private void move(float x, float y) {
        graph.handle(new PointerMovedEvent(graph, x, y, x, y, 1));
    }

    private void keyOnPalette(int code) {
        palette.handle(new KeyPressedEvent(palette, code, 0, 0));
    }

    private static List<String> ids(List<NodeType> types) {
        return types.stream().map(NodeType::id).toList();
    }

    // ---- открытие и закрытие ----

    @Test
    void graphInsideAnOverlayLayer_findsItsHost() {
        assertSame(layer, graph.parent());
        assertTrue(graph.openNodePalette(100.0f, 100.0f));
    }

    @Test
    void withoutAnOverlayLayer_theGraphCannotOpenAPalette() {
        NodeGraph standalone = new NodeGraph();

        assertFalse(standalone.openNodePalette(10.0f, 10.0f));
    }

    @Test
    void disabledPalette_neverOpens() {
        graph.nodePaletteEnabled(false);

        assertFalse(graph.openNodePalette(10.0f, 10.0f));
        assertFalse(palette.opened());
    }

    @Test
    void open_addsTheOverlayResetsTheQueryAndListsEveryType() {
        palette.searchField().text("leftover");

        graph.openNodePalette(120.0f, 80.0f);
        layer.applyQueuedMutations();

        assertTrue(palette.opened());
        assertSame(layer, palette.parent());
        assertEquals("", palette.searchField().text());
        assertEquals(graph.nodeTypes().all().size(), palette.model().results().size());
    }

    @Test
    void openingTwice_doesNotDuplicateTheOverlay() {
        graph.openNodePalette(10.0f, 10.0f);
        graph.openNodePalette(50.0f, 60.0f);
        layer.applyQueuedMutations();

        long copies = layer.children().stream().filter(child -> child == palette).count();
        assertEquals(1, copies);
    }

    @Test
    void escape_closesAndRemovesTheOverlay() {
        graph.openNodePalette(10.0f, 10.0f);
        layer.applyQueuedMutations();

        keyOnPalette(KeyCodes.ESCAPE);
        layer.applyQueuedMutations();

        assertFalse(palette.opened());
        assertNull(palette.parent());
    }

    @Test
    void clickOutside_closesButClickInsideKeepsItOpen() {
        graph.openNodePalette(10.0f, 10.0f);
        layer.applyQueuedMutations();

        layer.handle(new PointerPressedEvent(palette.searchField(), layer, EventPhase.CAPTURE,
                20.0f, 20.0f, 5.0f, 5.0f, 1, PointerButton.PRIMARY));
        assertTrue(palette.opened(), "a press on the search field belongs to the palette");

        layer.handle(new PointerPressedEvent(graph, layer, EventPhase.CAPTURE,
                800.0f, 600.0f, 800.0f, 600.0f, 1, PointerButton.PRIMARY));
        assertFalse(palette.opened(), "a press elsewhere dismisses it");
    }

    @Test
    void disposingTheGraph_closesTheOpenPalette() {
        graph.openNodePalette(10.0f, 10.0f);
        layer.applyQueuedMutations();

        graph.dispose();

        assertFalse(palette.opened());
    }

    // ---- поиск и навигация ----

    @Test
    void typing_filtersTheListAsYouGo() {
        graph.openNodePalette(10.0f, 10.0f);

        palette.searchField().text("sin");

        assertEquals(List.of("sink"), ids(palette.model().results()));
        assertEquals("Sink", palette.row(0).title());
        assertEquals("Test", palette.row(0).hint(), "a found node shows its category as a hint");
        assertFalse(palette.row(0).isCategory());
        palette.searchField().text("zzz");
        assertEquals("No matching nodes", palette.row(0).title());
        palette.row(0).click();
        assertTrue(palette.opened(), "the empty-state row is inert");
        assertFalse(palette.pickSelected());
    }

    @Test
    void arrowKeys_moveTheSelectionAndWrap() {
        graph.openNodePalette(10.0f, 10.0f);

        keyOnPalette(KeyCodes.DOWN);
        assertEquals(1, palette.model().selectedIndex());
        keyOnPalette(KeyCodes.UP);
        keyOnPalette(KeyCodes.UP);
        assertEquals(palette.model().entries().size() - 1, palette.model().selectedIndex());
    }

    // ---- дерево категорий ----

    @Test
    void emptyQuery_showsTheCategoryTreeCollapsed() {
        graph.openNodePalette(10.0f, 10.0f);

        assertTrue(palette.row(0).isCategory());
        assertEquals("Flow", palette.row(0).title());
        assertEquals("1", palette.row(0).hint(), "a group shows how many nodes it holds");
        assertFalse(palette.row(0).isExpanded());
        assertTrue(palette.row(0).isSelected(), "the first row starts selected");
        assertEquals(List.of("Flow", "Math", "Test", "Utility"),
                List.of(palette.row(0).title(), palette.row(1).title(), palette.row(2).title(), palette.row(3).title()));
    }

    @Test
    void clickingAGroup_expandsItAndKeepsThePaletteOpen() {
        graph.openNodePalette(10.0f, 10.0f);

        palette.row(1).click();

        assertTrue(palette.opened());
        assertTrue(palette.row(1).isExpanded());
        assertEquals("Math add", palette.row(2).title());
        assertEquals(1, palette.row(2).depth());
        assertEquals(0, graph.items().size(), "toggling a group creates nothing");
        palette.row(1).click();
        assertFalse(palette.row(1).isExpanded());
    }

    @Test
    void clickingANodeInsideAnExpandedGroup_picksIt() {
        graph.openNodePalette(10.0f, 10.0f);
        palette.row(1).click();

        palette.row(2).click();

        assertEquals("math", graph.items().get(0).typeId());
        assertFalse(palette.opened());
    }

    @Test
    void arrowKeys_expandCollapseAndEnterTogglesGroups() {
        graph.openNodePalette(10.0f, 10.0f);
        keyOnPalette(KeyCodes.DOWN);

        keyOnPalette(KeyCodes.RIGHT);
        assertTrue(palette.row(1).isExpanded());
        keyOnPalette(KeyCodes.DOWN);
        assertEquals("Math add", palette.row(2).title());
        keyOnPalette(KeyCodes.LEFT);
        assertEquals("Math", palette.model().selected() instanceof NodePaletteModel.CategoryEntry group
                ? group.title() : "", "left on a node steps up to its group");
        keyOnPalette(KeyCodes.LEFT);
        assertFalse(palette.row(1).isExpanded());
        keyOnPalette(KeyCodes.ENTER);
        assertTrue(palette.row(1).isExpanded(), "Enter on a group toggles it");
        assertTrue(palette.opened());
    }

    @Test
    void expandedGroups_areRememberedBetweenOpenings() {
        graph.openNodePalette(10.0f, 10.0f);
        palette.row(1).click();
        palette.close();

        graph.openNodePalette(50.0f, 50.0f);

        assertTrue(palette.row(1).isExpanded());
    }

    @Test
    void arrowsLeftAndRight_areLeftToTheSearchFieldWhileTyping() {
        graph.openNodePalette(10.0f, 10.0f);
        palette.searchField().text("sink");

        keyOnPalette(KeyCodes.RIGHT);
        keyOnPalette(KeyCodes.LEFT);

        assertEquals("Sink", palette.row(0).title());
        assertEquals(1, palette.model().entries().size());
    }

    @Test
    void mouseWheel_scrollsTheListWithoutMovingTheSelection() {
        NodeType[] many = new NodeType[20];
        for (int i = 0; i < many.length; i++) {
            many[i] = NodeType.of("bulk" + i, "Bulk " + i, "", id -> NodeBuilder.node(id).header("Bulk").build());
            graph.nodeTypes().register(many[i]);
        }
        graph.openNodePalette(10.0f, 10.0f);

        palette.handle(new ScrollEvent(palette, 20.0f, 20.0f, 5.0f, 5.0f, 0.0f, -1.0f, 0));

        assertEquals(1, palette.model().windowStart());
        assertEquals(0, palette.model().selectedIndex());
        palette.handle(new ScrollEvent(palette, 20.0f, 20.0f, 5.0f, 5.0f, 0.0f, 1.0f, 0));
        assertEquals(0, palette.model().windowStart());
    }

    @Test
    void hoveringARow_selectsItAndPressingItActivates() {
        graph.openNodePalette(10.0f, 10.0f);

        palette.row(2).handle(new PointerMovedEvent(palette.row(2), 30.0f, 60.0f, 5.0f, 5.0f, 1));

        assertEquals(2, palette.model().selectedIndex());
        assertTrue(palette.row(2).isSelected());
        assertFalse(palette.row(0).isSelected());
        palette.row(2).handle(new PointerPressedEvent(palette.row(2), 30.0f, 60.0f, 5.0f, 5.0f, 1, PointerButton.PRIMARY));
        assertTrue(palette.row(2).isExpanded(), "pressing a group row toggles it");
    }

    @Test
    void rowsOfOneTopCategory_shareAnAccentThatDiffersBetweenCategories() {
        MutableColor math = NodePalette.accentColor("Math");
        MutableColor nested = NodePalette.accentColor("Math/Vector");
        MutableColor flow = NodePalette.accentColor("Flow");
        MutableColor none = NodePalette.accentColor("");

        assertEquals(math.r(), nested.r(), EPS);
        assertEquals(math.g(), nested.g(), EPS);
        assertEquals(math.b(), nested.b(), EPS);
        assertFalse(Math.abs(math.r() - flow.r()) < EPS && Math.abs(math.g() - flow.g()) < EPS
                && Math.abs(math.b() - flow.b()) < EPS, "different categories get different colors");
        assertEquals(none.r(), none.g() - 0.02f, 0.01f, "uncategorized nodes are a neutral gray");
        assertEquals(math.r(), NodePalette.accentColor("math").r(), EPS, "case does not matter");
    }

    @Test
    void groupRowsCarryTheirCategoryAccent() {
        graph.openNodePalette(10.0f, 10.0f);

        assertEquals(NodePalette.accentColor("Math").r(), palette.row(1).accent().r(), EPS);
        assertEquals(NodePalette.accentColor("Flow").g(), palette.row(0).accent().g(), EPS);
    }

    @Test
    void pinPalette_opensWithGroupsExpandedSoMatchesAreVisible() {
        NodeGraphItem source = graph.spawn("src", "s", 0.0f, 0.0f);
        layout();
        float portX = source.x() + source.port("out").anchorX();
        float portY = source.y() + source.port("out").anchorY();

        press(portX, portY, PointerButton.PRIMARY);
        move(500.0f, 400.0f);
        releasePrimary(500.0f, 400.0f);

        assertTrue(palette.model().expandAll());
        assertTrue(palette.model().entries().stream().anyMatch(entry ->
                entry instanceof NodePaletteModel.NodeEntry node && node.type().id().equals("sink")));
    }

    // ---- выбор ----

    @Test
    void enter_spawnsTheSelectedTypeUnderTheOpenPoint() {
        graph.viewport(40.0f, 20.0f, 2.0f);
        graph.openNodePalette(240.0f, 220.0f);
        palette.searchField().text("sink");

        keyOnPalette(KeyCodes.ENTER);

        assertEquals(1, graph.items().size());
        NodeGraphItem created = graph.items().get(0);
        assertEquals("sink", created.typeId());
        assertEquals((240.0f - 40.0f) / 2.0f, created.x(), EPS);
        assertEquals((220.0f - 20.0f) / 2.0f, created.y(), EPS);
        assertTrue(created.selected());
        assertFalse(palette.opened(), "picking closes the palette first");
    }

    @Test
    void clickingARow_picksThatRow() {
        graph.openNodePalette(10.0f, 10.0f);
        palette.searchField().text("math add");

        palette.row(0).click();

        assertEquals("math", graph.items().get(0).typeId());
    }

    @Test
    void pickedNode_isOneUndoableStepAndReplacesTheOldSelection() {
        NodeGraphItem existing = graph.spawn("src", "existing", 500.0f, 500.0f);
        graph.selectItem(existing);
        graph.openNodePalette(10.0f, 10.0f);
        palette.searchField().text("sink");

        keyOnPalette(KeyCodes.ENTER);

        assertFalse(existing.selected(), "only the new node stays selected");
        assertEquals("Add node", graph.history().undoName());
        graph.undo();
        assertEquals(1, graph.items().size());
        graph.redo();
        assertEquals(2, graph.items().size());
    }

    // ---- ПКМ и пробел ----

    @Test
    void rightClickOnEmptySpace_opensThePalette() {
        layout();

        press(400.0f, 300.0f, PointerButton.SECONDARY);

        assertTrue(palette.opened());
    }

    @Test
    void rightClickOnANode_doesNotOpenIt() {
        NodeGraphItem node = graph.spawn("src", "n", 100.0f, 100.0f);
        layout();

        press(node.x() + 30.0f, node.y() + 12.0f, PointerButton.SECONDARY);

        assertFalse(palette.opened());
    }

    @Test
    void rightClickWithoutAnOverlayLayer_isIgnored() {
        NodeGraph standalone = new NodeGraph();
        standalone.measure(new LayoutContext(900.0f, 700.0f));
        standalone.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));

        standalone.handle(new PointerPressedEvent(standalone, 400.0f, 300.0f, 400.0f, 300.0f, 1, PointerButton.SECONDARY));

        assertFalse(standalone.nodePalette().opened());
    }

    @Test
    void space_opensAtTheLastPointerPositionOrTheCenter() {
        layout();
        graph.handle(new KeyPressedEvent(graph, KeyCodes.SPACE, 0, 0));
        assertTrue(palette.opened());
        palette.close();

        move(120.0f, 90.0f);
        graph.handle(new KeyPressedEvent(graph, KeyCodes.SPACE, 0, 0));
        palette.searchField().text("sink");
        keyOnPalette(KeyCodes.ENTER);

        assertEquals(120.0f, graph.items().get(0).x(), EPS);
        assertEquals(90.0f, graph.items().get(0).y(), EPS);
    }

    // ---- горячие клавиши не срабатывают при наборе текста в ноде ----

    @Test
    void keysTypedIntoAFieldInsideANode_doNotTriggerGraphShortcuts() {
        NodeGraphItem node = NodeBuilder.node("form").header("Form")
                .input("name", "Name", "any", ValueEditors.text("x")).build();
        graph.addItem(node);
        graph.selectItem(node);
        layout();
        TextField field = (TextField) node.editor("name").widget();
        float zoomBefore = graph.viewport().zoom();

        for (int code : new int[]{KeyCodes.C, KeyCodes.F, KeyCodes.R, KeyCodes.SPACE, KeyCodes.HOME}) {
            graph.handle(new KeyPressedEvent(field, graph, EventPhase.BUBBLE, code, 0, 0));
        }

        assertEquals(0, graph.regions().size(), "typing C must not group the selected node");
        assertFalse(palette.opened(), "typing a space must not open the palette");
        assertEquals(zoomBefore, graph.viewport().zoom(), EPS);
    }

    @Test
    void deleteBubblingFromAField_doesNotDeleteTheSelectedNode() {
        NodeGraphItem node = NodeBuilder.node("form").header("Form")
                .input("name", "Name", "any", ValueEditors.text("x")).build();
        graph.addItem(node);
        graph.selectItem(node);
        TextField field = (TextField) node.editor("name").widget();

        graph.handle(new KeyPressedEvent(field, graph, EventPhase.BUBBLE, KeyCodes.DELETE, 0, 0));

        assertNotNull(graph.item("form"));
    }

    @Test
    void shortcutsStillWorkWhenTheGraphItselfIsTheTarget() {
        NodeGraphItem node = graph.spawn("src", "n", 100.0f, 100.0f);
        graph.selectItem(node);
        layout();

        graph.handle(new KeyPressedEvent(graph, KeyCodes.C, 0, 0));

        assertEquals(1, graph.regions().size());
    }

    // ---- провод в пустое место ----

    private float[] outputPortOf(NodeGraphItem node) {
        NodeGraphPort port = node.port("out");
        return new float[]{node.x() + port.anchorX(), node.y() + port.anchorY()};
    }

    @Test
    void wireDroppedOnEmptySpace_opensAPaletteOfCompatibleTypesOnly() {
        NodeGraphItem source = graph.spawn("src", "s", 0.0f, 0.0f);
        layout();
        float[] port = outputPortOf(source);

        press(port[0], port[1], PointerButton.PRIMARY);
        move(500.0f, 400.0f);
        releasePrimary(500.0f, 400.0f);

        assertTrue(palette.opened());
        List<String> offered = ids(palette.model().results());
        assertTrue(offered.contains("sink"), offered.toString());
        assertTrue(offered.contains(RerouteModule.DATA_TYPE_ID), "a data reroute accepts any data type");
        assertFalse(offered.contains("src"), "a source has no input");
        assertFalse(offered.contains("math"), "number input does not accept vec3");
        assertFalse(offered.contains("event"), "exec-only node cannot take data");
        assertFalse(offered.contains(RerouteModule.FLOW_TYPE_ID));
    }

    @Test
    void pickingFromThePinPalette_createsAndConnectsTheNode() {
        NodeGraphItem source = graph.spawn("src", "s", 0.0f, 0.0f);
        layout();
        float[] port = outputPortOf(source);
        press(port[0], port[1], PointerButton.PRIMARY);
        move(500.0f, 400.0f);
        releasePrimary(500.0f, 400.0f);
        palette.searchField().text("sink");

        keyOnPalette(KeyCodes.ENTER);

        NodeGraphItem sink = graph.items().stream().filter(item -> item.typeId().equals("sink")).findFirst().orElseThrow();
        assertEquals(500.0f, sink.x(), EPS);
        assertEquals(1, graph.connections().size());
        assertEquals("s", graph.connections().get(0).fromItemId());
        assertEquals(sink.id(), graph.connections().get(0).toItemId());
        assertEquals("in", graph.connections().get(0).toPortId());
        assertEquals("Add node", graph.history().undoName());
        graph.undo();
        assertEquals(1, graph.items().size(), "undo removes the node together with its wire");
        assertEquals(0, graph.connections().size());
    }

    @Test
    void wireDroppedOnANodeBody_doesNotOpenAPalette() {
        NodeGraphItem source = graph.spawn("src", "s", 0.0f, 0.0f);
        NodeGraphItem other = graph.spawn("src", "other", 400.0f, 300.0f);
        layout();
        float[] port = outputPortOf(source);

        press(port[0], port[1], PointerButton.PRIMARY);
        move(other.x() + 40.0f, other.y() + 12.0f);
        releasePrimary(other.x() + 40.0f, other.y() + 12.0f);

        assertFalse(palette.opened());
    }

    @Test
    void wireDroppedOnACompatiblePort_connectsWithoutAPalette() {
        NodeGraphItem source = graph.spawn("src", "s", 0.0f, 0.0f);
        NodeGraphItem sink = graph.spawn("sink", "k", 400.0f, 0.0f);
        layout();
        float[] port = outputPortOf(source);
        float targetX = sink.x() + sink.port("in").anchorX();
        float targetY = sink.y() + sink.port("in").anchorY();

        press(port[0], port[1], PointerButton.PRIMARY);
        move(targetX, targetY);
        releasePrimary(targetX, targetY);

        assertEquals(1, graph.connections().size());
        assertFalse(palette.opened());
    }

    @Test
    void pinPalette_withoutAnOverlayLayer_silentlySkipsTheMenu() {
        NodeGraph standalone = new NodeGraph();
        standalone.portTypes().register(PortType.data("vec3", 0xFFF5D547));
        NodeGraphItem source = NodeBuilder.node("s").header("S").output("out", "Out", "vec3").build();
        standalone.addItem(source);
        standalone.measure(new LayoutContext(900.0f, 700.0f));
        standalone.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));
        float portX = source.x() + source.port("out").anchorX();
        float portY = source.y() + source.port("out").anchorY();

        standalone.handle(new PointerPressedEvent(standalone, portX, portY, portX, portY, 1, PointerButton.PRIMARY));
        standalone.handle(new PointerReleasedEvent(standalone, 500.0f, 400.0f, 500.0f, 400.0f, 1, PointerButton.PRIMARY));

        assertFalse(standalone.nodePalette().opened());
        assertEquals(1, standalone.items().size());
    }

    // ---- NodeType: порты по пробной ноде ----

    @Test
    void nodeType_sketchesItsPortsOnceAndAnswersCompatibilityQuestions() {
        NodeType sink = graph.nodeTypes().get("sink");

        assertEquals(1, sink.ports().stream().filter(p -> p.id().equals("in")).count());
        assertSame(sink.ports(), sink.ports(), "the probe result is cached");
        assertTrue(sink.acceptsConnectionFrom("vec3", graph.portTypes()));
        assertFalse(sink.acceptsConnectionFrom("number", graph.portTypes()));
        assertEquals("in", sink.firstCompatibleInput("vec3", graph.portTypes()));
        assertNull(sink.firstCompatibleInput("flow", graph.portTypes()));
    }

    @Test
    void nodeType_withABrokenFactory_hasNoPortsInsteadOfFailing() {
        NodeType broken = NodeType.of("broken", "Broken", "", id -> {
            throw new IllegalStateException("boom");
        });

        assertTrue(broken.ports().isEmpty());
        assertFalse(broken.acceptsConnectionFrom("vec3", graph.portTypes()));
    }

    @Test
    void pickedNodes_getUniqueIds() {
        graph.openNodePalette(10.0f, 10.0f);
        palette.searchField().text("sink");
        keyOnPalette(KeyCodes.ENTER);
        graph.openNodePalette(10.0f, 10.0f);
        palette.searchField().text("sink");
        keyOnPalette(KeyCodes.ENTER);

        List<String> idsOfNodes = new ArrayList<>();
        graph.items().forEach(item -> idsOfNodes.add(item.id()));
        assertEquals(2, idsOfNodes.stream().distinct().count());
    }
}
