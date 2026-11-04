package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextField;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Взаимодействие через настоящий путь ввода ({@link InputHandle}): щелчки по предпросмотру и горячие клавиши
 * доходят до редактора так же, как в игре.
 */
final class UiEditorInteractionTest {
    private static final float WIDTH = 1280.0f;
    private static final float HEIGHT = 720.0f;
    private static final String SOURCE =
            "<VBox id=\"root\" padding=\"10\" spacing=\"6\"><Label id=\"title\" text=\"Hello\"/>"
                    + "<Button id=\"ok\" text=\"OK\"/></VBox>";

    private EditorSession session;
    private XamlEditorDocument document;
    private EditorShell shell;
    private InputHandle input;

    @BeforeEach
    void setUp() {
        session = EditorSession.open(DocumentSource.memory("doc", "Doc", SOURCE));
        document = new XamlEditorDocument("doc", "Doc", session);
        shell = UiEditor.shell(document);
        input = new InputHandle(shell.root(), new DefaultUIContext());
        layout();
    }

    private void layout() {
        applyAll(shell.root());
        shell.root().measure(new LayoutContext(WIDTH, HEIGHT));
        shell.root().arrange(new MutableRect(0.0f, 0.0f, WIDTH, HEIGHT));
        applyAll(shell.root());
    }

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) applyAll(child);
    }

    private static Widget find(Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (Widget child : widget.children()) {
            Widget found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private UiNode node(String id) {
        for (UiNode child : session.document().root().children()) if (id.equals(child.attributeText("id", ""))) return child;
        return "root".equals(session.document().root().attributeText("id", "")) && "root".equals(id)
                ? session.document().root() : null;
    }

    private void click(Widget widget) {
        RectView bounds = widget.layoutBounds();
        float x = bounds.x() + bounds.width() / 2.0f;
        float y = bounds.y() + bounds.height() / 2.0f;
        input.pointerMoved(x, y, 0);
        input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
    }

    /** Щелчок по предпросмотру даёт окну фокус клавиатуры; выбор после него задаётся заново. */
    private void focusEditor() {
        Widget preview = ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        click(find(preview, "title"));
    }

    private static <T extends Widget> List<T> all(Widget root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T extends Widget> void collect(Widget widget, Class<T> type, List<T> into) {
        if (type.isInstance(widget)) into.add(type.cast(widget));
        for (Widget child : widget.children()) collect(child, type, into);
    }

    // ------------------------------------------------------------------ выбор щелчком

    @Test
    void clickingAWidgetInThePreviewSelectsItsNode() {
        Widget preview = ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        assertNotNull(preview);
        click(find(preview, "ok"));
        assertSame(node("ok"), session.selected().orElseThrow());

        click(find(preview, "title"));
        assertSame(node("title"), session.selected().orElseThrow(), "the deepest widget under the pointer wins");
    }

    @Test
    void clickingEmptySpaceOfAContainerSelectsTheContainer() {
        Widget preview = ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        RectView bounds = preview.layoutBounds();
        // правый нижний угол контейнера: ни метки, ни кнопки там нет
        float x = bounds.x() + bounds.width() - 2.0f;
        float y = bounds.y() + bounds.height() - 2.0f;
        input.pointerMoved(x, y, 0);
        input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        assertSame(session.document().root(), session.selected().orElseThrow());
    }

    @Test
    void thePreviewDoesNotReactToClicksItself() {
        Widget preview = ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        boolean[] pressed = {false};
        ((dev.sixik.unigui.widgets.interaction.Button) find(preview, "ok")).onClick(event -> pressed[0] = true);
        click(find(preview, "ok"));
        assertFalse(pressed[0], "a design-time click selects instead of pressing the button");
    }

    // ------------------------------------------------------------------ горячие клавиши

    @Test
    void deleteKeyRemovesTheSelectedWidget() {
        focusEditor();
        session.select(node("ok"));
        layout();
        input.keyPressed(KeyCodes.DELETE, 0, 0);
        assertEquals(1, session.document().root().children().size());
        assertSame(session.document().root(), session.selected().orElseThrow());
    }

    @Test
    void ctrlZUndoesAndCtrlYRedoesWhenNoTextFieldHasFocus() {
        focusEditor();
        session.select(node("title"));
        layout();
        input.keyPressed(KeyCodes.D, 0, KeyModifiers.CONTROL); // Duplicate
        assertEquals(3, session.document().root().children().size());

        input.keyPressed(KeyCodes.Z, 0, KeyModifiers.CONTROL);
        assertEquals(2, session.document().root().children().size());
        input.keyPressed(KeyCodes.Y, 0, KeyModifiers.CONTROL);
        assertEquals(3, session.document().root().children().size());
    }

    @Test
    void ctrlSSavesAndShortcutsStaySilentWithoutAMatchingCommand() {
        focusEditor();
        session.select(node("title"));
        layout();
        input.keyPressed(KeyCodes.D, 0, KeyModifiers.CONTROL);
        assertTrue(document.dirty());
        input.keyPressed(KeyCodes.S, 0, KeyModifiers.CONTROL);
        assertFalse(document.dirty());

        int before = session.document().root().children().size();
        input.keyPressed(KeyCodes.Q, 0, KeyModifiers.CONTROL);
        assertEquals(before, session.document().root().children().size());
    }

    @Test
    void shellHandlesKeysDirectlyAndRespectsTextFocus() {
        session.select(node("ok"));
        TextField typing = new TextField("abc");
        assertFalse(shell.handleKey(KeyCodes.DELETE, 0, typing), "Delete belongs to the text field");
        assertEquals(2, session.document().root().children().size());

        assertTrue(shell.handleKey(KeyCodes.DELETE, 0, null));
        assertEquals(1, session.document().root().children().size());
    }

    // ------------------------------------------------------------------ списки значений

    private ComboBox comboFor(String displayName) {
        for (HBox row : all(shell.dock().findPane(UiEditor.INSPECTOR).content(), HBox.class)) {
            List<Label> labels = all(row, Label.class);
            if (!labels.isEmpty() && displayName.equals(labels.get(0).text())) {
                List<ComboBox> combos = all(row, ComboBox.class);
                if (!combos.isEmpty()) return combos.get(0);
            }
        }
        throw new AssertionError("no combo row '" + displayName + "'");
    }

    @Test
    void booleanPropertiesAreChosenFromAList() {
        session.select(node("title"));
        applyAll(shell.dock().findPane(UiEditor.INSPECTOR).content());
        ComboBox enabled = comboFor("Enabled");
        assertEquals(List.of("(default)", "true", "false"), enabled.items());
        assertEquals(0, enabled.selectedIndex(), "an unset property shows the default entry");

        enabled.selectedIndex(2);
        assertEquals("false", node("title").attributeText("enabled", ""));
        assertTrue(document.canUndo());

        document.undo();
        assertEquals(0, comboFor("Enabled").selectedIndex(), "undo restores the unset state");
    }

    @Test
    void theResetButtonReturnsAPropertyToItsDefault() {
        session.select(node("title"));
        applyAll(shell.dock().findPane(UiEditor.INSPECTOR).content());
        comboFor("Enabled").selectedIndex(1);
        assertEquals("true", node("title").attributeText("enabled", ""));

        for (HBox row : all(shell.dock().findPane(UiEditor.INSPECTOR).content(), HBox.class)) {
            List<Label> labels = all(row, Label.class);
            if (!labels.isEmpty() && "Enabled".equals(labels.get(0).text())) {
                all(row, dev.sixik.unigui.widgets.interaction.Button.class).stream()
                        .filter(button -> "×".equals(button.text())).findFirst().orElseThrow().click();
            }
        }
        assertTrue(node("title").attributeText("enabled").isEmpty(), "the attribute is gone");
    }
}
