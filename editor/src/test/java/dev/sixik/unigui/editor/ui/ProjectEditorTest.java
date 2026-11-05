package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectEditorTest {
    @TempDir
    Path temp;

    private Project project;
    private EditorShell shell;

    @BeforeEach
    void setUp() {
        project = Project.create(new PackManifest("shop", "1.0", "Shop", ""));
        project.createScreen("main");
        project.createComponent("Card");
        shell = ProjectEditor.shell(project, temp.resolve("project"), temp.resolve("out/shop.zip"));
    }

    private Widget content() {
        Widget widget = shell.dock().findPane(ProjectEditor.PROJECT).content();
        applyAll(widget);
        return widget;
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

    private Button button(String id) {
        Widget widget = find(content(), id);
        assertNotNull(widget, "no widget " + id);
        return (Button) widget;
    }

    private ProjectPanel panel() {
        return (ProjectPanel) shell.panel(ProjectEditor.PROJECT).orElseThrow();
    }

    private void type(String text) {
        ((TextField) find(content(), "project.name")).text(text);
    }

    private String openTitle() {
        return shell.context().document().map(doc -> doc.title()).orElse("");
    }

    @Test
    void theListShowsEveryFileAndClickingOpensTheDocumentEverywhere() {
        assertNotNull(find(content(), "project.file.screens/main.xaml"));
        assertNotNull(find(content(), "project.file.components/card.xaml"));
        assertEquals("", openTitle());

        button("project.file.components/card.xaml").click();
        assertEquals("card.xaml", openTitle());
        assertTrue(shell.context().document().orElseThrow() instanceof XamlEditorDocument);
        assertEquals("components/card.xaml", panel().selectedPath());
        // Остальные панели видят тот же документ: иерархия компонента не пуста.
        Widget hierarchy = shell.dock().findPane(UiEditor.HIERARCHY).content();
        applyAll(hierarchy);
        assertFalse(hierarchy.children().isEmpty());
    }

    @Test
    void newFilesAreCreatedFromTheNameFieldOrWithADefaultName() {
        type("Product");
        button("project.new.component").click();
        assertTrue(project.has("components/product.xaml"));
        assertEquals("product.xaml", openTitle());
        assertNotNull(find(content(), "project.file.components/product.xaml"), "the list follows the project");

        button("project.new.screen").click();
        assertTrue(project.has("screens/Screen1.xaml"), "an empty field gives Screen1");
        button("project.new.screen").click();
        assertTrue(project.has("screens/Screen2.xaml"));

        type("Product");
        button("project.new.component").click();
        assertTrue(panel().statusText().contains("already exists"), panel().statusText());
    }

    @Test
    void renamingKeepsTheDocumentOpenUnderItsNewName() {
        button("project.file.components/card.xaml").click();
        type("ProductCard");
        button("project.rename").click();
        assertTrue(project.has("components/productCard.xaml"), panel().statusText());
        assertFalse(project.has("components/card.xaml"));
        assertEquals("productCard.xaml", openTitle());
        assertNotNull(find(content(), "project.file.components/productCard.xaml"));
    }

    @Test
    void deletingIsRefusedWhileTheFileIsUsed() {
        project.session("screens/main.xaml").replaceText("<VBox><Card id=\"c\"/></VBox>");
        project.session("screens/main.xaml").save();
        button("project.file.components/card.xaml").click();
        button("project.delete").click();
        assertTrue(project.has("components/card.xaml"));
        assertTrue(panel().statusText().contains("used by"), panel().statusText());

        button("project.file.screens/main.xaml").click();
        button("project.delete").click();
        assertFalse(project.has("screens/main.xaml"));
        button("project.file.components/card.xaml").click();
        button("project.delete").click();
        assertFalse(project.has("components/card.xaml"), "unused once the screen is gone");
    }

    @Test
    void checkListsProblemsAndTheyOpenTheirFile() {
        project.session("screens/main.xaml").replaceText("<VBox><NoSuchWidget/></VBox>");
        button("project.check").click();
        assertTrue(panel().statusText().contains("1 problem"), panel().statusText());
        Button problem = null;
        for (Widget candidate : content().children()) {
            if (candidate instanceof PanelWidget) {
                problem = findProblem(candidate);
                if (problem != null) break;
            }
        }
        assertNotNull(problem, "the problem is listed");
        problem.click();
        assertEquals("main.xaml", openTitle());
    }

    private static Button findProblem(Widget widget) {
        if (widget instanceof Button button && button.text().contains("NoSuchWidget")) return button;
        for (Widget child : widget.children()) {
            Button found = findProblem(child);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void saveWritesTheFolderAndExportWritesTheArchive() throws IOException {
        button("project.save").click();
        assertTrue(Files.isRegularFile(temp.resolve("project/pack.xaml")), panel().statusText());
        assertTrue(Files.isRegularFile(temp.resolve("project/components/card.xaml")));

        button("project.export").click();
        assertArrayEquals(project.toArchive(), Files.readAllBytes(temp.resolve("out/shop.zip")));
    }

    @Test
    void undoAndRedoStepThroughStructuralChanges() {
        type("Extra");
        button("project.new.screen").click();
        assertTrue(project.has("screens/Extra.xaml"));

        button("project.undo").click();
        assertFalse(project.has("screens/Extra.xaml"));
        assertEquals("", openTitle(), "the closed file's document is closed");
        assertTrue(panel().statusText().startsWith("Undid Create"), panel().statusText());
        assertEquals(null, find(content(), "project.file.screens/Extra.xaml"), "the list follows");

        button("project.redo").click();
        assertTrue(project.has("screens/Extra.xaml"));
        assertNotNull(find(content(), "project.file.screens/Extra.xaml"));
        button("project.undo").click(); // Extra
        button("project.undo").click(); // Card
        button("project.undo").click(); // main
        button("project.undo").click();
        assertTrue(panel().statusText().contains("Nothing to undo"), panel().statusText());
    }
}
