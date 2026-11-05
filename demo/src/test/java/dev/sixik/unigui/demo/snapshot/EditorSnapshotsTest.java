package dev.sixik.unigui.demo.snapshot;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.editor.UiEditorScreen;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.editor.ui.ComponentEditor;
import dev.sixik.unigui.editor.ui.UiEditor;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Снимает редакторы демо в {@code demo/build/snapshots}: без игры видно, как выглядит каждый экран после правок стиля.
 * Размер окна соответствует снимкам из игры: примерно 1036×550 логических пикселей при масштабе около двух.
 */
class EditorSnapshotsTest {
    private static final float WIDTH = 1036.0f;
    private static final float HEIGHT = 550.0f;
    private static final float SCALE = 2.0f;
    private static final Path OUT = Path.of("build", "snapshots");

    private static DockingRoot dock(Widget widget) {
        if (widget instanceof DockingRoot dock) return dock;
        for (Widget child : widget.children()) {
            DockingRoot found = dock(child);
            if (found != null) return found;
        }
        return null;
    }

    private static void snap(Widget root, String name, String... panes) throws IOException {
        SnapshotRenderer renderer = new SnapshotRenderer(root, WIDTH, HEIGHT, SCALE);
        renderer.settle(3);
        for (String pane : panes) dock(root).selectPane(pane);
        renderer.settle(3);
        Path file = OUT.resolve(name + ".png");
        renderer.write(file);
        assertTrue(Files.size(file) > 0, "the snapshot was written");
    }

    @Test
    void screenEditor() throws IOException {
        snap(UiEditorScreen.createScreen().root(), "screen");
    }

    private static UiNode find(UiNode node, String id) {
        if (id.equals(node.attributeText("id", ""))) return node;
        for (UiNode child : node.children()) {
            UiNode found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static void snapSelected(String text, String nodeId, String name, java.util.function.Function<XamlEditorDocument, EditorShell> shell)
            throws IOException {
        XamlEditorDocument document = new XamlEditorDocument("demo", "demo.xaml",
                EditorSession.open(DocumentSource.memory("demo", "demo.xaml", text)));
        EditorShell editor = shell.apply(document);
        document.session().select(find(document.session().document().root(), nodeId));
        snap(editor.root(), name);
    }

    @Test
    void screenSelection() throws IOException {
        snapSelected(UiEditorScreen.SAMPLE, "volume", "screen-selected", UiEditor::shell);
    }

    @Test
    void componentSelection() throws IOException {
        snapSelected(UiEditorScreen.COMPONENT_SAMPLE, "buy", "component-selected", ComponentEditor::shell);
    }

    @Test
    void boundProperty() throws IOException {
        XamlEditorDocument document = new XamlEditorDocument("demo", "demo.xaml",
                EditorSession.open(DocumentSource.memory("demo", "demo.xaml", UiEditorScreen.COMPONENT_SAMPLE)));
        EditorShell editor = ComponentEditor.shell(document);
        UiNode buy = find(document.session().document().root(), "buy");
        dev.sixik.unigui.xaml.component.Promotions.promote(document.session(), buy, "text",
                dev.sixik.unigui.xaml.component.Promotions.Kind.VARIABLE).orElseThrow();
        dev.sixik.unigui.xaml.component.Promotions.promote(document.session(), buy, "width",
                dev.sixik.unigui.xaml.component.Promotions.Kind.VARIABLE).orElseThrow();
        dev.sixik.unigui.xaml.component.Promotions.promote(document.session(), buy, "padding",
                dev.sixik.unigui.xaml.component.Promotions.Kind.PARAM).orElseThrow();
        dev.sixik.unigui.xaml.component.Promotions.promote(document.session(), buy, "flexDirection",
                dev.sixik.unigui.xaml.component.Promotions.Kind.VARIABLE).orElseThrow();
        document.session().select(buy);
        SnapshotRenderer renderer = new SnapshotRenderer(editor.root(), WIDTH, 900.0f, SCALE);
        renderer.settle(4);
        renderer.write(OUT.resolve("component-tall.png"));
    }

    private static Widget byId(Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (Widget child : widget.children()) {
            Widget found = byId(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static void snapEntry(String entry, String name, String... panes) throws IOException {
        Widget root = UiEditorScreen.createComponentScreen().root();
        SnapshotRenderer renderer = new SnapshotRenderer(root, WIDTH, HEIGHT, SCALE);
        renderer.settle(3);
        ((dev.sixik.unigui.widgets.interaction.Button) byId(root, entry)).click();
        for (String pane : panes) dock(root).selectPane(pane);
        renderer.settle(3);
        renderer.write(OUT.resolve(name + ".png"));
    }

    @Test
    void contractVariable() throws IOException {
        snapEntry("entry.Param.Title", "contract-param", "run");
    }

    @Test
    void contractEvent() throws IOException {
        snapEntry("entry.Event.Buy", "contract-event", "graph");
    }

    @Test
    void openMenuAndList() throws IOException {
        XamlEditorDocument document = new XamlEditorDocument("demo", "demo.xaml",
                EditorSession.open(DocumentSource.memory("demo", "demo.xaml", UiEditorScreen.SAMPLE)));
        EditorShell editor = UiEditor.shell(document);
        document.session().select(find(document.session().document().root(), "title"));
        SnapshotRenderer renderer = new SnapshotRenderer(editor.root(), WIDTH, HEIGHT, SCALE);
        renderer.settle(3);
        editor.openMenu("Edit", new dev.sixik.unigui.api.math.MutableRect(62.0f, 4.0f, 30.0f, 20.0f));
        renderer.settle(3);
        renderer.write(OUT.resolve("menu.png"));
    }

    @Test
    void projectStyles() throws IOException {
        snap(UiEditorScreen.createProjectScreen().root(), "project-styles", "styles");
    }

    @Test
    void screenEditorCode() throws IOException {
        snap(UiEditorScreen.createScreen().root(), "screen-code", "code");
    }

    @Test
    void componentEditor() throws IOException {
        snap(UiEditorScreen.createComponentScreen().root(), "component");
    }

    @Test
    void componentGraph() throws IOException {
        snap(UiEditorScreen.createComponentScreen().root(), "component-graph", "graph");
    }

    @Test
    void projectEditor() throws IOException {
        snap(UiEditorScreen.createProjectScreen().root(), "project");
    }
}
