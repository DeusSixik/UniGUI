package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.widgets.graph.NodePaletteModel.CategoryEntry;
import dev.sixik.unigui.widgets.graph.NodePaletteModel.Entry;
import dev.sixik.unigui.widgets.graph.NodePaletteModel.NodeEntry;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.NodeTypeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodePaletteModelTest {
    private NodeTypeRegistry registry;

    private static NodeType type(String id, String title, String category) {
        return NodeType.of(id, title, category, itemId -> NodeBuilder.node(itemId).header(title).build());
    }

    /** Короткое описание строки для сравнения: {@code +Math(3)} — группа, {@code .Add} — нода, отступ — глубина. */
    private static List<String> describe(List<Entry> entries) {
        return entries.stream().map(entry -> {
            String indent = "  ".repeat(entry.depth());
            if (entry instanceof CategoryEntry category) {
                return indent + (category.expanded() ? "-" : "+") + category.title() + "(" + category.count() + ")";
            }
            return indent + "." + ((NodeEntry) entry).type().title();
        }).toList();
    }

    private static List<String> ids(List<NodeType> types) {
        return types.stream().map(NodeType::id).toList();
    }

    @BeforeEach
    void setUp() {
        registry = new NodeTypeRegistry()
                .register(type("print", "Print String", "Utility"))
                .register(type("sprint", "Sprint", "Movement"))
                .register(type("add", "Add", "Math"))
                .register(type("vadd", "Vector add", "Math/Vector"))
                .register(type("dot", "Dot", "Math/Vector"))
                .register(type("branch", "Branch", "Flow"))
                .register(type("note", "Note", ""));
    }

    // ---- дерево ----

    @Test
    void emptyQuery_showsCollapsedCategoriesSortedWithUncategorizedNodesLast() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        assertEquals(List.of("+Flow(1)", "+Math(3)", "+Movement(1)", "+Utility(1)", ".Note"), describe(model.entries()));
        assertEquals(0, model.selectedIndex());
        assertNull(model.selectedType(), "a group is selected first, not a node");
    }

    @Test
    void expandingAGroup_revealsSubgroupsBeforeNodes() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        model.expand("Math");

        assertEquals(List.of("+Flow(1)", "-Math(3)", "  +Vector(2)", "  .Add", "+Movement(1)", "+Utility(1)", ".Note"),
                describe(model.entries()));
        model.expand("Math/Vector");
        assertEquals(List.of("+Flow(1)", "-Math(3)", "  -Vector(2)", "    .Dot", "    .Vector add", "  .Add",
                "+Movement(1)", "+Utility(1)", ".Note"), describe(model.entries()));
    }

    @Test
    void collapseAndToggle_workByPath() {
        NodePaletteModel model = new NodePaletteModel(registry, 20).expand("Math").expand("Math/Vector");

        model.collapse("Math");
        assertEquals(List.of("+Flow(1)", "+Math(3)", "+Movement(1)", "+Utility(1)", ".Note"), describe(model.entries()));
        model.toggle("Flow");
        assertTrue(describe(model.entries()).contains("  .Branch"));
        model.toggle("Flow");
        assertFalse(describe(model.entries()).contains("  .Branch"));
    }

    @Test
    void expandAll_showsEveryGroupOpen() {
        NodePaletteModel model = new NodePaletteModel(registry, 20).expandAll(true);

        assertTrue(model.expandAll());
        assertEquals(List.of("-Flow(1)", "  .Branch", "-Math(3)", "  -Vector(2)", "    .Dot", "    .Vector add", "  .Add",
                "-Movement(1)", "  .Sprint", "-Utility(1)", "  .Print String", ".Note"), describe(model.entries()));
    }

    @Test
    void categoryPaths_areNormalizedAndMergedIgnoringCase() {
        NodeTypeRegistry messy = new NodeTypeRegistry()
                .register(type("a", "A", " Math / Vector "))
                .register(type("b", "B", "math//vector"))
                .register(type("c", "C", "MATH"));

        NodePaletteModel model = new NodePaletteModel(messy, 20).expandAll(true);

        assertEquals(List.of("-Math(3)", "  -Vector(2)", "    .A", "    .B", "  .C"), describe(model.entries()));
    }

    @Test
    void filter_hidesCategoriesWithoutMatchingNodes() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        model.filter(type -> type.category().startsWith("Math"));

        assertEquals(List.of("+Math(3)"), describe(model.entries()));
        model.filter(null);
        assertEquals(5, model.entries().size());
    }

    @Test
    void togglingAGroup_keepsTheSelectionOnThatGroup() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);
        model.select(1);

        model.toggle("Math");

        assertEquals(1, model.selectedIndex());
        assertEquals("Math", ((CategoryEntry) model.selected()).path());
        model.expand("Flow");
        assertEquals("Math", ((CategoryEntry) model.selected()).path(), "rows above shift the index, not the selection");
        assertEquals(2, model.selectedIndex());
    }

    // ---- поиск ----

    @Test
    void query_replacesTheTreeWithAFlatRankedList() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);
        model.move(2);

        model.query("pri");

        assertEquals(List.of(".Print String", ".Sprint"), describe(model.entries()));
        assertEquals(List.of("print", "sprint"), ids(model.results()));
        assertEquals(0, model.selectedIndex());
        assertEquals("print", model.selectedType().id());
        assertEquals("pri", model.query());
    }

    @Test
    void query_matchesCategoryPathAndIgnoresCase() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        model.query("VECTOR");

        assertEquals(List.of("vadd", "dot"), ids(model.results()), "title prefix first, then category matches");
    }

    @Test
    void clearingTheQuery_bringsTheTreeBack() {
        NodePaletteModel model = new NodePaletteModel(registry, 20).expand("Math");

        model.query("add").query("");

        assertEquals("-Math(3)", describe(model.entries()).get(1), "expanded groups survive searching");
    }

    @Test
    void nullAndBlankQueries_areTheEmptyQuery() {
        NodePaletteModel model = new NodePaletteModel(registry, 20).query("add").query(null);
        assertEquals("", model.query());
        assertEquals(5, model.entries().size());

        model.query("   ");
        assertEquals(5, model.entries().size(), "blank means tree, not a search for spaces");
    }

    @Test
    void noMatches_leavesNothingSelected() {
        NodePaletteModel model = new NodePaletteModel(registry, 20).query("zzz");

        assertTrue(model.isEmpty());
        assertEquals(-1, model.selectedIndex());
        assertNull(model.selected());
        assertNull(model.selectedType());
        assertNull(model.activateSelected());
        assertTrue(model.visible().isEmpty());
        model.move(1);
        model.select(3);
        model.expandSelected();
        model.collapseSelected();
        assertNull(model.selected());
    }

    // ---- выбор и клавиши ----

    @Test
    void move_wrapsAroundBothEnds() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        model.move(-1);
        assertEquals("Note", ((NodeEntry) model.selected()).type().title());
        model.move(1);
        assertEquals("Flow", ((CategoryEntry) model.selected()).path());
        model.move(7);
        assertEquals(2, model.selectedIndex());
    }

    @Test
    void select_clampsToTheEntries() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        model.select(99);
        assertEquals(4, model.selectedIndex());
        model.select(-4);
        assertEquals(0, model.selectedIndex());
    }

    @Test
    void activate_togglesGroupsAndReturnsNodes() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        assertNull(model.activateSelected(), "activating a group only toggles it");
        assertTrue(((CategoryEntry) model.selected()).expanded());
        model.move(1);
        assertEquals("branch", model.activateSelected().id());
        assertEquals("branch", model.selectedType().id());
    }

    @Test
    void expandSelected_andCollapseSelected_followTreeNavigation() {
        NodePaletteModel model = new NodePaletteModel(registry, 20).expand("Math").expand("Math/Vector");
        model.select(describe(model.entries()).indexOf("    .Dot"));

        model.collapseSelected();
        assertEquals("Math/Vector", ((CategoryEntry) model.selected()).path(), "a node steps up to its group");
        model.collapseSelected();
        assertFalse(((CategoryEntry) model.selected()).expanded(), "an open group collapses first");
        model.collapseSelected();
        assertEquals("Math", ((CategoryEntry) model.selected()).path(), "a closed nested group steps up to its parent");
        model.collapseSelected();
        assertFalse(((CategoryEntry) model.selected()).expanded());
        model.expandSelected();
        assertTrue(((CategoryEntry) model.selected()).expanded());
    }

    @Test
    void collapseSelected_onAFullyExpandedList_onlyStepsUp() {
        NodePaletteModel model = new NodePaletteModel(registry, 20).expandAll(true);
        model.select(describe(model.entries()).indexOf("  -Vector(2)"));

        model.collapseSelected();

        assertEquals("Math", ((CategoryEntry) model.selected()).path());
        assertTrue(((CategoryEntry) model.selected()).expanded(), "expand-all lists never hide nodes");
    }

    // ---- окно ----

    @Test
    void window_followsTheSelectionBothWays() {
        NodeTypeRegistry many = new NodeTypeRegistry();
        for (int i = 0; i < 25; i++) {
            many.register(type("t" + i, "Node " + i, ""));
        }
        NodePaletteModel model = new NodePaletteModel(many, 10);

        assertEquals(10, model.visible().size());
        assertEquals(0, model.windowStart());
        for (int i = 0; i < 12; i++) model.move(1);
        assertEquals(12, model.selectedIndex());
        assertEquals(3, model.windowStart(), "window scrolled just enough to show the selection");
        model.select(1);
        assertEquals(1, model.windowStart());
        model.move(-1);
        model.move(-1);
        assertEquals(24, model.selectedIndex(), "wrapped to the last entry");
        assertEquals(15, model.windowStart());
        assertEquals(10, model.visible().size());
    }

    @Test
    void scroll_movesTheWindowWithoutMovingTheSelection() {
        NodeTypeRegistry many = new NodeTypeRegistry();
        for (int i = 0; i < 25; i++) {
            many.register(type("t" + i, "Node " + i, ""));
        }
        NodePaletteModel model = new NodePaletteModel(many, 10);

        model.scroll(4);
        assertEquals(4, model.windowStart());
        assertEquals(0, model.selectedIndex());
        model.scroll(100);
        assertEquals(15, model.windowStart(), "clamped so the window stays full");
        model.scroll(-100);
        assertEquals(0, model.windowStart());
    }

    @Test
    void window_neverExceedsShortLists() {
        NodePaletteModel model = new NodePaletteModel(registry, 20);

        model.move(3);
        model.scroll(5);

        assertEquals(0, model.windowStart());
        assertEquals(5, model.visible().size());
    }

    @Test
    void maxVisible_isAtLeastOne() {
        NodePaletteModel model = new NodePaletteModel(registry, 0);

        assertEquals(1, model.maxVisible());
        assertEquals(1, model.visible().size());
    }
}
