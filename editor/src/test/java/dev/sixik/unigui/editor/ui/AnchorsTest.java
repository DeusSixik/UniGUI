package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnchorsTest {
    private static final String SOURCE = """
            <StackPanel id="root">
              <Button id="b" text="Go" width="80" height="24" margin="4"/>
            </StackPanel>""";

    private EditorSession session;
    private XamlEditorDocument document;
    private EditorShell shell;

    @BeforeEach
    void setUp() {
        session = EditorSession.open(DocumentSource.memory("a", "A", SOURCE));
        document = new XamlEditorDocument("a", "A", session);
        shell = UiEditor.shell(document);
    }

    private UiNode button() {
        return session.document().root().children().get(0);
    }

    private static Button find(Widget widget, String id) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        if (id.equals(widget.id()) && widget instanceof Button found) return found;
        for (Widget child : widget.children()) {
            Button found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void anchorsApplyOnlyInsideAStackPanel() {
        assertTrue(Anchors.supported(session, button()));
        assertFalse(Anchors.supported(session, session.document().root()), "the root has no slot to anchor in");
        EditorSession flow = EditorSession.open(DocumentSource.memory("f", "F",
                "<VBox><Button id=\"b\"/></VBox>"));
        assertFalse(Anchors.supported(flow, flow.document().root().children().get(0)), "a VBox lays children out in a flow");
    }

    @Test
    void thePickerWritesTheAlignmentAndUndoRestoresIt() {
        session.select(button());
        Widget inspector = shell.dock().findPane(UiEditor.INSPECTOR).content();
        Button corner = find(inspector, "anchor.Bottom.Right");
        assertNotNull(corner, "the inspector offers the nine-point picker for a StackPanel child");
        corner.click();
        assertEquals("end", button().attributeText("horizontalAlignment", ""));
        assertEquals("end", button().attributeText("verticalAlignment", ""));

        document.undo();
        document.undo();
        assertTrue(button().attributeText("horizontalAlignment").isEmpty());
    }

    @Test
    void movingChangesOnlyTheMarginAndKeepsTheAnchor() {
        Anchors.anchor(session, button(), "end", "start");
        assertTrue(Anchors.move(session, button(), 10.0f, 6.0f));
        // отступ 4 со всех сторон: от правого края виджет уходит на 10 вправо, от верхнего — на 6 вниз
        assertEquals("10 -6 4 4", button().attributeText("margin", ""));
        assertEquals("end", button().attributeText("horizontalAlignment", ""));
    }

    @Test
    void aDragIsOneStepOfUndo() {
        session.select(button());
        shell.dock().findPane(UiEditor.VIEWPORT).content();
        ViewportPanel viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        viewport.beginMove(100.0f, 100.0f);
        viewport.moveTo(103.0f, 101.0f);
        viewport.moveTo(110.0f, 105.0f);
        viewport.moveTo(120.0f, 112.0f);
        viewport.endMove();
        assertEquals("16 4 4 24", button().attributeText("margin", "").isEmpty() ? "" : button().attributeText("margin", ""));

        document.undo();
        assertEquals("4", button().attributeText("margin", ""), "the whole drag is undone in one step");
    }
}
