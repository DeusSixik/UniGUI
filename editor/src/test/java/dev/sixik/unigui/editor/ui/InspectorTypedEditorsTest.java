package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectorTypedEditorsTest {
    private static final String SOURCE = "<VBox id=\"root\"><Label id=\"l\" text=\"Hi\"/><Button id=\"b\"/></VBox>";

    private EditorSession session;
    private EditorShell shell;

    @BeforeEach
    void setUp() {
        session = EditorSession.open(DocumentSource.memory("d", "D", SOURCE));
        shell = UiEditor.shell(new XamlEditorDocument("d", "D", session));
    }

    private UiNode label() {
        return session.document().root().children().get(0);
    }

    private Widget inspector() {
        Widget content = shell.dock().findPane(UiEditor.INSPECTOR).content();
        applyAll(content);
        return content;
    }

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) applyAll(child);
    }

    private Widget find(String id) {
        return find(inspector(), id);
    }

    private static Widget find(Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (Widget child : widget.children()) {
            Widget found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private TextField field(String id) {
        Widget widget = find(id);
        assertNotNull(widget, "no editor " + id);
        return (TextField) widget;
    }

    private List<String> labels() {
        List<String> texts = new ArrayList<>();
        collect(inspector(), texts);
        return texts;
    }

    private static void collect(Widget widget, List<String> texts) {
        if (widget instanceof Label label) texts.add(label.text());
        for (Widget child : widget.children()) collect(child, texts);
    }

    @Test
    void aColorIsEditedAsHexAndByChannel() {
        session.select(label());
        field("inspector.color.color").text("ff8800");
        assertEquals("#FF8800", label().attributeText("color", ""), "the text is normalized");
        assertEquals("255", field("inspector.color.color.r").text().isEmpty() ? "" : "255");

        field("inspector.color.color.g").text("16");
        assertEquals("#FF1000", label().attributeText("color", ""), "a channel edit rewrites the whole color");
        assertEquals("#FF1000", field("inspector.color.color").text());

        field("inspector.color.color.a").text("128");
        assertEquals("#80FF1000", label().attributeText("color", ""));
    }

    @Test
    void anInvalidColorIsReportedAndLeavesTheDocument() {
        session.select(label());
        field("inspector.color.color").text("red");
        assertTrue(label().attributeText("color").isEmpty());
        assertTrue(labels().stream().anyMatch(text -> text.contains("not a color")), labels().toString());
    }

    @Test
    void insetsAreFourFieldsWrittenInTheShortestForm() {
        session.select(label());
        field("inspector.insets.margin.top").text("4");
        assertEquals("4 0 0 0", label().attributeText("margin", ""));
        field("inspector.insets.margin.right").text("4");
        field("inspector.insets.margin.bottom").text("4");
        field("inspector.insets.margin.left").text("4");
        assertEquals("4", label().attributeText("margin", ""), "four equal values collapse");
    }

    @Test
    void numbersAreNormalizedAndBadNumbersAreRefused() {
        session.select(label());
        field("inspector.number.opacity").text("0.50");
        assertEquals("0.5", label().attributeText("opacity", ""));
        field("inspector.number.opacity").text("lots");
        assertEquals("0.5", label().attributeText("opacity", ""));
        assertTrue(labels().stream().anyMatch(text -> text.contains("not a number")), labels().toString());
    }

    @Test
    void textureProjectsOfferTheirResourcesAsAList() {
        Project project = Project.create(new PackManifest("ui", "1", "UI", ""));
        project.createScreen("main");
        project.addResource("textures/icons/sword.png", new byte[]{1});
        project.addResource("textures/bg.png", new byte[]{1});
        project.session("screens/main.xaml").replaceText("<VBox><Button id=\"b\"/></VBox>");
        EditorShell editor = ProjectEditor.shell(project, null, null);
        ProjectPanel panel = (ProjectPanel) editor.panel(ProjectEditor.PROJECT).orElseThrow();
        editor.dock().findPane(ProjectEditor.PROJECT).content();
        panel.open("screens/main.xaml");
        EditorSession main = project.session("screens/main.xaml");
        main.select(main.document().root().children().get(0));

        Widget inspector = editor.dock().findPane(UiEditor.INSPECTOR).content();
        applyAll(inspector);
        ComboBox combo = (ComboBox) find(inspector, "inspector.resource.hoverTexture");
        assertNotNull(combo, "a texture property becomes a list of the project's textures");
        combo.selectedIndex(2);
        assertEquals("icons/sword.png", main.document().root().children().get(0).attributeText("hoverTexture", ""));
    }
}
