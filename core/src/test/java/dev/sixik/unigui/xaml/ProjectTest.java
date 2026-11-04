package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectTest {
    private final Project project = Project.create(new PackManifest("shop", "1.0", "Shop", "A shop"));

    @AfterEach
    void cleanUp() {
        project.uninstall();
    }

    private static List<String> paths(List<Project.Entry> entries) {
        return entries.stream().map(Project.Entry::path).toList();
    }

    @Test
    void newDocumentsAreCreatedByKindWithValidTemplates() {
        project.createScreen("main");
        project.createComponent("ProductCard");
        project.createStruct("Product");
        assertEquals(List.of("screens/main.xaml"), paths(project.entries(Project.Kind.SCREEN)));
        assertEquals(List.of("components/productCard.xaml"), paths(project.entries(Project.Kind.COMPONENT)));
        assertEquals(List.of("structs/product.xaml"), paths(project.entries(Project.Kind.STRUCT)));
        assertEquals(List.of(), project.check(), "templates are valid documents");
    }

    @Test
    void namesAreCheckedAndDuplicatesRefused() {
        project.createScreen("main");
        assertThrows(IllegalArgumentException.class, () -> project.createScreen("main"));
        assertThrows(IllegalArgumentException.class, () -> project.createComponent("1bad"));
        assertThrows(IllegalArgumentException.class, () -> project.addResource("../escape.png", new byte[1]));
    }

    @Test
    void oneSessionPerDocumentAndEditsReachThePackBeforeBeingSaved() {
        project.createScreen("main");
        EditorSession session = project.session("screens/main.xaml");
        assertSame(session, project.session("screens/main.xaml"), "panels share one history");
        UiNode root = session.document().root();
        assertTrue(session.apply(Edits.addChild(root, 0, new UiNode(UiName.of("Label")))));
        assertTrue(project.dirty());
        assertTrue(project.toPack().text("screens/main.xaml").orElseThrow().contains("Label"), "export includes unsaved edits");
        assertFalse(session.dirty(), "exporting syncs the edits into the project");
    }

    @Test
    void aProjectRoundTripsThroughAFolderAndThroughAPack(@TempDir Path folder) throws IOException {
        project.createComponent("Card");
        project.createScreen("main");
        project.addResource("textures/icons/sword.png", new byte[]{1, 2, 3});
        project.save(folder);
        assertFalse(project.dirty());

        Project reopened = Project.open(folder);
        assertEquals(project.manifest(), reopened.manifest());
        assertEquals(project.entries(), reopened.entries());
        assertArrayEquals(new byte[]{1, 2, 3}, reopened.bytes("textures/icons/sword.png").orElseThrow());

        Project fromPack = Project.of(project.toPack());
        assertEquals(project.entries(), fromPack.entries());
        assertTrue(Arrays.equals(new byte[]{1, 2, 3}, fromPack.bytes("textures/icons/sword.png").orElseThrow()));
    }

    @Test
    void installingMakesProjectComponentsAvailableAtRuntime() {
        project.createComponent("Card");
        assertTrue(project.install().clean());
        assertTrue(Components.find("shop/card").isPresent());
        project.uninstall();
        assertTrue(Components.find("shop/card").isEmpty());
    }

    @Test
    void renamingAComponentUpdatesItsTagEverywhere() {
        project.createComponent("Card");
        project.createScreen("main");
        EditorSession screen = project.session("screens/main.xaml");
        screen.replaceText("<VBox><Card id=\"first\"/><Card id=\"second\"/></VBox>");
        screen.save();

        assertEquals("", project.rename("components/card.xaml", "ProductCard"));
        assertTrue(project.has("components/productCard.xaml"));
        assertFalse(project.has("components/card.xaml"));
        assertTrue(project.text("components/productCard.xaml").orElseThrow().contains("ProductCard"));
        String main = project.text("screens/main.xaml").orElseThrow();
        assertTrue(main.contains("<ProductCard id=\"first\"") && main.contains("<ProductCard id=\"second\""), main);
        assertFalse(main.contains("<Card"), main);
        assertEquals(List.of(), project.check());
    }

    @Test
    void renamingIsRefusedForTakenNamesStructsAndBadNames() {
        project.createComponent("Card");
        project.createComponent("Other");
        project.createStruct("Product");
        assertFalse(project.rename("components/card.xaml", "Other").isEmpty(), "the tag is taken");
        assertFalse(project.rename("structs/product.xaml", "Item").isEmpty(), "structs are referenced by type names");
        assertFalse(project.rename("components/card.xaml", "bad name").isEmpty());
        assertFalse(project.rename("components/missing.xaml", "X").isEmpty());
        assertTrue(project.has("components/card.xaml"));
    }

    @Test
    void checkReportsProblemsPerFile() {
        project.createScreen("main");
        project.session("screens/main.xaml").replaceText("<VBox><NoSuchWidget/></VBox>");
        List<Project.Problem> problems = project.check();
        assertEquals(1, problems.size(), problems.toString());
        assertEquals("screens/main.xaml", problems.get(0).path());
        assertTrue(problems.get(0).message().contains("NoSuchWidget"));
    }

    @Test
    void deletingRemovesTheFileAndItsSession() {
        project.createScreen("main");
        project.session("screens/main.xaml");
        assertTrue(project.delete("screens/main.xaml"));
        assertFalse(project.has("screens/main.xaml"));
        assertFalse(project.delete("screens/main.xaml"));
        assertThrows(IllegalArgumentException.class, () -> project.session("screens/main.xaml"));
    }

    @Test
    void structuralChangesNotifyListeners() {
        int[] calls = {0};
        project.onChanged(() -> calls[0]++);
        project.createScreen("main");
        project.rename("screens/main.xaml", "home");
        project.delete("screens/home.xaml");
        assertEquals(3, calls[0]);
    }
}
