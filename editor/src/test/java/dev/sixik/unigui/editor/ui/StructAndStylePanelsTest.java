package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StructAndStylePanelsTest {
    private Project project;
    private EditorShell shell;
    private ProjectPanel projectPanel;

    @BeforeEach
    void setUp() {
        project = Project.create(new PackManifest("ui", "1", "UI", ""));
        project.createStruct("Product");
        project.createScreen("main");
        project.session("screens/main.xaml").replaceText("<VBox id=\"root\"><Button id=\"b\"/></VBox>");
        project.session("structs/product.xaml").replaceText(
                "<Struct x:Name=\"Product\" xmlns:x=\"https://unigui.dev/xaml\"><Field Name=\"title\" Type=\"string\"/></Struct>");
        shell = ProjectEditor.shell(project, null, null);
        projectPanel = (ProjectPanel) shell.panel(ProjectEditor.PROJECT).orElseThrow();
        shell.dock().findPane(ProjectEditor.PROJECT).content();
    }

    private Widget pane(String id) {
        Widget content = shell.dock().findPane(id).content();
        applyAll(content);
        return content;
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

    private TextField field(String pane, String id) {
        Widget widget = find(pane(pane), id);
        assertNotNull(widget, "no widget " + id);
        return (TextField) widget;
    }

    private Button button(String pane, String id) {
        Widget widget = find(pane(pane), id);
        assertNotNull(widget, "no widget " + id);
        return (Button) widget;
    }

    private List<String> labels(String pane) {
        List<String> texts = new ArrayList<>();
        collect(pane(pane), texts);
        return texts;
    }

    private static void collect(Widget widget, List<String> texts) {
        if (widget instanceof Label label) texts.add(label.text());
        for (Widget child : widget.children()) collect(child, texts);
    }

    private void chooseFromMenu(String pane, String menuId, String item) {
        button(pane, menuId).click();
        for (ContextMenu menu : all(shell.overlay(), ContextMenu.class)) {
            for (Widget entry : menu.itemWidgets()) {
                if (entry instanceof Button button && button.text().startsWith(item)) {
                    button.click();
                    return;
                }
            }
        }
        throw new AssertionError("no menu item '" + item + "'");
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

    private String structText() {
        return project.text("structs/product.xaml").orElseThrow();
    }

    // ------------------------------------------------------------------ структуры

    @Test
    void theStructPanelShowsFieldsOnlyForAStructDocument() {
        projectPanel.open("screens/main.xaml");
        assertTrue(labels(StructPanelAccess.ID).contains("Not a struct document"));
        projectPanel.open("structs/product.xaml");
        assertNotNull(find(pane(StructPanelAccess.ID), "struct.field.title.name"));
    }

    @Test
    void fieldsAreAddedRetypedRenamedMovedAndRemoved() {
        projectPanel.open("structs/product.xaml");
        button(StructPanelAccess.ID, "struct.add").click();
        assertTrue(project.session("structs/product.xaml").text().contains("field2"));

        field(StructPanelAccess.ID, "struct.field.title.type").text("int");
        assertTrue(project.session("structs/product.xaml").text().contains("Type=\"int\""));
        field(StructPanelAccess.ID, "struct.field.title.name").text("name");
        assertTrue(project.session("structs/product.xaml").text().contains("Name=\"name\""));
        assertFalse(project.session("structs/product.xaml").text().contains("Name=\"title\""));

        chooseFromMenu(StructPanelAccess.ID, "struct.menu.field2", "Remove");
        assertFalse(project.session("structs/product.xaml").text().contains("field2"));
    }

    @Test
    void aBadFieldTypeIsReportedAndNotWritten() {
        projectPanel.open("structs/product.xaml");
        field(StructPanelAccess.ID, "struct.field.title.type").text("nonsense");
        assertTrue(project.session("structs/product.xaml").text().contains("Type=\"string\""));
        assertTrue(labels(StructPanelAccess.ID).stream().anyMatch(text -> !text.isEmpty() && text.toLowerCase().contains("nonsense")),
                labels(StructPanelAccess.ID).toString());
    }

    // ------------------------------------------------------------------ стили

    @Test
    void stylesAreAddedWithSettersEditedAndRemoved() {
        projectPanel.open("screens/main.xaml");
        field(StructPanelAccess.STYLES, "style.key").text("Primary");
        field(StructPanelAccess.STYLES, "style.target").text("Button");
        button(StructPanelAccess.STYLES, "style.add").click();
        assertTrue(project.session("screens/main.xaml").text().contains("Primary"));

        field(StructPanelAccess.STYLES, "style.Primary.property").text("text");
        field(StructPanelAccess.STYLES, "style.Primary.value").text("Go");
        button(StructPanelAccess.STYLES, "style.Primary.add").click();
        assertTrue(project.session("screens/main.xaml").text().contains("Value=\"Go\""));

        field(StructPanelAccess.STYLES, "style.Primary.setter.text").text("Buy");
        assertTrue(project.session("screens/main.xaml").text().contains("Value=\"Buy\""));

        chooseFromMenu(StructPanelAccess.STYLES, "style.menu.Primary", "Remove style");
        assertFalse(project.session("screens/main.xaml").text().contains("Primary"));
    }

    @Test
    void anUnknownSetterPropertyIsRefusedWithAReason() {
        projectPanel.open("screens/main.xaml");
        field(StructPanelAccess.STYLES, "style.key").text("Primary");
        field(StructPanelAccess.STYLES, "style.target").text("Button");
        button(StructPanelAccess.STYLES, "style.add").click();
        field(StructPanelAccess.STYLES, "style.Primary.property").text("noSuchProperty");
        field(StructPanelAccess.STYLES, "style.Primary.value").text("1");
        button(StructPanelAccess.STYLES, "style.Primary.add").click();
        assertFalse(project.session("screens/main.xaml").text().contains("noSuchProperty"));
        assertTrue(labels(StructPanelAccess.STYLES).stream().anyMatch(text -> text.contains("noSuchProperty")),
                labels(StructPanelAccess.STYLES).toString());
    }

    @Test
    void aStyleNeedsAKeyAndAWidgetToLiveIn() {
        projectPanel.open("screens/main.xaml");
        button(StructPanelAccess.STYLES, "style.add").click();
        assertTrue(labels(StructPanelAccess.STYLES).contains("Type a key for the new style."));
        projectPanel.open("structs/product.xaml");
        assertTrue(labels(StructPanelAccess.STYLES).contains("This document has no widget to hold styles"));
        assertEquals(1, project.entries(Project.Kind.STRUCT).size());
    }
}
