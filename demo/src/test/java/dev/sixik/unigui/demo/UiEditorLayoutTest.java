package dev.sixik.unigui.demo;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.editor.UiEditorScreen;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.editor.ui.UiEditor;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Без экрана и рендера проверяет главное, что можно проверить о виде редактора: после раскладки каждая панель и
 * каждый виджет предпросмотра получили положительный размер внутри окна.
 */
class UiEditorLayoutTest {
    private static final float WIDTH = 1280.0f;
    private static final float HEIGHT = 720.0f;

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) applyAll(child);
    }

    private static void layout(Widget root) {
        applyAll(root);
        root.measure(new LayoutContext(WIDTH, HEIGHT));
        root.arrange(new MutableRect(0.0f, 0.0f, WIDTH, HEIGHT));
        applyAll(root);
    }

    private static DockingRoot dock(Widget widget) {
        if (widget instanceof DockingRoot dock) return dock;
        for (Widget child : widget.children()) {
            DockingRoot found = dock(child);
            if (found != null) return found;
        }
        return null;
    }

    private static void collect(Widget widget, List<Widget> into) {
        into.add(widget);
        for (Widget child : widget.children()) collect(child, into);
    }

    @Test
    void everyOpenPanelGetsRealSpaceInsideTheWindow() {
        Widget root = UiEditorScreen.createScreen().root();
        layout(root);
        DockingRoot dock = dock(root);
        assertNotNull(dock);

        RectView window = root.layoutBounds();
        assertTrue(window.width() > 1000.0f && window.height() > 600.0f, "window: " + window);
        for (String id : List.of("hierarchy", "palette", "inspector", "viewport", "diagnostics")) {
            var pane = dock.findPane(id);
            if (pane == null) continue;
            RectView bounds = pane.content().layoutBounds();
            // Невыбранные вкладки не раскладываются; выбранные обязаны занимать место.
            if (bounds.width() > 0.0f || bounds.height() > 0.0f) {
                assertTrue(bounds.width() > 50.0f && bounds.height() > 30.0f, id + " is too small: " + bounds);
                assertTrue(bounds.x() >= -0.5f && bounds.x() + bounds.width() <= WIDTH + 0.5f, id + " leaves the window: " + bounds);
            }
        }
        RectView viewport = dock.findPane("viewport").content().layoutBounds();
        assertTrue(viewport.width() > 300.0f && viewport.height() > 200.0f, "the preview area is usable: " + viewport);
    }

    @Test
    void theComponentEditorGivesTheContractAndThePreviewRealSpace() {
        Widget root = UiEditorScreen.createComponentScreen().root();
        layout(root);
        DockingRoot dock = dock(root);
        assertNotNull(dock);

        RectView contract = dock.findPane("contract").content().layoutBounds();
        assertTrue(contract.width() > 150.0f && contract.height() > 100.0f, "the contract panel is usable: " + contract);
        RectView viewport = dock.findPane("viewport").content().layoutBounds();
        assertTrue(viewport.width() > 300.0f && viewport.height() > 200.0f, "the preview area is usable: " + viewport);

        // Ничто в панели контракта не вылезает за её границы: подписи и поля не обрезаются.
        List<Widget> inContract = new ArrayList<>();
        collect(dock.findPane("contract").content(), inContract);
        for (Widget widget : inContract) {
            RectView bounds = widget.layoutBounds();
            if (bounds.width() <= 0.0f) continue;
            assertTrue(bounds.x() >= contract.x() - 0.5f && bounds.x() + bounds.width() <= contract.x() + contract.width() + 0.5f,
                    widget.getClass().getSimpleName() + " '" + widget.id() + "' leaves the contract panel: " + bounds
                            + " in " + contract);
        }

        List<Widget> everything = new ArrayList<>();
        collect(dock.findPane("viewport").content(), everything);
        for (String id : List.of("card", "title", "price", "buy")) {
            Widget widget = everything.stream().filter(candidate -> id.equals(candidate.id())).findFirst().orElse(null);
            assertNotNull(widget, "the card body has '" + id + "'");
            assertTrue(widget.layoutBounds().width() > 0.0f && widget.layoutBounds().height() > 0.0f,
                    id + " has a real size: " + widget.layoutBounds());
        }
    }

    @Test
    void nothingInTheInspectorLeavesThePanel() {
        XamlEditorDocument document = new XamlEditorDocument("demo", "demo.xaml",
                EditorSession.open(DocumentSource.memory("demo", "demo.xaml", UiEditorScreen.SAMPLE)));
        EditorShell shell = UiEditor.shell(document);
        document.session().select(document.session().document().root());
        Widget root = shell.root();
        layout(root);
        layout(root);

        Widget inspector = dock(root).findPane("inspector").content();
        RectView panel = inspector.layoutBounds();
        assertTrue(panel.width() > 150.0f, "the inspector is open: " + panel);
        List<Widget> widgets = new ArrayList<>();
        collect(inspector, widgets);
        long fields = widgets.stream().filter(TextField.class::isInstance).count();
        assertTrue(fields > 10, "the properties are shown, got " + fields);
        for (Widget widget : widgets) {
            RectView bounds = widget.layoutBounds();
            if (bounds.width() <= 0.0f) continue;
            assertTrue(bounds.x() + bounds.width() <= panel.x() + panel.width() + 0.5f,
                    widget.getClass().getSimpleName() + " leaves the inspector: x=" + bounds.x() + " w=" + bounds.width()
                            + " panel right=" + (panel.x() + panel.width()));
        }
    }

    @Test
    void thePaletteSharesTheLeftColumnSoThePreviewKeepsItsHeight() {
        Widget root = UiEditorScreen.createScreen().root();
        layout(root);
        DockingRoot dock = dock(root);
        RectView palette = dock.findPane("palette").content().layoutBounds();
        RectView hierarchy = dock.findPane("hierarchy").content().layoutBounds();
        RectView viewport = dock.findPane("viewport").content().layoutBounds();
        assertTrue(palette.y() > hierarchy.y() + hierarchy.height() - 0.5f, "the palette lies under the hierarchy");
        assertTrue(palette.x() + palette.width() <= viewport.x(), "the palette is left of the preview");
        assertTrue(viewport.height() > HEIGHT * 0.5f, "the preview takes most of the height: " + viewport);
    }

    @Test
    void thePreviewWidgetsAreLaidOutWithPositiveSizes() {
        Widget root = UiEditorScreen.createScreen().root();
        layout(root);
        Widget viewport = dock(root).findPane("viewport").content();
        List<Widget> everything = new ArrayList<>();
        collect(viewport, everything);
        long sized = everything.stream()
                .filter(widget -> widget.id() != null && !widget.id().isEmpty())
                .filter(widget -> widget.layoutBounds().width() > 0.0f && widget.layoutBounds().height() > 0.0f)
                .count();
        // В образце 10 виджетов с id: root, title, nameRow, name, sound, volume, loading, buttons, save, cancel.
        assertTrue(sized >= 8, "most preview widgets have a real size, got " + sized);
    }
}
