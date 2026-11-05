package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Переименование региона и правка его комментария: F2, меню региона, редактор и история.
 */
class NodeGraphRegionEditTest {
    private OverlayLayer layer;
    private NodeGraph graph;
    private NodeGraphRegion region;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph().selectionMode(NodeGraphSelectionMode.MULTIPLE);
        region = graph.addRegion("r", "Setup", 100.0f, 100.0f, 400.0f, 250.0f);
        layer = new OverlayLayer();
        layer.content(graph);
        layer.applyQueuedMutations();
        layer.setUiContextInternal(new DefaultUIContext());
        layout();
    }

    private void layout() {
        layer.measure(new LayoutContext(1000.0f, 800.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 1000.0f, 800.0f));
    }

    private boolean key(int code, int modifiers) {
        KeyPressedEvent event = new KeyPressedEvent(graph, code, 0, modifiers);
        graph.handle(event);
        return event.isCancelled();
    }

    private RegionEditor openEditor() {
        layer.applyQueuedMutations();
        for (var child : layer.children()) {
            if (child instanceof RegionEditor editor && editor.opened()) return editor;
        }
        return null;
    }

    private static boolean press(RegionEditor editor, int code, int modifiers) {
        KeyPressedEvent event = new KeyPressedEvent(editor, code, 0, modifiers);
        editor.handle(event);
        return event.isCancelled();
    }

    // ---- модель ----

    @Test
    void renameRegion_isOneUndoStepAndAnnouncesTheEdit() {
        List<String> kinds = new ArrayList<>();
        graph.onRegionChanged(event -> kinds.add(event.kind().name()));

        assertTrue(graph.renameRegion(region, "Boot"));

        assertEquals("Boot", region.title());
        assertEquals(List.of("EDITED"), kinds);
        assertEquals("Rename region", graph.history().undoName());
        graph.undo();
        assertEquals("Setup", region.title());
        graph.redo();
        assertEquals("Boot", region.title());
    }

    @Test
    void renameRegion_toTheSameTitleDoesNothing() {
        assertFalse(graph.renameRegion(region, "Setup"));

        assertFalse(graph.history().canUndo());
    }

    @Test
    void commentRegion_setsAndClearsWithUndo() {
        assertTrue(graph.commentRegion(region, "Spawns the NPC"));
        assertEquals("Spawns the NPC", region.comment());
        assertEquals("Edit region comment", graph.history().undoName());

        assertTrue(graph.commentRegion(region, ""));
        assertEquals("", region.comment());

        graph.undo();
        assertEquals("Spawns the NPC", region.comment());
        graph.undo();
        assertEquals("", region.comment());
    }

    @Test
    void editingAForeignRegion_isRejected() {
        NodeGraphRegion foreign = new NodeGraphRegion("x", "X", 0, 0, 100, 100);

        assertFalse(graph.renameRegion(foreign, "Y"));
        assertFalse(graph.commentRegion(foreign, "Y"));
        assertFalse(graph.openRegionEditor(foreign, RegionEditor.Mode.TITLE));
    }

    // ---- F2 и редактор названия ----

    @Test
    void f2_opensTheTitleEditorForTheSelectedRegionWithItsTitle() {
        graph.selectRegion(region);

        assertTrue(key(KeyCodes.F2, 0));

        RegionEditor editor = openEditor();
        assertNotNull(editor);
        assertEquals(RegionEditor.Mode.TITLE, editor.mode());
        assertEquals("Setup", editor.text());
        assertInstanceOf(TextField.class, editor.field());
    }

    @Test
    void f2_withoutASelectedRegionDoesNothing() {
        assertFalse(key(KeyCodes.F2, 0));

        assertEquals(null, openEditor());
    }

    @Test
    void theEditorTakesFocusOnTheField() {
        graph.selectRegion(region);
        key(KeyCodes.F2, 0);

        layout();

        assertTrue(((TextInput) openEditor().field()).focused(), "typing goes straight into the field");
    }

    @Test
    void enter_appliesTheNewTitleAndClosesTheEditor() {
        graph.selectRegion(region);
        key(KeyCodes.F2, 0);
        RegionEditor editor = openEditor();
        editor.text("Boot sequence");

        assertTrue(press(editor, KeyCodes.ENTER, 0));

        assertEquals("Boot sequence", region.title());
        assertFalse(editor.opened());
        assertEquals(null, openEditor());
        assertEquals("Rename region", graph.history().undoName());
    }

    @Test
    void escape_cancelsWithoutChangingTheTitle() {
        graph.selectRegion(region);
        key(KeyCodes.F2, 0);
        RegionEditor editor = openEditor();
        editor.text("Never applied");

        assertTrue(press(editor, KeyCodes.ESCAPE, 0));

        assertEquals("Setup", region.title());
        assertFalse(editor.opened());
        assertFalse(graph.history().canUndo());
    }

    @Test
    void clickingOutside_appliesWhatWasTyped() {
        graph.selectRegion(region);
        key(KeyCodes.F2, 0);
        RegionEditor editor = openEditor();
        editor.text("Typed");

        editor.dismiss();

        assertEquals("Typed", region.title());
    }

    @Test
    void anOutsideClickThroughTheOverlayLayer_dismissesAndAppliesTheEditor() {
        graph.selectRegion(region);
        key(KeyCodes.F2, 0);
        layout();
        RegionEditor editor = openEditor();
        editor.text("Via layer");

        layer.handle(new PointerPressedEvent(layer, layer, EventPhase.CAPTURE, 900.0f, 700.0f, 900.0f, 700.0f, 1,
                PointerButton.PRIMARY));

        assertFalse(editor.opened());
        assertEquals("Via layer", region.title());
    }

    @Test
    void emptyTitle_isAllowed() {
        graph.selectRegion(region);
        key(KeyCodes.F2, 0);
        RegionEditor editor = openEditor();
        editor.text("");

        press(editor, KeyCodes.ENTER, 0);

        assertEquals("", region.title());
    }

    // ---- редактор комментария ----

    @Test
    void commentEditor_enterAddsALineAndCtrlEnterApplies() {
        region.comment("First");
        RegionEditor.Mode mode = RegionEditor.Mode.COMMENT;
        assertTrue(graph.openRegionEditor(region, mode));
        RegionEditor editor = openEditor();
        assertInstanceOf(TextArea.class, editor.field());
        assertEquals("First", editor.text());
        editor.text("First\nSecond");

        assertFalse(press(editor, KeyCodes.ENTER, 0), "plain Enter belongs to the text area");
        assertTrue(editor.opened());
        assertEquals("First", region.comment());

        assertTrue(press(editor, KeyCodes.ENTER, KeyModifiers.CONTROL));

        assertEquals("First\nSecond", region.comment());
        assertFalse(editor.opened());
        assertEquals("Edit region comment", graph.history().undoName());
    }

    @Test
    void commentEditor_escapeCancels() {
        graph.openRegionEditor(region, RegionEditor.Mode.COMMENT);
        RegionEditor editor = openEditor();
        editor.text("lost");

        press(editor, KeyCodes.ESCAPE, 0);

        assertEquals("", region.comment());
    }

    // ---- меню региона ----

    private ContextMenu openMenu() {
        layer.applyQueuedMutations();
        for (var child : layer.children()) {
            if (child instanceof ContextMenu menu && menu.opened()) return menu;
        }
        throw new AssertionError("no menu is open");
    }

    private static List<String> texts(ContextMenu menu) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < menu.itemCount(); i++) texts.add(menu.itemButton(i).text());
        return texts;
    }

    private void rightClickRegionTitle() {
        graph.handle(new PointerPressedEvent(graph, 250.0f, 110.0f, 250.0f, 110.0f, 1, PointerButton.SECONDARY));
    }

    @Test
    void regionMenu_hasRenameAndCommentBesideGroupAndDelete() {
        rightClickRegionTitle();

        assertEquals(List.of("Undo", "Redo", "Rename", "Comment...", "Group", "Delete"), texts(openMenu()));
    }

    @Test
    void regionMenuRename_opensTheTitleEditorForThatRegion() {
        rightClickRegionTitle();

        openMenu().itemButton(2).click();

        RegionEditor editor = openEditor();
        assertNotNull(editor);
        assertEquals(RegionEditor.Mode.TITLE, editor.mode());
        assertEquals("Setup", editor.text());
        editor.text("From menu");
        press(editor, KeyCodes.ENTER, 0);
        assertEquals("From menu", region.title());
    }

    @Test
    void regionMenuComment_opensTheCommentEditor() {
        region.comment("old");
        rightClickRegionTitle();

        openMenu().itemButton(3).click();

        RegionEditor editor = openEditor();
        assertNotNull(editor);
        assertEquals(RegionEditor.Mode.COMMENT, editor.mode());
        assertEquals("old", editor.text());
    }

    @Test
    void renamedTitleAndNewComment_showUpInTheSnapshot() {
        graph.renameRegion(region, "Boot");
        graph.commentRegion(region, "Note");

        NodeGraphSnapshot snapshot = graph.snapshot();

        assertEquals("Boot", snapshot.regions().get(0).title());
        assertEquals("Note", snapshot.regions().get(0).comment());
    }
}
