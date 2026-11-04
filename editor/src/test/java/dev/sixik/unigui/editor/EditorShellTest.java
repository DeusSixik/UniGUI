package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockNode;
import dev.sixik.unigui.widgets.docking.DockSplitOrientation;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.edit.Edits;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EditorShellTest {
    private static EditorPanel panel(String id, PanelSlot slot, float share) {
        return new EditorPanel() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String title() {
                return id.substring(0, 1).toUpperCase() + id.substring(1);
            }

            @Override
            public Widget create(EditorContext context) {
                return new Label(id);
            }

            @Override
            public PanelSlot slot() {
                return slot;
            }

            @Override
            public float share() {
                return share;
            }
        };
    }

    private static EditorShell.Builder shell() {
        return EditorShell.builder()
                .panel(panel("scene", PanelSlot.CENTER, 0.5f))
                .panel(panel("game", PanelSlot.CENTER, 0.5f))
                .panel(panel("hierarchy", PanelSlot.LEFT, 0.22f))
                .panel(panel("inspector", PanelSlot.RIGHT, 0.30f))
                .panel(panel("project", PanelSlot.BOTTOM, 0.35f))
                .panel(panel("console", PanelSlot.BOTTOM, 0.35f))
                .layout(EditorLayout.named("Default")
                        .left(0.18f, "hierarchy").right(0.28f, "inspector")
                        .bottom(0.35f, "project", "console").center("scene", "game").build());
    }

    private static void collect(Widget widget, Class<?> type, List<Widget> into) {
        if (type.isInstance(widget)) into.add(widget);
        for (Widget child : widget.children()) collect(child, type, into);
    }

    private static List<String> buttonTexts(Widget root) {
        List<Widget> found = new ArrayList<>();
        collect(root, Button.class, found);
        return found.stream().map(widget -> ((Button) widget).text()).toList();
    }

    private static List<String> menuTitles(ContextMenu menu) {
        return menu.itemWidgets().stream().map(widget -> ((Button) widget).text()).toList();
    }

    // ------------------------------------------------------------------ раскладки

    @Test
    void slotLayoutBuildsTheSameTreeAsAHandWrittenOne() {
        EditorLayout layout = EditorLayout.named("Default")
                .left(0.18f, "hierarchy").right(0.28f, "inspector")
                .bottom(0.35f, "project", "console").center("scene", "game").build();

        // Ровно то дерево, которое демо Unity собирало вручную.
        var sceneGame = new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("scene", "game"), "scene", null, null);
        var projectConsole = new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("project", "console"), "project", null, null);
        var center = new DockLayoutSnapshot.Node(DockNode.Kind.SPLIT, DockSplitOrientation.VERTICAL, 0.65f, List.of(), "", sceneGame, projectConsole);
        var inspector = new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("inspector"), "inspector", null, null);
        var centerRight = new DockLayoutSnapshot.Node(DockNode.Kind.SPLIT, DockSplitOrientation.HORIZONTAL, 0.72f, List.of(), "", center, inspector);
        var hierarchy = new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("hierarchy"), "hierarchy", null, null);
        var root = new DockLayoutSnapshot.Node(DockNode.Kind.SPLIT, DockSplitOrientation.HORIZONTAL, 0.18f, List.of(), "", hierarchy, centerRight);

        assertEquals(new DockLayoutSnapshot(root, "scene"), layout.snapshot());
    }

    @Test
    void firstLayoutIsAppliedAndAllItsPanelsAreOpen() {
        EditorShell shell = shell().build();
        assertEquals("Default", shell.currentLayout());
        for (String id : List.of("scene", "game", "hierarchy", "inspector", "project", "console")) {
            assertTrue(shell.isOpen(id), id + " should be open");
        }
    }

    @Test
    void switchingLayoutsReplacesTheArrangement() {
        EditorShell shell = shell()
                .layout(EditorLayout.named("Minimal").center("scene").build())
                .build();
        shell.applyLayout("Minimal");
        assertEquals("Minimal", shell.currentLayout());
        assertTrue(shell.isOpen("scene"));
        assertFalse(shell.isOpen("inspector"));
        shell.applyLayout("Default");
        assertTrue(shell.isOpen("inspector"));
    }

    @Test
    void unknownLayoutsAndPanelsInLayoutsAreRejected() {
        EditorShell shell = shell().build();
        assertThrows(IllegalArgumentException.class, () -> shell.applyLayout("Nope"));
        assertThrows(IllegalArgumentException.class, () -> shell()
                .layout(EditorLayout.named("Broken").center("ghost").build()).build()
                .applyLayout("Broken"));
        assertThrows(IllegalArgumentException.class, () -> shell().panel(panel("scene", PanelSlot.CENTER, 0.5f)));
        assertThrows(IllegalArgumentException.class, () -> shell()
                .layout(EditorLayout.named("Default").center("scene").build()));
    }

    // ------------------------------------------------------------------ панели

    @Test
    void aClosedPanelReopensInItsOwnArea() {
        EditorShell shell = shell().build();
        shell.closePanel("inspector");
        assertFalse(shell.isOpen("inspector"));

        shell.openPanel("inspector");
        assertTrue(shell.isOpen("inspector"));
        // Снова справа от центральной панели с долей 0.30 (у существующей части остаётся 0.70).
        DockLayoutSnapshot snapshot = shell.dock().manager().snapshot();
        assertTrue(contains(snapshot.root(), "inspector"));
        assertEquals("inspector", snapshot.activePaneId());
    }

    private static boolean contains(DockLayoutSnapshot.Node node, String id) {
        if (node == null) return false;
        return node.paneIds().contains(id) || contains(node.first(), id) || contains(node.second(), id);
    }

    @Test
    void panelsOfTheSameAreaShareATab() {
        EditorShell shell = shell().build();
        shell.closePanel("console");
        shell.openPanel("console");
        DockLayoutSnapshot.Node leaf = findLeaf(shell.dock().manager().snapshot().root(), "console");
        assertTrue(leaf.paneIds().contains("project"), "console joins the project tab strip: " + leaf.paneIds());
    }

    private static DockLayoutSnapshot.Node findLeaf(DockLayoutSnapshot.Node node, String id) {
        if (node == null) return null;
        if (node.paneIds().contains(id)) return node;
        DockLayoutSnapshot.Node first = findLeaf(node.first(), id);
        return first != null ? first : findLeaf(node.second(), id);
    }

    @Test
    void openingAnOpenPanelJustSelectsIt() {
        EditorShell shell = shell().build();
        shell.openPanel("game");
        assertEquals("game", shell.dock().manager().snapshot().activePaneId());
        assertThrows(IllegalArgumentException.class, () -> shell.openPanel("ghost"));
    }

    @Test
    void aPanelThatIsNotClosableStaysOpen() {
        EditorPanel pinned = new EditorPanel() {
            @Override
            public String id() {
                return "pinned";
            }

            @Override
            public String title() {
                return "Pinned";
            }

            @Override
            public Widget create(EditorContext context) {
                return new Label("pinned");
            }

            @Override
            public boolean closable() {
                return false;
            }
        };
        EditorShell shell = EditorShell.builder().panel(pinned)
                .layout(EditorLayout.named("L").center("pinned").build()).build();
        shell.closePanel("pinned");
        assertTrue(shell.isOpen("pinned"));
    }

    @Test
    void panelContentIsCreatedOnceWithTheSharedContext() {
        List<EditorContext> seen = new ArrayList<>();
        EditorShell shell = EditorShell.builder()
                .panel(EditorPanel.of("a", "A", PanelSlot.CENTER, context -> {
                    seen.add(context);
                    return new Label("a");
                }))
                .layout(EditorLayout.named("L").center("a").build()).build();
        shell.applyLayout("L");
        shell.closePanel("a");
        shell.openPanel("a");
        assertEquals(1, seen.size());
        assertTrue(seen.get(0) == shell.context());
    }

    // ------------------------------------------------------------------ меню и команды

    @Test
    void windowMenuListsEveryPanelAndOpensIt() {
        EditorShell shell = shell().build();
        shell.closePanel("hierarchy");

        ContextMenu window = shell.buildMenu("Window");
        assertEquals(List.of("Scene", "Game", "Hierarchy", "Inspector", "Project", "Console"), menuTitles(window));

        shell.context().commands().run("window.open.hierarchy", shell.context());
        assertTrue(shell.isOpen("hierarchy"));
    }

    @Test
    void menuBarShowsDeclaredMenusInOrderEvenWhenEmpty() {
        EditorShell shell = shell().menus("File", "Edit", "Assets").build();
        List<String> texts = buttonTexts(shell.root());
        assertEquals(List.of("File", "Edit", "Assets", "Window"), texts.subList(0, 4));
    }

    @Test
    void clickingAMenuButtonOpensItsMenuInTheOverlay() {
        EditorShell shell = shell().command(EditorCommand.builder("file.hello").title("Hello").menu("File")
                .action(ctx -> { }).build()).build();
        List<Widget> buttons = new ArrayList<>();
        collect(shell.root(), Button.class, buttons);
        Button file = (Button) buttons.stream().filter(button -> "File".equals(((Button) button).text())).findFirst().orElseThrow();

        file.click();
        List<Widget> menus = new ArrayList<>();
        collect(shell.overlay(), ContextMenu.class, menus);
        assertFalse(menus.isEmpty(), "a drop-down menu was added to the overlay");
    }

    @Test
    void menuItemsRunTheirCommandAndCommandsCanBeDisabled() {
        List<String> log = new ArrayList<>();
        boolean[] allowed = {false};
        EditorShell shell = shell()
                .command(EditorCommand.builder("tool.run").title("Run").menu("Tools").order(2)
                        .enabledWhen(ctx -> allowed[0]).action(ctx -> log.add("run")).build())
                .command(EditorCommand.builder("tool.first").title("First").menu("Tools").order(1)
                        .action(ctx -> log.add("first")).build())
                .build();

        assertEquals(List.of("First", "Run"), menuTitles(shell.buildMenu("Tools")), "ordered by order");
        assertFalse(shell.context().commands().run("tool.run", shell.context()));
        assertTrue(log.isEmpty());

        allowed[0] = true;
        assertTrue(shell.context().commands().run("tool.run", shell.context()));
        assertEquals(List.of("run"), log);
        assertFalse(shell.context().commands().run("missing", shell.context()));
    }

    @Test
    void duplicateCommandIdsAreRejected() {
        EditorContext context = new EditorContext();
        context.commands().register(EditorCommand.builder("a").build());
        assertThrows(IllegalArgumentException.class, () -> context.commands().register(EditorCommand.builder("a").build()));
    }

    @Test
    void standardCommandsFollowTheCurrentDocument() {
        EditorShell shell = shell().standardCommands().build();
        EditorContext context = shell.context();
        EditorCommands commands = context.commands();

        assertFalse(commands.find("edit.undo").orElseThrow().enabled(context), "no document, nothing to undo");
        assertFalse(commands.find("file.save").orElseThrow().enabled(context));

        XamlEditorDocument document = EditorContextTest.document("<VBox id=\"a\"/>");
        context.document(document);
        assertFalse(commands.find("edit.undo").orElseThrow().enabled(context));
        assertFalse(commands.find("file.save").orElseThrow().enabled(context), "nothing to save yet");

        document.session().apply(Edits.setProperty(document.session().document().root(), "id", "b"));
        assertTrue(commands.find("edit.undo").orElseThrow().enabled(context));
        assertTrue(commands.find("file.save").orElseThrow().enabled(context));

        assertTrue(commands.run("edit.undo", context));
        assertTrue(commands.find("edit.redo").orElseThrow().enabled(context));
        assertTrue(commands.run("edit.redo", context));
        assertTrue(commands.run("file.save", context));
        assertFalse(document.dirty());
    }

    @Test
    void toolbarAppearsOnlyWhenSomethingIsAddedAndKeepsSectionOrder() {
        EditorShell plain = shell().build();
        List<Widget> separatorsPlain = new ArrayList<>();
        collect(plain.root(), dev.sixik.unigui.widgets.containers.Box.class, separatorsPlain);

        EditorShell withToolbar = shell()
                .toolbar(ToolbarSection.RIGHT, s -> new Button("right"))
                .toolbar(ToolbarSection.LEFT, s -> new Button("left"))
                .toolbar(ToolbarSection.CENTER, s -> new Button("center"))
                .build();
        List<String> texts = buttonTexts(withToolbar.root());
        int left = texts.indexOf("left");
        int center = texts.indexOf("center");
        int right = texts.indexOf("right");
        assertTrue(left >= 0 && left < center && center < right, texts.toString());
        assertFalse(buttonTexts(plain.root()).contains("left"));
    }

    @Test
    void titleAndEdgeWidgetsLandInTheMenuBar() {
        EditorShell shell = shell().title("My editor")
                .menuBarLeading(s -> new Button("logo"))
                .menuBarTrailing(s -> new Button("close"))
                .build();
        List<String> texts = buttonTexts(shell.root());
        assertEquals("logo", texts.get(0));
        assertEquals("close", texts.get(texts.size() - 1).equals("close") ? "close" : texts.get(texts.size() - 1));
        List<Widget> labels = new ArrayList<>();
        collect(shell.root(), Label.class, labels);
        assertTrue(labels.stream().anyMatch(label -> "My editor".equals(((Label) label).text())));
    }
}
