package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditHistory;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.edit.Hierarchy;
import dev.sixik.unigui.xaml.edit.Inspector;
import dev.sixik.unigui.xaml.edit.NodePath;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.ValueKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EditorSessionTest {
    private static String idOf(UiNode node) {
        return node.attributeText("id", "");
    }

    // ------------------------------------------------------------------ сессия: контракты прежнего редактора

    @Test
    void parseFailurePreservesTheLastValidDocument() {
        EditorSession session = EditorSession.create("<VBox><Button id=\"ok\" text=\"OK\" /></VBox>");
        session.select(NodePath.of(0));

        assertFalse(session.replaceText("<VBox><Button></VBox>"), "malformed text is rejected");
        assertEquals("<VBox><Button></VBox>", session.text(), "the raw buffer is kept for the code view");
        assertEquals("VBox", session.document().root().type().local());
        assertEquals("ok", idOf(session.document().root().children().get(0)), "the last valid tree survives");
        assertEquals(NodePath.of(0), session.selectedPath().orElseThrow());
        assertEquals(1, session.diagnostics(EditorSession.Channel.PARSE).size());
        assertTrue(session.diagnostics(EditorSession.Channel.PARSE).get(0).hasLocation());
        assertTrue(session.diagnostics(EditorSession.Channel.VALIDATION).isEmpty());
        assertTrue(session.dirty(), "broken text still counts as an unsaved change");

        assertTrue(session.replaceText("<VBox><Button id=\"fixed\"/></VBox>"), "fixing the text recovers the document");
        assertTrue(session.diagnostics(EditorSession.Channel.PARSE).isEmpty());
    }

    @Test
    void selectingTheSameNodeTwiceEmitsOnce() {
        EditorSession session = EditorSession.create("<VBox><Button id=\"ok\" /></VBox>");
        int[] changes = {0};
        try (var ignored = session.onChanged(change -> {
            if (change.kind() == SessionChange.Kind.SELECTION_CHANGED) changes[0]++;
        })) {
            session.select(NodePath.of(0));
            session.select(NodePath.of(0));
            assertEquals(1, changes[0]);
            session.select((UiNode) null);
            session.select((UiNode) null);
            assertEquals(2, changes[0]);
        }
    }

    @Test
    void undoRedoKeepsTheSelectionOnTheSameNode() {
        EditorSession session = EditorSession.create(
                "<VBox><Label id=\"first\" /><Button id=\"selected\" /></VBox>");
        session.select(NodePath.of(1));
        UiNode selectedNode = session.selected().orElseThrow();

        assertTrue(session.apply(Edits.addChild(session.document().root(), 0, UiNode.of("TextWidget").set("id", "inserted"))));
        assertEquals(1, session.undoCount());
        assertEquals(0, session.redoCount());
        assertEquals(NodePath.of(2), session.selectedPath().orElseThrow(), "selection follows its node");
        assertEquals(selectedNode, session.selected().orElseThrow());
        assertTrue(session.text().contains("inserted"));

        assertTrue(session.undo());
        assertEquals(NodePath.of(1), session.selectedPath().orElseThrow());
        assertFalse(session.text().contains("inserted"));
        assertTrue(session.redo());
        assertEquals(NodePath.of(2), session.selectedPath().orElseThrow());
        assertFalse(session.undo() && session.undo(), "only one step existed");
        assertFalse(session.redo() && session.redo() && session.redo());
    }

    @Test
    void editsThatBreakTheChildRulesAreRejectedWithoutChangingTheDocument() {
        EditorSession session = EditorSession.create("<VBox><Button id=\"button\" /></VBox>");
        UiNode button = session.document().root().children().get(0);

        assertFalse(session.apply(Edits.addChild(button, Integer.MAX_VALUE, UiNode.of("Label").set("id", "rejected"))));
        assertTrue(button.children().isEmpty());
        assertEquals(0, session.undoCount());
        assertTrue(session.diagnostics(EditorSession.Channel.EDIT).stream()
                .anyMatch(d -> d.message().contains("Widget Button cannot contain child Label")));

        assertTrue(session.apply(Edits.addChild(session.document().root(), Integer.MAX_VALUE,
                UiNode.of("Label").set("id", "inserted"))));
        assertEquals(2, session.document().root().children().size());
        assertEquals(1, session.undoCount());
        assertTrue(session.diagnostics(EditorSession.Channel.EDIT).isEmpty(), "an accepted edit clears the rejection");
    }

    @Test
    void saveRevertAndEventsFollowTheSource() {
        DocumentSource source = DocumentSource.memory("demo", "Demo", "<Label id=\"saved\" text=\"Saved\" />");
        EditorSession session = EditorSession.open(source);
        List<SessionChange.Kind> events = new ArrayList<>();
        var subscription = session.onChanged(change -> events.add(change.kind()));

        assertFalse(session.dirty());
        session.mode(EditorSession.Mode.CODE);
        assertEquals(EditorSession.Mode.CODE, session.mode());
        assertTrue(events.contains(SessionChange.Kind.MODE_CHANGED));

        assertTrue(session.apply(Edits.setProperty(session.document().root(), "id", "edited")));
        assertTrue(session.dirty());
        assertTrue(events.contains(SessionChange.Kind.DOCUMENT_CHANGED));
        assertTrue(events.contains(SessionChange.Kind.UNDO_STACK_CHANGED));

        assertTrue(session.save());
        assertFalse(session.dirty());
        assertTrue(source.readText().contains("id=\"edited\""), source.readText());
        assertTrue(events.contains(SessionChange.Kind.SAVED));

        assertTrue(session.replaceText("<Label id=\"unsaved\" text=\"Unsaved\" />"));
        assertTrue(session.dirty());
        assertEquals("unsaved", idOf(session.document().root()));

        assertTrue(session.revert());
        assertFalse(session.dirty());
        assertEquals("edited", idOf(session.document().root()));
        assertTrue(events.contains(SessionChange.Kind.REVERTED));

        subscription.close();
        int count = events.size();
        session.mode(EditorSession.Mode.SPLIT);
        assertEquals(count, events.size(), "a closed subscription receives nothing");
    }

    @Test
    void sessionsWithoutASourceCannotSaveButCanMarkClean() {
        EditorSession session = EditorSession.create("<Label/>");
        session.apply(Edits.setProperty(session.document().root(), "text", "x"));
        assertTrue(session.dirty());
        assertFalse(session.save());
        session.markClean();
        assertFalse(session.dirty());
    }

    @Test
    void creatingFromBrokenTextFailsClearly() {
        assertThrows(XamlLoadException.class, () -> EditorSession.create("<VBox>"));
    }

    @Test
    void replacingTextKeepsTheSelectionByPathAndDropsStaleHistory() {
        EditorSession session = EditorSession.create("<VBox><Label id=\"a\"/><Label id=\"b\"/></VBox>");
        session.apply(Edits.setProperty(session.document().root().children().get(0), "text", "x"));
        session.select(NodePath.of(1));
        assertEquals(1, session.undoCount());

        assertTrue(session.replaceText("<VBox><Label id=\"a\"/><Label id=\"b\" text=\"typed\"/><Label id=\"c\"/></VBox>"));
        assertEquals("b", idOf(session.selected().orElseThrow()), "selection is re-resolved by path in the new tree");
        assertEquals(0, session.undoCount(), "old edits refer to nodes that no longer exist");
    }

    @Test
    void validationFindsProblemsAfterEveryEditAndClearsThemOnUndo() {
        EditorSession session = EditorSession.create("<VBox><Label id=\"a\"/></VBox>");
        assertTrue(session.diagnostics(EditorSession.Channel.VALIDATION).isEmpty());
        UiNode label = session.document().root().children().get(0);

        assertTrue(session.apply(Edits.setProperty(label, "color", "not-a-color")), "editor edits may be invalid");
        assertEquals(1, session.diagnostics(EditorSession.Channel.VALIDATION).size());
        assertTrue(session.diagnostics(EditorSession.Channel.VALIDATION).get(0).message().contains("color"));

        assertTrue(session.undo());
        assertTrue(session.diagnostics(EditorSession.Channel.VALIDATION).isEmpty());
    }

    @Test
    void deletingTheSelectedNodeClearsTheSelection() {
        EditorSession session = EditorSession.create("<VBox><Label id=\"a\"/><Label id=\"b\"/></VBox>");
        session.select(NodePath.of(0));
        assertTrue(session.apply(Edits.remove(session.document(), session.selected().orElseThrow())));
        assertTrue(session.selected().isEmpty());
        assertTrue(session.undo());
        assertEquals(1, session.document().root().children().size() - 1);
        assertEquals("a", idOf(session.document().root().children().get(0)));
    }

    // ------------------------------------------------------------------ правки

    @Test
    void continuousChangesToOnePropertyAreOneUndoStep() {
        EditorSession session = EditorSession.create("<Label id=\"a\"/>");
        UiNode label = session.document().root();
        for (int i = 1; i <= 10; i++) assertTrue(session.apply(Edits.setProperty(label, "opacity", i / 10.0 + "")));
        assertEquals(1, session.undoCount(), "dragging a slider must not flood the undo stack");
        assertEquals("1.0", label.attributeText("opacity", ""));

        session.apply(Edits.setProperty(label, "text", "other"));
        assertEquals(2, session.undoCount(), "a different property starts a new step");

        assertTrue(session.undo());
        assertTrue(session.undo());
        assertTrue(label.attributeText("opacity").isEmpty(), "undo restores the state before the first change");
    }

    @Test
    void removingAPropertyAndUndoingRestoresItsExactPosition() {
        EditorSession session = EditorSession.create("<VBox id=\"v\" spacing=\"4\" padding=\"2\"/>");
        String before = XamlFormat.INSTANCE.write(session.document());
        UiNode root = session.document().root();

        assertTrue(session.apply(Edits.removeProperty(root, "spacing")));
        assertTrue(root.attributeText("spacing").isEmpty());
        assertTrue(session.undo());
        assertEquals(before, session.text(), "attribute order is part of the document");
        assertEquals(List.of("id", "spacing", "padding"),
                root.properties().stream().map(p -> p.name().local()).toList());
    }

    @Test
    void movingNodesKeepsOrderAndUndoesExactly() {
        EditorSession session = EditorSession.create(
                "<VBox><HBox id=\"left\"><Label id=\"x\"/></HBox><HBox id=\"right\"/><Label id=\"y\"/></VBox>");
        String before = XamlFormat.INSTANCE.write(session.document());
        UiNode root = session.document().root();
        UiNode x = root.children().get(0).children().get(0);
        UiNode right = root.children().get(1);

        assertTrue(session.apply(Edits.move(session.document(), x, right, 0)));
        assertTrue(root.children().get(0).children().isEmpty());
        assertEquals("x", idOf(right.children().get(0)));
        assertTrue(session.undo());
        assertEquals(before, session.text());

        assertFalse(session.apply(Edits.move(session.document(), root.children().get(0), x, 0)),
                "a node cannot move into its own descendant");
        assertTrue(session.diagnostics(EditorSession.Channel.EDIT).get(0).message().contains("itself or its descendant"));
        assertFalse(session.apply(Edits.move(session.document(), x, root.children().get(2), 0)),
                "Label does not accept children");
        assertFalse(session.apply(Edits.move(session.document(), root, right, 0)), "the root cannot move");
        assertFalse(session.apply(Edits.remove(session.document(), root)), "the root cannot be removed");
    }

    @Test
    void removalRestoresCommentsAndTextAroundTheNode() {
        EditorSession session = EditorSession.create("<VBox><!--one--><Label id=\"a\"/><!--two--><Label id=\"b\"/></VBox>");
        String before = XamlFormat.INSTANCE.write(session.document());
        assertTrue(session.apply(Edits.remove(session.document(), session.document().root().children().get(0))));
        assertFalse(session.text().contains("id=\"a\""));
        assertTrue(session.undo());
        assertEquals(before, session.text());
    }

    @Test
    void groupedEditsApplyAndUndoAsOneStepAndAreCheckedAsAWhole() {
        EditorSession session = EditorSession.create("<VBox id=\"v\"><Label id=\"a\"/></VBox>");
        UiNode root = session.document().root();
        UiNode label = root.children().get(0);
        String before = XamlFormat.INSTANCE.write(session.document());

        Edit rename = Edits.group("Rename and recolour", List.of(
                Edits.setProperty(root, "id", "renamed"),
                Edits.setProperty(label, "text", "hello"),
                Edits.setType(label, UiName.of("TextBlock"))));
        assertTrue(session.apply(rename));
        assertEquals(1, session.undoCount());
        assertEquals("Rename and recolour", session.undoLabel().orElseThrow());
        assertEquals("TextBlock", label.type().local());
        assertTrue(session.undo());
        assertEquals(before, session.text());

        Edit poisoned = Edits.group("Poisoned", List.of(
                Edits.setProperty(root, "id", "never"),
                Edits.addChild(label, 0, UiNode.of("Label"))));
        assertFalse(session.apply(poisoned), "one bad member rejects the whole group");
        assertEquals("v", idOf(root), "nothing from a rejected group is applied");
    }

    @Test
    void anyEditSequenceKeepsTheTextConsistentWithTheTree() {
        EditorSession session = EditorSession.create("<VBox><HBox id=\"row\"/></VBox>");
        UiNode root = session.document().root();
        UiNode row = root.children().get(0);
        session.apply(Edits.addChild(row, 0, UiNode.of("Label").set("id", "l1").set("text", "a & <b>")));
        session.apply(Edits.addChild(row, 1, UiNode.of("Button").set("id", "b1")));
        session.apply(Edits.setProperty(row, "spacing", "6"));
        session.apply(Edits.setProperty(row, "padding", "{StaticResource Pad}"));
        session.apply(Edits.move(session.document(), row.children().get(1), root, 0));
        session.apply(Edits.removeProperty(row, "spacing"));

        for (int step = 0; step < 8; step++) {
            UiDocument reparsed = XamlFormat.INSTANCE.read(session.text());
            assertTrue(session.document().sameStructure(reparsed), "after step " + step + ":\n" + session.text());
            if (!session.undo()) break;
        }
        assertEquals(0, session.undoCount());
        while (session.redo()) {
            UiDocument reparsed = XamlFormat.INSTANCE.read(session.text());
            assertTrue(session.document().sameStructure(reparsed));
        }
        assertEquals(6, session.undoCount());
    }

    @Test
    void historyIsBoundedAndClearable() {
        EditHistory history = new EditHistory(3);
        UiNode node = UiNode.of("Label");
        for (int i = 0; i < 10; i++) history.execute(Edits.setProperty(node, "p" + i, "v"));
        assertEquals(3, history.undoCount(), "the oldest steps are forgotten first");
        history.undo();
        assertEquals(1, history.redoCount());
        history.execute(Edits.setProperty(node, "fresh", "v"));
        assertEquals(0, history.redoCount(), "a new edit discards the redo stack");
        history.clear();
        assertEquals(0, history.undoCount());
        assertTrue(history.undo().isEmpty() && history.redo().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new EditHistory(0));
    }

    // ------------------------------------------------------------------ иерархия

    @Test
    void hierarchyListsEveryNodeAndEachPathResolvesBackToItsNode() {
        UiDocument document = XamlFormat.INSTANCE.read("""
                <VBox id="root">
                    <Label id="title"/>
                    <ScrollView id="scroll">
                        <ScrollView.Content><VBox id="inner"><Label id="deep"/></VBox></ScrollView.Content>
                    </ScrollView>
                    <Mystery id="ghost"/>
                </VBox>
                """);
        List<Hierarchy.Row> rows = Hierarchy.rows(document, Xaml.defaultSchema());
        assertEquals(List.of("root", "title", "scroll", "inner", "deep", "ghost"),
                rows.stream().map(Hierarchy.Row::id).toList(),
                "parents come before their children; property-element content follows its owner");
        for (Hierarchy.Row row : rows) {
            assertSame(row.node(), row.path().resolve(document).orElseThrow(), row.label());
            assertEquals(row.path(), NodePath.find(document, row.node()).orElseThrow());
        }

        Hierarchy.Row scroll = byId(rows, "scroll");
        assertEquals(1, scroll.depth());
        assertEquals(1, scroll.childCount());
        assertFalse(byId(rows, "inner").structureEditable(), "content of property elements cannot be rearranged");
        assertFalse(byId(rows, "deep").structureEditable());
        assertTrue(byId(rows, "title").structureEditable());
        assertFalse(byId(rows, "ghost").known());
        assertEquals("Label #title", byId(rows, "title").label());
        assertEquals(List.of("deep"), Hierarchy.filter(rows, "DEE").stream().map(Hierarchy.Row::id).toList());
        assertEquals(rows.size(), Hierarchy.filter(rows, "  ").size());
        assertEquals(document.root(), Hierarchy.parentOf(document, byId(rows, "title").node()).orElseThrow());
        assertTrue(Hierarchy.parentOf(document, document.root()).isEmpty());
        assertTrue(Hierarchy.parentOf(document, byId(rows, "inner").node()).isEmpty());
    }

    private static Hierarchy.Row byId(List<Hierarchy.Row> rows, String id) {
        return rows.stream().filter(row -> row.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void nodesInsidePropertyElementsStillAcceptPropertyEdits() {
        EditorSession session = EditorSession.create(
                "<ScrollView><ScrollView.Content><VBox id=\"inner\"/></ScrollView.Content></ScrollView>");
        UiNode inner = Hierarchy.rows(session.document(), session.schema()).stream()
                .filter(row -> row.id().equals("inner")).findFirst().orElseThrow().node();
        assertTrue(session.apply(Edits.setProperty(inner, "spacing", "9")));
        assertTrue(session.text().contains("spacing=\"9\""), session.text());
        assertFalse(session.apply(Edits.remove(session.document(), inner)));
        assertTrue(session.diagnostics(EditorSession.Channel.EDIT).get(0).message().contains("property elements"));
    }

    // ------------------------------------------------------------------ инспектор

    @Test
    void inspectorDescribesKnownTypes() {
        UiNode label = XamlFormat.INSTANCE.read(
                "<Label id=\"t\" text=\"Hi\" width=\"oops\" future=\"1\" opacity=\"{Binding hud.alpha}\"/>").root();
        Inspector.Inspection inspection = Inspector.inspect(Xaml.defaultSchema(), label);

        assertTrue(inspection.known());
        assertEquals("Label", inspection.typeName());
        assertEquals("Display", inspection.category());
        Inspector.PropertyView text = inspection.property("text");
        assertTrue(text.set());
        assertEquals("Hi", text.valueText());
        assertEquals("Content", text.category());
        assertNull(text.problem());

        Inspector.PropertyView width = inspection.property("width");
        assertEquals(ValueKind.SIZE_VALUE, width.kind());
        assertNotNull(width.problem(), "an invalid value is reported next to the field");
        assertEquals("oops", width.valueText());

        Inspector.PropertyView opacity = inspection.property("opacity");
        assertEquals("{Binding hud.alpha}", opacity.valueText());
        assertTrue(opacity.bindable());
        assertNull(opacity.problem(), "binding expressions are not parsed by the converter");

        assertEquals("t", inspection.property("id").valueText());
        assertFalse(inspection.property("height").set());
        assertEquals("auto", inspection.property("height").defaultValue());
        assertEquals(List.of("future"), inspection.unknown().stream().map(p -> p.name().local()).toList());
        assertEquals("Common", inspection.byCategory().keySet().iterator().next(), "categories are in the usual order");
        assertTrue(new ArrayList<>(inspection.byCategory().keySet()).indexOf("Layout")
                < new ArrayList<>(inspection.byCategory().keySet()).indexOf("Behavior"));
    }

    @Test
    void inspectorExposesSlotsAndHandlesUnknownTypes() {
        UiNode scroll = XamlFormat.INSTANCE.read(
                "<ScrollView><ScrollView.Content><VBox/></ScrollView.Content></ScrollView>").root();
        Inspector.Inspection inspection = Inspector.inspect(Xaml.defaultSchema(), scroll);
        Inspector.SlotView content = inspection.slots().stream().filter(s -> s.name().equals("Content")).findFirst().orElseThrow();
        assertTrue(content.single());
        assertEquals(1, content.count());
        assertTrue(inspection.unknown().isEmpty(), "a slot is not an unknown property");

        UiNode ghost = XamlFormat.INSTANCE.read("<Mystery a=\"1\"/>").root();
        Inspector.Inspection unknown = Inspector.inspect(Xaml.defaultSchema(), ghost);
        assertFalse(unknown.known());
        assertTrue(unknown.properties().isEmpty());
        assertEquals(1, unknown.unknown().size());
    }

    // ------------------------------------------------------------------ источники

    @Test
    void fileSourcesReadAndWriteUtf8() throws IOException {
        Path file = Files.createTempFile("unigui-session-", ".xaml");
        try {
            Files.writeString(file, "<Label id=\"файл\" text=\"Привет\"/>");
            EditorSession session = EditorSession.open(DocumentSource.file(file));
            assertEquals("файл", idOf(session.document().root()));
            assertTrue(session.apply(Edits.setProperty(session.document().root(), "text", "Пока")));
            assertTrue(session.save());
            assertTrue(Files.readString(file).contains("Пока"));
            assertEquals(file.getFileName().toString(), DocumentSource.file(file).title());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
