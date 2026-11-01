package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Привязка по Ctrl, выделение по Shift, перетаскивание группы, двойной клик по проводу и меню по ПКМ на ноде.
 *
 * <p>Сцена: три ноды {@code a}, {@code b}, {@code c} в ряд (по 140 шириной), ниже пара {@code s -> k} с горизонтальным
 * проводом на высоте 35.</p>
 */
class NodeGraphInteractionTest {
    private static final float EPS = 1.0e-3f;

    private OverlayLayer layer;
    private NodeGraph graph;
    private DefaultUIContext context;
    private NodeGraphItem a;
    private NodeGraphItem b;
    private NodeGraphItem c;
    private final long[] now = {1_000L};

    @BeforeEach
    void setUp() {
        graph = new NodeGraph().selectionMode(NodeGraphSelectionMode.MULTIPLE).gridSize(24.0f);
        graph.portTypes().register(PortType.data("vec3", 0xFFF5D547));
        graph.nodeTypes()
                .register(NodeType.of("src", "Source", "Test", id -> NodeBuilder.node(id)
                        .header("Source").output("out", "Out", "vec3").build()))
                .register(NodeType.of("sink", "Sink", "Test", id -> NodeBuilder.node(id)
                        .header("Sink").input("in", "In", "vec3").build()));
        a = graph.spawn("src", "a", 100.0f, 100.0f);
        b = graph.spawn("src", "b", 300.0f, 100.0f);
        c = graph.spawn("src", "c", 500.0f, 100.0f);
        graph.spawn("src", "s", 0.0f, 400.0f);
        graph.spawn("sink", "k", 500.0f, 400.0f);
        graph.addConnection("wire", "s", "out", "k", "in");
        graph.clock(() -> now[0]);
        layer = new OverlayLayer();
        layer.content(graph);
        layer.applyQueuedMutations();
        context = new DefaultUIContext();
        graph.setUiContextInternal(context);
        layout();
    }

    private void layout() {
        layer.measure(new LayoutContext(900.0f, 700.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));
    }

    private void press(float x, float y) {
        graph.handle(new PointerPressedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
    }

    private void pressSecondary(float x, float y) {
        graph.handle(new PointerPressedEvent(graph, x, y, x, y, 1, PointerButton.SECONDARY));
    }

    private void move(float x, float y) {
        graph.handle(new PointerMovedEvent(graph, x, y, x, y, 1));
    }

    private void release(float x, float y) {
        graph.handle(new PointerReleasedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
    }

    private void click(float x, float y) {
        press(x, y);
        release(x, y);
    }

    private void drag(float fromX, float fromY, float toX, float toY) {
        press(fromX, fromY);
        move((fromX + toX) * 0.5f, (fromY + toY) * 0.5f);
        move(toX, toY);
        release(toX, toY);
    }

    /** Точка на заголовке ноды, не на стрелке сворачивания. */
    private static float[] grab(NodeGraphItem node) {
        return new float[]{node.x() + 30.0f, node.y() + 12.0f};
    }

    private void clickNode(NodeGraphItem node) {
        float[] p = grab(node);
        click(p[0], p[1]);
    }

    private void holdCtrl(boolean held) {
        if (held) context.keyboard().press(KeyCodes.LEFT_CONTROL);
        else context.keyboard().release(KeyCodes.LEFT_CONTROL);
    }

    private void holdShift(boolean held) {
        if (held) context.keyboard().press(KeyCodes.LEFT_SHIFT);
        else context.keyboard().release(KeyCodes.LEFT_SHIFT);
    }

    private static List<String> selectedIds(NodeGraph graph) {
        return graph.selectedItemIds().stream().sorted().toList();
    }

    // ---- привязка к сетке при зажатом Ctrl ----

    @Test
    void withoutCtrl_dragIsFree() {
        float[] p = grab(a);

        drag(p[0], p[1], p[0] + 31.0f, p[1] + 17.0f);

        assertEquals(131.0f, a.x(), EPS);
        assertEquals(117.0f, a.y(), EPS);
    }

    @Test
    void holdingCtrl_snapsTheDraggedNodeToTheGrid() {
        float[] p = grab(a);
        holdCtrl(true);

        drag(p[0], p[1], p[0] + 31.0f, p[1] + 17.0f);

        assertEquals(120.0f, a.x(), EPS);
        assertEquals(120.0f, a.y(), EPS);
    }

    @Test
    void releasingCtrlMidDrag_stopsSnappingImmediately() {
        float[] p = grab(a);
        holdCtrl(true);
        press(p[0], p[1]);
        move(p[0] + 31.0f, p[1] + 17.0f);
        assertEquals(120.0f, a.x(), EPS);

        holdCtrl(false);
        move(p[0] + 33.0f, p[1] + 19.0f);
        release(p[0] + 33.0f, p[1] + 19.0f);

        assertEquals(133.0f, a.x(), EPS);
        assertEquals(119.0f, a.y(), EPS);
    }

    @Test
    void withTheToggleOn_ctrlSuspendsSnappingInstead() {
        graph.snapToGrid(true);
        float[] p = grab(a);

        drag(p[0], p[1], p[0] + 31.0f, p[1] + 17.0f);
        assertEquals(120.0f, a.x(), EPS, "toggle on and no Ctrl: snapped");

        holdCtrl(true);
        float[] q = grab(b);
        drag(q[0], q[1], q[0] + 31.0f, q[1] + 17.0f);
        assertEquals(331.0f, b.x(), EPS, "toggle on and Ctrl held: free");
    }

    @Test
    void holdingCtrl_snapsRegionsAndKeepsCarriedNodesAligned() {
        NodeGraphRegion region = graph.addRegion("area", "Area", 80.0f, 70.0f, 400.0f, 150.0f);
        holdCtrl(true);
        float headerX = region.x() + 380.0f;
        float headerY = region.y() + 10.0f;

        drag(headerX, headerY, headerX + 31.0f, headerY + 17.0f);

        assertEquals(0.0f, region.x() % 24.0f, EPS);
        assertEquals(0.0f, region.y() % 24.0f, EPS);
        assertEquals(a.x() - region.x(), 100.0f - 80.0f, EPS, "the carried node keeps its offset to the region");
    }

    // ---- выделение по Shift ----

    @Test
    void plainClick_selectsOnlyThatNode() {
        clickNode(a);
        clickNode(b);

        assertEquals(List.of("b"), selectedIds(graph));
    }

    @Test
    void shiftClick_addsToTheSelectionAndTogglesOff() {
        clickNode(a);
        holdShift(true);

        clickNode(b);
        assertEquals(List.of("a", "b"), selectedIds(graph));
        clickNode(c);
        assertEquals(List.of("a", "b", "c"), selectedIds(graph));
        clickNode(a);
        assertEquals(List.of("b", "c"), selectedIds(graph));
    }

    @Test
    void shiftClickOnASelectedNode_deselectsItWithoutStartingADrag() {
        clickNode(a);
        holdShift(true);
        float[] p = grab(a);

        press(p[0], p[1]);
        move(p[0] + 60.0f, p[1] + 40.0f);
        release(p[0] + 60.0f, p[1] + 40.0f);

        assertFalse(a.selected());
        assertEquals(100.0f, a.x(), EPS, "the node stays put");
    }

    @Test
    void shiftIsIgnoredInSingleSelectionMode() {
        graph.selectionMode(NodeGraphSelectionMode.SINGLE);
        clickNode(a);
        holdShift(true);

        clickNode(b);

        assertEquals(List.of("b"), selectedIds(graph));
    }

    @Test
    void shiftClick_alsoClearsAWireSelection() {
        click(250.0f, 435.0f);
        assertEquals(List.of("wire"), graph.selectedConnectionIds());
        holdShift(true);

        clickNode(a);

        assertTrue(graph.selectedConnectionIds().isEmpty());
        assertEquals(List.of("a"), selectedIds(graph));
    }

    @Test
    void shiftLasso_addsToTheSelectionButAPlainLassoReplacesIt() {
        clickNode(a);
        holdShift(true);

        drag(280.0f, 80.0f, 460.0f, 200.0f);

        assertEquals(List.of("a", "b"), selectedIds(graph));

        holdShift(false);
        drag(480.0f, 80.0f, 660.0f, 200.0f);

        assertEquals(List.of("c"), selectedIds(graph), "without Shift the lasso starts a new selection");
    }

    @Test
    void shiftClickOnEmptySpace_keepsTheSelection() {
        clickNode(a);
        clickNode(b);
        holdShift(true);

        click(700.0f, 300.0f);

        assertEquals(List.of("b"), selectedIds(graph).stream().filter(id -> id.equals("b")).toList());
        assertTrue(b.selected());
    }

    @Test
    void plainClickOnEmptySpace_clearsTheSelection() {
        clickNode(a);

        click(700.0f, 300.0f);

        assertTrue(selectedIds(graph).isEmpty());
    }

    // ---- перетаскивание группы ----

    @Test
    void draggingASelectedNode_carriesTheWholeSelection() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        holdShift(false);
        float[] p = grab(a);

        drag(p[0], p[1], p[0] + 50.0f, p[1] + 30.0f);

        assertEquals(150.0f, a.x(), EPS);
        assertEquals(130.0f, a.y(), EPS);
        assertEquals(350.0f, b.x(), EPS);
        assertEquals(130.0f, b.y(), EPS);
        assertEquals(500.0f, c.x(), EPS, "an unselected node stays");
    }

    @Test
    void groupDrag_isOneUndoStepForEveryNode() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        holdShift(false);
        float[] p = grab(b);
        drag(p[0], p[1], p[0] + 50.0f, p[1] + 30.0f);

        assertEquals("Move nodes", graph.history().undoName());
        graph.undo();

        assertEquals(100.0f, a.x(), EPS);
        assertEquals(300.0f, b.x(), EPS);
        assertEquals(100.0f, b.y(), EPS);
        graph.redo();
        assertEquals(350.0f, b.x(), EPS);
        assertEquals(150.0f, a.x(), EPS);
    }

    @Test
    void groupDrag_reportsAMoveEventPerNode() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        holdShift(false);
        List<String> moved = new ArrayList<>();
        graph.onItemMoved(event -> moved.add(event.itemId()));
        float[] p = grab(a);

        press(p[0], p[1]);
        move(p[0] + 10.0f, p[1]);

        assertTrue(moved.containsAll(List.of("a", "b")), moved.toString());
    }

    @Test
    void pressInsideAGroupWithoutMoving_collapsesTheSelectionToThatNode() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        clickNode(c);
        holdShift(false);
        assertEquals(3, graph.selectedItemIds().size());

        clickNode(b);

        assertEquals(List.of("b"), selectedIds(graph));
    }

    @Test
    void pressInsideAGroup_doesNotCollapseItBeforeTheDrag() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        holdShift(false);
        float[] p = grab(b);

        press(p[0], p[1]);

        assertEquals(List.of("a", "b"), selectedIds(graph), "the group is intact while the mouse is down");
        release(p[0], p[1]);
    }

    @Test
    void draggingAnUnselectedNode_movesOnlyIt() {
        clickNode(a);
        float[] p = grab(b);

        drag(p[0], p[1], p[0] + 40.0f, p[1] + 20.0f);

        assertEquals(340.0f, b.x(), EPS);
        assertEquals(100.0f, a.x(), EPS);
        assertEquals(List.of("b"), selectedIds(graph));
    }

    @Test
    void groupDrag_withCtrl_snapsTheGrabbedNodeAndKeepsTheOffsets() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        holdShift(false);
        holdCtrl(true);
        float[] p = grab(a);

        drag(p[0], p[1], p[0] + 31.0f, p[1] + 17.0f);

        assertEquals(120.0f, a.x(), EPS);
        assertEquals(120.0f, a.y(), EPS);
        assertEquals(320.0f, b.x(), EPS, "b moved by the same snapped shift as a");
        assertEquals(120.0f, b.y(), EPS);
    }

    // ---- двойной клик по проводу ----

    @Test
    void doubleClickOnAWire_insertsARerouteUnderTheCursor() {
        click(250.0f, 435.0f);
        now[0] += 200L;

        click(252.0f, 435.0f);

        assertNull(graph.connection("wire"));
        assertEquals(2, graph.connections().size());
        NodeGraphItem reroute = graph.items().stream()
                .filter(item -> item.typeId().equals(RerouteModule.DATA_TYPE_ID)).findFirst().orElseThrow();
        assertEquals(252.0f - 18.0f, reroute.x(), EPS);
        assertEquals(435.0f - 10.0f, reroute.y(), EPS);
        assertTrue(reroute.selected(), "the new reroute is the selection");
        assertTrue(graph.selectedConnectionIds().isEmpty());
    }

    @Test
    void doubleClick_isOneUndoStep() {
        click(250.0f, 435.0f);
        click(250.0f, 435.0f);

        assertEquals("Insert reroute", graph.history().undoName());
        graph.undo();
        assertNotNull(graph.connection("wire"));
    }

    @Test
    void aSlowSecondClick_justSelectsTheWire() {
        click(250.0f, 435.0f);
        now[0] += 800L;

        click(250.0f, 435.0f);

        assertNotNull(graph.connection("wire"));
        assertEquals(List.of("wire"), graph.selectedConnectionIds());
    }

    @Test
    void aSecondClickFarFromTheFirst_isNotADoubleClick() {
        click(250.0f, 435.0f);
        now[0] += 100L;

        click(300.0f, 435.0f);

        assertNotNull(graph.connection("wire"));
    }

    @Test
    void clicksOnDifferentWires_doNotCombine() {
        graph.spawn("src", "s2", 0.0f, 550.0f);
        graph.spawn("sink", "k2", 500.0f, 550.0f);
        graph.addConnection("wire2", "s2", "out", "k2", "in");
        layout();
        click(250.0f, 435.0f);
        now[0] += 100L;

        click(250.0f, 585.0f);

        assertNotNull(graph.connection("wire"));
        assertNotNull(graph.connection("wire2"));
        assertEquals(List.of("wire2"), graph.selectedConnectionIds());
    }

    @Test
    void disablingTheInterval_turnsDoubleClickOff() {
        graph.doubleClickMillis(0);
        click(250.0f, 435.0f);
        click(250.0f, 435.0f);

        assertNotNull(graph.connection("wire"));
        assertEquals(0, graph.doubleClickMillis());
    }

    @Test
    void doubleClickOnAnExecWire_insertsAnExecReroute() {
        graph.nodeTypes().register(NodeType.of("event", "Event", "Flow", id -> NodeBuilder.node(id)
                .header("Event").flowRow(null, "then").build()));
        graph.nodeTypes().register(NodeType.of("action", "Action", "Flow", id -> NodeBuilder.node(id)
                .header("Action").flowRow("exec", "then").build()));
        graph.spawn("event", "e", 0.0f, 250.0f);
        graph.spawn("action", "x", 500.0f, 250.0f);
        graph.addConnection("flowWire", "e", "then", "x", "exec");
        layout();
        NodeGraphPort out = graph.item("e").port("then");
        float y = graph.item("e").y() + out.anchorY();

        click(300.0f, y);
        click(300.0f, y);

        assertTrue(graph.items().stream().anyMatch(item -> item.typeId().equals(RerouteModule.FLOW_TYPE_ID)));
    }

    // ---- меню по ПКМ на ноде ----

    private List<ContextMenu> openMenus() {
        layer.applyQueuedMutations();
        List<ContextMenu> menus = new ArrayList<>();
        for (var child : layer.children()) {
            if (child instanceof ContextMenu menu && menu.opened()) menus.add(menu);
        }
        return menus;
    }

    private ContextMenu rootMenu() {
        List<ContextMenu> menus = openMenus();
        assertFalse(menus.isEmpty(), "a context menu should be open");
        return menus.get(0);
    }

    private static List<String> texts(ContextMenu menu) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < menu.itemCount(); i++) texts.add(menu.itemButton(i).text());
        return texts;
    }

    private static List<Boolean> enabled(ContextMenu menu) {
        List<Boolean> states = new ArrayList<>();
        for (int i = 0; i < menu.itemCount(); i++) states.add(menu.itemButton(i).enabled());
        return states;
    }

    @Test
    void rightClickOnANode_opensTheMenuWithTheRequestedActions() {
        float[] p = grab(a);

        pressSecondary(p[0], p[1]);

        ContextMenu menu = rootMenu();
        assertEquals(List.of("Undo", "Redo", "Duplicate", "Group", "Align ▶", "Spread ▶"), texts(menu));
    }

    @Test
    void aSingleSelectedNode_enablesDuplicateAndGroupOnly() {
        float[] p = grab(a);

        pressSecondary(p[0], p[1]);

        assertEquals(List.of(false, false, true, true, false, false), enabled(rootMenu()));
    }

    @Test
    void threeSelectedNodes_enableAlignAndSpread() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        clickNode(c);
        holdShift(false);
        float[] p = grab(b);

        pressSecondary(p[0], p[1]);

        assertEquals(List.of(false, false, true, true, true, true), enabled(rootMenu()));
        assertEquals(3, graph.selectedItemIds().size(), "a right click on a selected node keeps the group");
    }

    @Test
    void rightClickOnAnUnselectedNode_selectsItExclusively() {
        clickNode(a);
        float[] p = grab(b);

        pressSecondary(p[0], p[1]);

        assertEquals(List.of("b"), selectedIds(graph));
    }

    @Test
    void undoAndRedoItems_followTheHistory() {
        graph.spawn("src", "extra", 700.0f, 300.0f);
        graph.transact("edit", () -> graph.removeItem("extra"));
        float[] p = grab(a);

        pressSecondary(p[0], p[1]);
        ContextMenu menu = rootMenu();
        assertTrue(menu.itemButton(0).enabled(), "there is something to undo");
        assertFalse(menu.itemButton(1).enabled());
        menu.itemButton(0).click();

        assertNotNull(graph.item("extra"));
        assertFalse(menu.opened(), "choosing an action closes the menu");
        pressSecondary(p[0], p[1]);
        assertTrue(rootMenu().itemButton(1).enabled(), "now there is something to redo");
    }

    @Test
    void duplicateAndGroupItems_actOnTheSelection() {
        float[] p = grab(a);
        pressSecondary(p[0], p[1]);
        rootMenu().itemButton(2).click();

        assertEquals(6, graph.items().size());

        pressSecondary(p[0], p[1]);
        rootMenu().itemButton(3).click();

        assertEquals(1, graph.regions().size());
    }

    @Test
    void alignSubmenu_listsLeftRightTopBottomAndAlignsTheSelection() {
        graph.item("b").position(300.0f, 220.0f);
        graph.item("c").position(560.0f, 160.0f);
        layout();
        clickNode(a);
        holdShift(true);
        clickNode(b);
        clickNode(c);
        holdShift(false);
        float[] p = grab(a);

        pressSecondary(p[0], p[1]);
        rootMenu().itemButton(4).click();

        ContextMenu align = openMenus().stream().filter(menu -> menu.itemCount() == 4).findFirst().orElseThrow();
        assertEquals(List.of("Left", "Right", "Top", "Bottom"), texts(align));
        align.itemButton(0).click();
        assertEquals(100.0f, graph.item("b").x(), EPS);
        assertEquals(100.0f, graph.item("c").x(), EPS);
        assertEquals("Align nodes", graph.history().undoName());
    }

    @Test
    void alignSubmenu_topAndBottomUseTheSelectionBounds() {
        graph.item("b").position(300.0f, 220.0f);
        layout();
        clickNode(a);
        holdShift(true);
        clickNode(b);
        holdShift(false);
        float[] p = grab(a);
        pressSecondary(p[0], p[1]);
        rootMenu().itemButton(4).click();
        ContextMenu align = openMenus().stream().filter(menu -> menu.itemCount() == 4).findFirst().orElseThrow();

        align.itemButton(2).click();

        assertEquals(100.0f, graph.item("b").y(), EPS, "Top lines everything up with the highest node");
    }

    private ContextMenu openSpreadSubmenu(NodeGraphItem menuNode) {
        float[] p = grab(menuNode);
        pressSecondary(p[0], p[1]);
        rootMenu().itemButton(5).click();
        return openMenus().stream().filter(menu -> menu.itemCount() == 2).findFirst().orElseThrow();
    }

    private void selectAll3() {
        clickNode(a);
        holdShift(true);
        clickNode(b);
        clickNode(c);
        holdShift(false);
    }

    @Test
    void spreadSubmenu_distributesHorizontally() {
        graph.item("b").position(380.0f, 100.0f);
        layout();
        selectAll3();
        ContextMenu spread = openSpreadSubmenu(a);

        assertEquals(List.of("Horizontally", "Vertically"), texts(spread));
        spread.itemButton(0).click();

        float gapAb = graph.item("b").x() - (graph.item("a").x() + graph.item("a").arrangedWidth());
        float gapBc = graph.item("c").x() - (graph.item("b").x() + graph.item("b").arrangedWidth());
        assertEquals(gapAb, gapBc, 0.01f);
        assertEquals(300.0f, graph.item("b").x(), EPS, "b moved to the middle");
        assertEquals("Distribute nodes", graph.history().undoName());
    }

    @Test
    void spreadSubmenu_distributesVertically() {
        graph.item("a").position(100.0f, 100.0f);
        graph.item("b").position(300.0f, 130.0f);
        graph.item("c").position(500.0f, 380.0f);
        layout();
        selectAll3();
        ContextMenu spread = openSpreadSubmenu(a);

        spread.itemButton(1).click();

        float gapAb = graph.item("b").y() - (graph.item("a").y() + graph.item("a").arrangedHeight());
        float gapBc = graph.item("c").y() - (graph.item("b").y() + graph.item("b").arrangedHeight());
        assertEquals(gapAb, gapBc, 0.01f);
        assertTrue(graph.item("b").y() > 130.0f, "b was pushed down towards the middle");
    }

    @Test
    void rightClickOnEmptySpace_stillOpensThePaletteNotTheMenu() {
        pressSecondary(700.0f, 300.0f);

        assertTrue(graph.nodePalette().opened());
        assertTrue(openMenus().isEmpty());
    }

    @Test
    void rightClickOnAPort_opensNothing() {
        NodeGraphItem source = graph.item("s");
        float portX = source.x() + source.port("out").anchorX();
        float portY = source.y() + source.port("out").anchorY();

        pressSecondary(portX, portY);

        assertTrue(openMenus().isEmpty());
        assertFalse(graph.nodePalette().opened());
    }

    @Test
    void disabledMenu_neverOpens() {
        graph.nodeContextMenuEnabled(false);
        float[] p = grab(a);

        pressSecondary(p[0], p[1]);

        assertTrue(openMenus().isEmpty());
        assertFalse(graph.nodeContextMenuEnabled());
    }

    @Test
    void withoutAnOverlayLayer_theMenuCannotOpen() {
        NodeGraph standalone = new NodeGraph();

        assertFalse(standalone.openNodeMenu(10.0f, 10.0f));
    }

    private static void assertNull(Object value) {
        org.junit.jupiter.api.Assertions.assertNull(value);
    }
}
