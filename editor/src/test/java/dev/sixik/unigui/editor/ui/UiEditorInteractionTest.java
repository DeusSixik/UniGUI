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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private DefaultUIContext uiContext;

    @BeforeEach
    void setUp() {
        session = EditorSession.open(DocumentSource.memory("doc", "Doc", SOURCE));
        document = new XamlEditorDocument("doc", "Doc", session);
        shell = UiEditor.shell(document);
        uiContext = new DefaultUIContext();
        ((dev.sixik.unigui.impl.widget.WidgetBase) shell.root()).setUiContextInternal(uiContext); // как делает экран: виджеты знают контекст, захват указателя работает
        input = new InputHandle(shell.root(), uiContext);
        layout();
    }

    private void layout() {
        applyAll(shell.root());
        shell.root().measure(new LayoutContext(WIDTH, HEIGHT));
        shell.root().arrange(new MutableRect(0.0f, 0.0f, WIDTH, HEIGHT));
        applyAll(shell.root());
        // Рамка экрана вписывается в панель при отрисовке; в тесте отрисовки нет, поэтому вписываем явно.
        shell.panel(UiEditor.VIEWPORT).ifPresent(panel -> ((ViewportPanel) panel).applyView());
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
        ViewportPanel viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        Widget previewRoot = viewport.previewRoot();
        for (Widget up = widget; up != null && previewRoot != null; up = up.parent()) {
            if (up == previewRoot) {
                bounds = viewport.onScreen(bounds); // виджет предпросмотра лежит под масштабом рамки
                break;
            }
        }
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
        ViewportPanel viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        RectView bounds = viewport.onScreen(preview.layoutBounds());
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

    /** Строка свойства в инспекторе; группы раскрываются по одной, пока строка не окажется в видимой части окна. */
    private Label visibleRowLabel(String name) {
        Widget content = shell.dock().findPane(UiEditor.INSPECTOR).content();
        List<dev.sixik.unigui.widgets.interaction.Button> headers = all(content,
                dev.sixik.unigui.widgets.interaction.Button.class).stream()
                .filter(button -> button.id().startsWith("inspector.group.")).toList();
        headers.forEach(header -> header.click()); // свернуть всё
        for (dev.sixik.unigui.widgets.interaction.Button header : headers) {
            header.click();
            layout();
            for (HBox row : all(content, HBox.class)) {
                List<Label> labels = all(row, Label.class);
                if (labels.isEmpty() || !name.equals(labels.get(0).text())) continue;
                RectView bounds = labels.get(0).layoutBounds();
                if (bounds.height() > 0.0f && bounds.y() >= 0.0f && bounds.y() + bounds.height() < HEIGHT) {
                    return labels.get(0);
                }
            }
            header.click();
        }
        throw new AssertionError("row '" + name + "' never became visible");
    }

    @Test
    void theRowContextMenuReturnsAPropertyToItsDefault() {
        session.select(node("title"));
        applyAll(shell.dock().findPane(UiEditor.INSPECTOR).content());
        comboFor("Enabled").selectedIndex(1);
        assertEquals("true", node("title").attributeText("enabled", ""));

        Label label = visibleRowLabel("Enabled");
        RectView bounds = label.layoutBounds();
        float x = bounds.x() + bounds.width() / 2.0f;
        float y = bounds.y() + bounds.height() / 2.0f;
        input.pointerMoved(x, y, 0);
        input.pointerPressed(x, y, 0, PointerButton.SECONDARY);
        input.pointerReleased(x, y, 0, PointerButton.SECONDARY);

        for (dev.sixik.unigui.widgets.feedback.ContextMenu menu : all(shell.overlay(),
                dev.sixik.unigui.widgets.feedback.ContextMenu.class)) {
            for (Widget entry : menu.itemWidgets()) {
                if (entry instanceof dev.sixik.unigui.widgets.interaction.Button button
                        && button.text().startsWith("Clear")) button.click();
            }
        }
        assertTrue(node("title").attributeText("enabled").isEmpty(), "the attribute is gone");
    }

    @Test
    void inspectorGroupsCollapseAndExpand() {
        session.select(node("title"));
        Widget content = shell.dock().findPane(UiEditor.INSPECTOR).content();
        applyAll(content);
        List<dev.sixik.unigui.widgets.interaction.Button> headers = all(content,
                dev.sixik.unigui.widgets.interaction.Button.class).stream()
                .filter(button -> button.id().startsWith("inspector.group.")).toList();
        assertTrue(headers.size() >= 2, "the properties are split into groups");
        dev.sixik.unigui.widgets.interaction.Button first = headers.get(0);
        assertTrue(first.text().startsWith("▾"));
        first.click();
        assertTrue(first.text().startsWith("▸"), first.text());
        first.click();
        assertTrue(first.text().startsWith("▾"), first.text());
    }

    @Test
    void hoveringARowInTheHierarchyHighlightsItsWidgetInThePreview() {
        applyAll(shell.dock().findPane(UiEditor.HIERARCHY).content());
        layout();
        dev.sixik.unigui.widgets.interaction.Button row = all(shell.dock().findPane(UiEditor.HIERARCHY).content(),
                dev.sixik.unigui.widgets.interaction.Button.class).stream()
                .filter(button -> button.text().contains("#title")).findFirst().orElseThrow();
        RectView bounds = row.layoutBounds();
        input.pointerMoved(bounds.x() + bounds.width() / 2.0f, bounds.y() + bounds.height() / 2.0f, 0);

        ViewportPanel viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        assertSame(find(viewport.previewRoot(), "title"), viewport.hoveredWidget());

        input.pointerMoved(1.0f, 1.0f, 0);
        assertNull(viewport.hoveredWidget(), "leaving the row clears the highlight");
    }

    // ------------------------------------------------------------------ перетаскивание

    private void drag(Widget from, float toX, float toY) {
        RectView start = from.layoutBounds();
        float x = start.x() + start.width() / 2.0f;
        float y = start.y() + start.height() / 2.0f;
        input.pointerMoved(x, y, 0);
        input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        input.pointerMoved(x + 12.0f, y + 12.0f, 0);
        input.pointerMoved((x + toX) / 2.0f, (y + toY) / 2.0f, 0);
        input.pointerMoved(toX, toY, 0);
        input.pointerReleased(toX, toY, 0, PointerButton.PRIMARY);
    }

    private dev.sixik.unigui.widgets.interaction.Button hierarchyRow(String text) {
        applyAll(shell.dock().findPane(UiEditor.HIERARCHY).content());
        return all(shell.dock().findPane(UiEditor.HIERARCHY).content(), dev.sixik.unigui.widgets.interaction.Button.class)
                .stream().filter(button -> button.text().contains(text)).findFirst().orElseThrow();
    }

    private Widget paletteTile(String name) {
        applyAll(shell.dock().findPane(UiEditor.PALETTE).content());
        // Список длинный: поиск оставляет одну плитку в видимой части панели.
        all(shell.dock().findPane(UiEditor.PALETTE).content(), dev.sixik.unigui.widgets.interaction.TextField.class)
                .get(0).text(name);
        applyAll(shell.dock().findPane(UiEditor.PALETTE).content());
        layout();
        return all(shell.dock().findPane(UiEditor.PALETTE).content(), dev.sixik.unigui.widgets.interaction.Button.class)
                .stream().filter(button -> name.equals(button.text())).findFirst().orElseThrow();
    }

    @Test
    void draggingATypeFromThePaletteOntoTheViewportAddsItToTheWidgetUnderThePointer() {
        Widget tile = paletteTile("Slider");
        Widget preview = ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        RectView target = ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).onScreen(preview.layoutBounds());
        drag(tile, target.x() + target.width() - 3.0f, target.y() + target.height() - 3.0f);
        UiNode root = session.document().root();
        assertEquals(3, root.children().size(), "the slider joined the container under the pointer");
        assertEquals("Slider", root.children().get(2).type().local());
    }

    @Test
    void draggingATypeOntoAHierarchyRowAddsItThere() {
        Widget tile = paletteTile("Slider");
        layout();
        RectView row = hierarchyRow("#root").layoutBounds();
        drag(tile, row.x() + row.width() / 2.0f, row.y() + row.height() / 2.0f);
        assertEquals("Slider", session.document().root().children().get(2).type().local());
    }

    @Test
    void draggingARowOntoAnotherReordersTheDocument() {
        layout();
        RectView target = hierarchyRow("#ok").layoutBounds();
        drag(hierarchyRow("#title"), target.x() + target.width() / 2.0f, target.y() + target.height() / 2.0f);
        UiNode root = session.document().root();
        assertEquals(List.of("ok", "title"), root.children().stream().map(n -> n.attributeText("id", "")).toList(),
                "the label moved after the button");
    }

    // ------------------------------------------------------------------ холст

    @Test
    void thePreviewIsLaidOutInsideAFixedScreenFrame() {
        ViewportPanel viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        layout();
        Widget root = viewport.previewRoot();
        assertEquals(1280.0f, root.layoutBounds().width(), 0.5f, "the default screen is 1280x720");
        assertEquals(720.0f, root.layoutBounds().height(), 0.5f);

        viewport.screen("854x480");
        layout();
        assertEquals(854.0f, viewport.previewRoot().layoutBounds().width(), 0.5f);
        viewport.screen("Free");
        layout();
        assertTrue(viewport.previewRoot().layoutBounds().width() < 854.0f, "Free fills the panel instead");
    }

    @Test
    void clickingAZoomedPreviewStillSelectsTheWidgetUnderThePointer() {
        ViewportPanel viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        layout();
        viewport.zoom(0.5f);
        viewport.applyView();

        Widget ok = find(viewport.previewRoot(), "ok");
        RectView onScreen = viewport.onScreen(ok.layoutBounds());
        assertEquals(ok.layoutBounds().width() * 0.5f, onScreen.width(), 0.5f, "the widget is drawn at half size");

        float x = onScreen.x() + onScreen.width() / 2.0f;
        float y = onScreen.y() + onScreen.height() / 2.0f;
        input.pointerMoved(x, y, 0);
        input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        assertSame(node("ok"), session.selected().orElseThrow());
    }

    @Test
    void playModeLetsTheLiveInterfaceReceiveClicks() {
        ViewportPanel viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        layout();
        boolean[] pressed = {false};
        ((dev.sixik.unigui.widgets.interaction.Button) find(viewport.previewRoot(), "ok")).onClick(event -> pressed[0] = true);

        viewport.playing(true);
        viewport.applyView();
        assertTrue(viewport.playing());
        click(find(viewport.previewRoot(), "ok"));
        assertTrue(pressed[0], "in Play mode the button gets the click");
        assertTrue(session.selected().isEmpty(), "and nothing is selected");

        viewport.playing(false);
        pressed[0] = false;
        click(find(viewport.previewRoot(), "ok"));
        assertFalse(pressed[0], "back in design mode the click selects instead");
    }
}
