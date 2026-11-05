package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Строка состояния, панель инструментов и общие элементы окна редактора. */
class EditorChromeTest {
    private static final String XAML = "<VBox id=\"root\"><Label id=\"title\" text=\"Hello\"/></VBox>";

    private XamlEditorDocument document;
    private EditorSession session;
    private EditorShell shell;

    @BeforeEach
    void open() {
        session = EditorSession.open(DocumentSource.memory("doc", "doc.xaml", XAML));
        document = new XamlEditorDocument("doc", "doc.xaml", session);
        shell = UiEditor.shell(document);
    }

    private static Widget find(Widget widget, String id) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        if (id.equals(widget.id())) return widget;
        for (Widget child : widget.children()) {
            Widget found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private String label(String id) {
        return ((Label) find(shell.root(), id)).text();
    }

    @Test
    void theStatusBarFollowsTheSelectionAndTheDocument() {
        assertEquals("Nothing selected", label("status.selection"));
        assertEquals("No problems", label("status.problems"));
        assertEquals("Saved", label("status.document"));

        session.select(session.document().root().children().get(0));
        assertEquals("root  ›  title", label("status.selection"));

        session.replaceText("<VBox><Label");
        assertTrue(label("status.problems").contains("problem"), label("status.problems"));
        assertNotEquals("No problems", label("status.problems"));
    }

    @Test
    void theToolbarRunsDocumentCommands() {
        ToolButton undo = (ToolButton) find(shell.root(), "toolbar.edit.undo");
        assertNotNull(undo);
        session.apply(dev.sixik.unigui.xaml.edit.Edits.setProperty(session.document().root().children().get(0), "text", "Bye"));
        assertTrue(document.canUndo());
        undo.click();
        assertTrue(session.text().contains("Hello"), session.text());
    }

    @Test
    void theLayoutSwitchMarksTheCurrentLayout() {
        ToolButton design = (ToolButton) find(shell.root(), "toolbar.layout." + UiEditor.DESIGN_LAYOUT);
        ToolButton code = (ToolButton) find(shell.root(), "toolbar.layout." + UiEditor.CODE_LAYOUT);
        assertTrue(design.active());
        assertFalse(code.active());

        code.click();
        assertEquals(UiEditor.CODE_LAYOUT, shell.currentLayout());
        assertTrue(code.active());
        assertFalse(design.active());
    }

    @Test
    void widgetTypesGetTheirOwnIconsAndUnknownTypesAShared() {
        assertSame(EditorIcons.forWidget("VBox"), EditorIcons.forWidget("vbox"));
        assertNotSame(EditorIcons.forWidget("VBox"), EditorIcons.forWidget("HBox"));
        assertNotSame(EditorIcons.forWidget("Button"), EditorIcons.forWidget("Label"));
        assertSame(EditorIcons.forWidget("Mystery"), EditorIcons.forWidget("Unheard"));
        assertSame(EditorIcons.forWidget("Mystery"), EditorIcons.forWidget(null));
    }

    @Test
    void aTypeHasTheSameColorOnPinsAndContractEntries() {
        assertNotEquals(EditorStyles.pinColor("bool"), EditorStyles.pinColor("string"));
        assertEquals(EditorStyles.pinColor("float"), EditorStyles.pinColor("double"));
        assertEquals(EditorStyles.pinColor("whatever"), EditorStyles.pinColor(null));
    }
}
