package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.XmlCodeEditor;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
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

final class UiEditorTest {
    private static final String SOURCE =
            "<VBox id=\"root\"><Label id=\"title\" text=\"Hello\"/><Button id=\"ok\" text=\"OK\"/></VBox>";

    private DocumentSource source;
    private EditorSession session;
    private XamlEditorDocument document;
    private EditorShell shell;

    @BeforeEach
    void setUp() {
        source = DocumentSource.memory("doc", "Doc", SOURCE);
        session = EditorSession.open(source);
        document = new XamlEditorDocument("doc", "Doc", session);
        shell = UiEditor.shell(document);
    }

    // ------------------------------------------------------------------ вспомогательное

    private Widget content(String panel) {
        Widget widget = shell.dock().findPane(panel).content();
        applyAll(widget);
        return widget;
    }

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) applyAll(child);
    }

    private static <T extends Widget> List<T> all(Widget root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T extends Widget> void collect(Widget widget, Class<T> type, List<T> into) {
        if (type.isInstance(widget)) into.add(type.cast(widget));
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) collect(child, type, into);
    }

    private List<String> buttonTexts(String panel) {
        return all(content(panel), Button.class).stream().map(Button::text).toList();
    }

    private Button button(String panel, String text) {
        return all(content(panel), Button.class).stream().filter(button -> text.equals(button.text()))
                .findFirst().orElseThrow(() -> new AssertionError("no button '" + text + "' in " + buttonTexts(panel)));
    }

    private UiNode node(String id) {
        return find(session.document().root(), id);
    }

    private static UiNode find(UiNode node, String id) {
        if (id.equals(node.attributeText("id", ""))) return node;
        for (UiNode child : node.children()) {
            UiNode found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    /** Поле инспектора по заголовку свойства. */
    private TextField inspectorField(String displayName) {
        for (HBox row : all(content(UiEditor.INSPECTOR), HBox.class)) {
            List<Label> labels = all(row, Label.class);
            if (!labels.isEmpty() && displayName.equals(labels.get(0).text())) return all(row, TextField.class).get(0);
        }
        throw new AssertionError("no inspector row '" + displayName + "'");
    }

    // ------------------------------------------------------------------ иерархия

    @Test
    void hierarchyListsEveryNodeAndClickingSelects() {
        List<String> rows = buttonTexts(UiEditor.HIERARCHY);
        assertEquals(3, rows.size(), rows.toString());
        assertTrue(rows.get(0).contains("VBox"));
        assertTrue(rows.get(1).startsWith("  "), "children are indented");

        button(UiEditor.HIERARCHY, rows.get(1)).click();
        assertSame(node("title"), session.selected().orElseThrow());
        assertSame(node("title"), shell.context().selection().first(UiNode.class).orElseThrow(),
                "the shared selection mirrors the session");
    }

    @Test
    void hierarchyFollowsStructuralEdits() {
        session.select(node("root"));
        UiEdits.insert(session, session.schema().type("Slider").orElseThrow());
        assertEquals(4, buttonTexts(UiEditor.HIERARCHY).size());
    }

    @Test
    void hierarchyFilterNarrowsTheRows() {
        TextField filter = all(content(UiEditor.HIERARCHY), TextField.class).get(0);
        filter.text("ok");
        assertEquals(1, buttonTexts(UiEditor.HIERARCHY).size());
    }

    // ------------------------------------------------------------------ инспектор

    @Test
    void inspectorShowsTheSelectedNodeProperties() {
        assertTrue(all(content(UiEditor.INSPECTOR), Label.class).stream()
                .anyMatch(label -> "Select a widget".equals(label.text())));

        session.select(node("title"));
        assertEquals("Hello", inspectorField("Text").text());
        assertTrue(all(content(UiEditor.INSPECTOR), Label.class).stream()
                .anyMatch(label -> "Label".equals(label.text())), "the type name heads the panel");
    }

    @Test
    void editingAFieldWritesTheDocumentAndUndoRestoresIt() {
        session.select(node("title"));
        inspectorField("Text").text("Bye");
        assertTrue(session.text().contains("text=\"Bye\""), session.text());
        assertTrue(document.canUndo());

        document.undo();
        assertTrue(session.text().contains("text=\"Hello\""), session.text());
        assertEquals("Hello", inspectorField("Text").text(), "the field follows the undo");
    }

    @Test
    void typingDoesNotRebuildTheFieldBeingEdited() {
        session.select(node("title"));
        TextField before = inspectorField("Text");
        before.text("A");
        before.text("AB");
        assertSame(before, inspectorField("Text"), "the same widget keeps the focus");
        assertTrue(session.text().contains("text=\"AB\""));
    }

    @Test
    void anEmptyFieldRemovesTheAttribute() {
        session.select(node("title"));
        inspectorField("Text").text("");
        assertTrue(node("title").attributeText("text").isEmpty(), session.text());
        assertTrue(document.canUndo());
    }

    @Test
    void anInvalidValueIsShownAndDoesNotBreakTheDocument() {
        session.select(node("title"));
        inspectorField("Opacity").text("lots");
        List<Label> problems = all(content(UiEditor.INSPECTOR), Label.class).stream()
                .filter(label -> !label.text().isEmpty() && label.text().toLowerCase().contains("lots")).toList();
        assertFalse(problems.isEmpty(), "the problem text appears under the field");
    }

    // ------------------------------------------------------------------ палитра

    @Test
    void paletteAddsAWidgetIntoTheSelectedContainer() {
        session.select(node("root"));
        button(UiEditor.PALETTE, "  Slider").click();
        UiNode root = session.document().root();
        assertEquals(3, root.children().size());
        assertEquals("Slider", root.children().get(2).type().local());
        assertSame(root.children().get(2), session.selected().orElseThrow(), "the new widget becomes selected");
    }

    @Test
    void paletteFallsBackToTheNearestContainerWhenTheSelectionTakesNoChildren() {
        session.select(node("title"));
        button(UiEditor.PALETTE, "  Slider").click();
        UiNode root = session.document().root();
        assertEquals(3, root.children().size(), "added next to the label, inside the VBox");
        assertEquals(0, node("title").children().size());
    }

    @Test
    void paletteSearchFiltersTypes() {
        TextField search = all(content(UiEditor.PALETTE), TextField.class).get(0);
        search.text("slid");
        List<String> texts = buttonTexts(UiEditor.PALETTE);
        assertEquals(List.of("  Slider"), texts);
    }

    // ------------------------------------------------------------------ предпросмотр

    private ViewportPanel viewport() {
        return (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
    }

    @Test
    void previewShowsTheDocumentAndFollowsEdits() {
        content(UiEditor.VIEWPORT);
        Widget root = viewport().previewRoot();
        assertNotNull(root);
        assertEquals("Hello", ((Label) find(root, "title")).text());

        session.select(node("title"));
        inspectorField("Text").text("Changed");
        assertEquals("Changed", ((Label) find(viewport().previewRoot(), "title")).text());
    }

    private static Widget find(Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (Widget child : widget.children()) {
            Widget found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void theSelectedNodeMapsToItsPreviewWidget() {
        content(UiEditor.VIEWPORT);
        assertNull(viewport().selectedWidget());
        session.select(node("ok"));
        assertSame(find(viewport().previewRoot(), "ok"), viewport().selectedWidget());
    }

    @Test
    void aBrokenDocumentKeepsTheLastGoodPreview() {
        content(UiEditor.VIEWPORT);
        Widget before = viewport().previewRoot();
        session.replaceText("<VBox><NoSuchWidget/></VBox>");
        // Документ разобрался, но тип неизвестен: предпросмотр не меняется, причина показана.
        assertSame(before, viewport().previewRoot());
        assertTrue(all(content(UiEditor.VIEWPORT), Label.class).stream()
                .anyMatch(label -> label.text().startsWith("Preview:")));
    }

    // ------------------------------------------------------------------ текст и диагностика

    @Test
    void codePanelMirrorsTheDocumentBothWays() {
        XmlCodeEditor editor = all(content(UiEditor.CODE), XmlCodeEditor.class).get(0);
        assertEquals(session.text(), editor.text());

        session.select(node("title"));
        inspectorField("Text").text("FromInspector");
        assertEquals(session.text(), editor.text(), "an inspector edit reaches the text");

        editor.text("<VBox id=\"root\"><Label id=\"title\" text=\"FromCode\"/></VBox>");
        assertEquals("FromCode", ((Label) find(viewport().previewRoot(), "title")).text(),
                "typing valid XAML updates the preview");
        assertEquals(2, buttonTexts(UiEditor.HIERARCHY).size());
    }

    @Test
    void invalidTextIsReportedAndLeavesTheDocumentAlone() {
        XmlCodeEditor editor = all(content(UiEditor.CODE), XmlCodeEditor.class).get(0);
        String good = session.text();
        editor.text("<VBox><Label");
        assertTrue(session.document().root().children().size() == 2, "the parsed document is unchanged");
        assertFalse(session.allDiagnostics().isEmpty());
        assertEquals(good.contains("Hello"), true);

        List<Label> lines = all(content(UiEditor.DIAGNOSTICS), Label.class);
        assertFalse(lines.isEmpty());
        assertFalse(lines.stream().anyMatch(label -> "No problems".equals(label.text())));
    }

    @Test
    void diagnosticsStartClean() {
        assertTrue(all(content(UiEditor.DIAGNOSTICS), Label.class).stream()
                .anyMatch(label -> "No problems".equals(label.text())));
    }

    // ------------------------------------------------------------------ команды

    @Test
    void editCommandsOperateOnTheSelection() {
        var commands = shell.context().commands();
        assertFalse(commands.find("edit.delete").orElseThrow().enabled(shell.context()), "nothing selected");

        session.select(node("title"));
        assertTrue(commands.run("edit.moveDown", shell.context()));
        assertEquals("ok", session.document().root().children().get(0).attributeText("id", ""));
        assertTrue(commands.run("edit.moveUp", shell.context()));
        assertEquals("title", session.document().root().children().get(0).attributeText("id", ""));
        commands.run("edit.moveUp", shell.context());
        assertEquals("title", session.document().root().children().get(0).attributeText("id", ""), "already first");

        assertTrue(commands.run("edit.duplicate", shell.context()));
        assertEquals(3, session.document().root().children().size());

        UiNode copy = session.selected().orElseThrow();
        assertTrue(commands.run("edit.delete", shell.context()));
        assertFalse(session.document().root().children().contains(copy));
        assertSame(session.document().root(), session.selected().orElseThrow(), "selection moves to the parent");

        assertFalse(commands.find("edit.delete").orElseThrow().enabled(shell.context()), "the root cannot be deleted");
    }

    @Test
    void saveWritesTheSourceAndUndoIsAvailableThroughTheMenuCommands() {
        var commands = shell.context().commands();
        assertFalse(commands.find("file.save").orElseThrow().enabled(shell.context()));

        session.select(node("title"));
        inspectorField("Text").text("Saved");
        assertTrue(commands.run("file.save", shell.context()));
        assertTrue(source.readText().contains("text=\"Saved\""));

        assertTrue(commands.run("edit.undo", shell.context()));
        assertTrue(session.text().contains("Hello"));
    }

    @Test
    void layoutsSwitchTheCenterTab() {
        assertEquals(UiEditor.DESIGN_LAYOUT, shell.currentLayout());
        assertEquals(UiEditor.VIEWPORT, shell.dock().manager().snapshot().activePaneId());
        shell.applyLayout(UiEditor.CODE_LAYOUT);
        assertEquals(UiEditor.CODE, shell.dock().manager().snapshot().activePaneId());
        assertTrue(shell.isOpen(UiEditor.INSPECTOR));
        assertFalse(shell.isOpen(UiEditor.HIERARCHY));
        shell.openPanel(UiEditor.HIERARCHY);
        assertTrue(shell.isOpen(UiEditor.HIERARCHY));
    }

    @Test
    void switchingDocumentsRefreshesThePanels() {
        XamlEditorDocument other = new XamlEditorDocument("other", "Other",
                EditorSession.open(DocumentSource.memory("other", "Other", "<HBox id=\"solo\"/>")));
        shell.context().document(other);
        assertEquals(1, buttonTexts(UiEditor.HIERARCHY).size());
        assertEquals("solo", find(viewport().previewRoot(), "solo").id());

        shell.context().document(null);
        assertEquals(0, buttonTexts(UiEditor.HIERARCHY).size());
        assertNull(viewport().previewRoot());
    }
}
