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
        project.createStruct("Other");
        assertFalse(project.rename("components/card.xaml", "Other").isEmpty(), "the tag is taken");
        assertFalse(project.rename("structs/product.xaml", "Other").isEmpty(), "the struct name is taken");
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

    private void shopWithReferences() {
        project.createStruct("Product");
        project.createComponent("Card");
        project.createScreen("main");
        project.addResource("textures/icons/sword.png", new byte[]{1});
        EditorSession card = project.session("components/card.xaml");
        card.replaceText("""
                <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract>
                    <Param Name="Item" Type="struct:Product"/>
                    <Event Name="Buy" Direction="Out" Args="item:struct:Product"/>
                  </Component.Contract>
                  <Button id="icon" hoverTexture="icons/sword.png"/>
                  <Component.Graph>
                    <Node Id="make" Type="Struct.Make" Struct="Product"/>
                  </Component.Graph>
                </Component>""");
        card.save();
        EditorSession screen = project.session("screens/main.xaml");
        screen.replaceText("<VBox><Card id=\"first\"/><Card id=\"second\"/></VBox>");
        screen.save();
    }

    @Test
    void usagesListEveryReferenceToAComponentAStructAndAResource() {
        shopWithReferences();
        List<Project.Usage> component = project.usages("components/card.xaml");
        assertEquals(2, component.size(), component.toString());
        assertTrue(component.stream().allMatch(use -> use.path().equals("screens/main.xaml")));

        List<Project.Usage> struct = project.usages("structs/product.xaml");
        assertTrue(struct.size() >= 3, "param type, event argument and the graph node: " + struct);
        assertTrue(struct.stream().allMatch(use -> use.path().equals("components/card.xaml")));

        List<Project.Usage> texture = project.usages("textures/icons/sword.png");
        assertEquals(1, texture.size(), texture.toString());
        assertEquals("components/card.xaml", texture.get(0).path());

        assertEquals(List.of(), project.usages("screens/main.xaml"));
    }

    @Test
    void deletingIsRefusedWhileSomethingUsesTheFile() {
        shopWithReferences();
        String reason = project.deleteIfUnused("components/card.xaml");
        assertTrue(reason.contains("screens/main.xaml"), reason);
        assertTrue(project.has("components/card.xaml"));
        assertEquals("", project.deleteIfUnused("screens/main.xaml"));
        assertEquals("", project.deleteIfUnused("components/card.xaml"), "unused once the screen is gone");
    }

    @Test
    void renamingAStructUpdatesTypesAndNodesEverywhere() {
        shopWithReferences();
        assertEquals("", project.rename("structs/product.xaml", "Item"));
        assertTrue(project.has("structs/item.xaml"));
        String struct = project.text("structs/item.xaml").orElseThrow();
        assertTrue(struct.contains("Name=\"Item\""), struct);
        String card = project.text("components/card.xaml").orElseThrow();
        assertTrue(card.contains("Type=\"struct:Item\"") && card.contains("Args=\"item:struct:Item\"")
                && card.contains("Struct=\"Item\""), card);
        assertFalse(card.contains("Product"), card);
        assertEquals(List.of(), project.check());
    }

    @Test
    void refreshingPicksUpExternalEditsAndKeepsUnsavedWork(@TempDir Path folder) throws IOException {
        project.createScreen("main");
        project.createComponent("Card");
        project.save(folder);
        project.install();

        java.nio.file.Files.writeString(folder.resolve("screens/main.xaml"), "<VBox><Label text=\"outside\"/></VBox>");
        java.nio.file.Files.writeString(folder.resolve("screens/new.xaml"), "<VBox/>");
        java.nio.file.Files.delete(folder.resolve("components/card.xaml"));

        Project.Refresh refresh = project.refreshFrom(folder);
        assertEquals(List.of("screens/new.xaml"), refresh.added());
        assertEquals(List.of("screens/main.xaml"), refresh.changed());
        assertEquals(List.of("components/card.xaml"), refresh.removed());
        assertTrue(project.text("screens/main.xaml").orElseThrow().contains("outside"));
        assertFalse(project.has("components/card.xaml"));
        assertTrue(Components.find("shop/card").isEmpty(), "the installation follows the folder");

        // Несохранённые правки не затираются внешним изменением.
        EditorSession main = project.session("screens/main.xaml");
        main.replaceText("<VBox><Label text=\"mine\"/></VBox>");
        java.nio.file.Files.writeString(folder.resolve("screens/main.xaml"), "<VBox><Label text=\"theirs\"/></VBox>");
        Project.Refresh conflict = project.refreshFrom(folder);
        assertEquals(List.of("screens/main.xaml"), conflict.conflicts());
        assertTrue(project.text("screens/main.xaml").orElseThrow().contains("mine"));

    }

    @Test
    void renamingAStructFieldUpdatesTheNodesAndWiresThatUseIt() {
        project.createStruct("Product");
        project.createComponent("Card");
        project.session("structs/product.xaml").replaceText("""
                <Struct x:Name="Product" xmlns:x="https://unigui.dev/xaml">
                  <Field Name="title" Type="string"/>
                </Struct>""");
        project.session("structs/product.xaml").save();
        EditorSession card = project.session("components/card.xaml");
        card.replaceText("""
                <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract>
                    <Param Name="Item" Type="struct:Product"/>
                    <Param Name="Caption" Type="string"/>
                    <Event Name="Go" Direction="In"/>
                  </Component.Contract>
                  <Label text="x"/>
                  <Component.Graph>
                    <Node Id="in" Type="Event.Go"/>
                    <Node Id="get" Type="Param.Get" Name="Item"/>
                    <Node Id="break" Type="Struct.Break" Struct="Product"/>
                    <Node Id="make" Type="Struct.Make" Struct="Product" title="hi"/>
                    <Node Id="with" Type="Struct.With" Struct="Product" Field="title"/>
                    <Node Id="set" Type="Param.Set" Name="Caption"/>
                    <Wire From="get.value" To="break.value"/>
                    <Wire From="break.title" To="set.value"/>
                    <Wire From="in.then" To="set.exec"/>
                  </Component.Graph>
                </Component>""");
        card.save();
        assertEquals(List.of(), project.check(), "the document is valid before the rename");

        assertEquals("", project.renameStructField("structs/product.xaml", "title", "name"));
        String text = project.text("components/card.xaml").orElseThrow();
        assertTrue(text.contains("From=\"break.name\""), text);
        assertTrue(text.contains("Field=\"name\""), text);
        assertTrue(text.contains("name=\"hi\""), text);
        assertFalse(text.contains("title"), text);
        assertEquals(List.of(), project.check(), "the graph still compiles after the rename");

        assertFalse(project.renameStructField("structs/product.xaml", "missing", "x").isEmpty());
        assertFalse(project.renameStructField("components/card.xaml", "a", "b").isEmpty());
    }

    @Test
    void pollingReadsTheFolderOnlyWhenItsFilesChanged(@TempDir Path folder) throws IOException {
        project.createScreen("main");
        project.save(folder);
        assertTrue(project.poll(folder).isEmpty(), "saving the project does not count as an outside change");

        Path file = folder.resolve("screens/main.xaml");
        java.nio.file.Files.writeString(file, "<VBox><Label text=\"outside\"/></VBox>");
        java.nio.file.Files.setLastModifiedTime(file,
                java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 5_000));
        Project.Refresh refresh = project.poll(folder).orElseThrow();
        assertEquals(List.of("screens/main.xaml"), refresh.changed());
        assertTrue(project.text("screens/main.xaml").orElseThrow().contains("outside"));
        assertTrue(project.poll(folder).isEmpty(), "the next poll sees no change");
    }

    // ------------------------------------------------------------------ отмена структурных правок

    @Test
    void creatingAndDeletingFilesCanBeUndoneAndRedone() {
        assertFalse(project.canUndo());
        project.createScreen("main");
        project.createComponent("Card");
        assertEquals("Create components/card.xaml", project.undoLabel().orElseThrow());

        assertTrue(project.undo());
        assertFalse(project.has("components/card.xaml"));
        assertTrue(project.has("screens/main.xaml"));
        assertTrue(project.undo());
        assertFalse(project.has("screens/main.xaml"));
        assertFalse(project.undo(), "nothing left to undo");

        assertTrue(project.redo());
        assertTrue(project.has("screens/main.xaml"));
        assertTrue(project.redo());
        assertTrue(project.has("components/card.xaml"));
        assertFalse(project.redo());
    }

    @Test
    void aNewEditAfterAnUndoDropsTheRedoHistory() {
        project.createScreen("main");
        project.undo();
        assertTrue(project.canRedo());
        project.createScreen("other");
        assertFalse(project.canRedo());
    }

    @Test
    void deletingAFileAndUndoingBringsBackItsEditedContent() {
        project.createScreen("main");
        EditorSession main = project.session("screens/main.xaml");
        main.replaceText("<VBox><Label text=\"keep me\"/></VBox>");
        assertTrue(project.dirty("screens/main.xaml"));

        assertEquals("", project.deleteIfUnused("screens/main.xaml"));
        assertFalse(project.has("screens/main.xaml"));
        assertTrue(project.undo());
        assertTrue(project.text("screens/main.xaml").orElseThrow().contains("keep me"), "unsaved edits were synced before the delete");
        assertEquals(List.of(), project.check());
    }

    @Test
    void aComponentRenameAndItsTagUpdatesAreUndoneTogether() {
        project.createComponent("Card");
        project.createScreen("main");
        EditorSession screen = project.session("screens/main.xaml");
        screen.replaceText("<VBox><Card id=\"c\"/></VBox>");
        screen.save();

        assertEquals("", project.rename("components/card.xaml", "ProductCard"));
        assertTrue(project.text("screens/main.xaml").orElseThrow().contains("<ProductCard"));
        assertTrue(project.undo());
        assertTrue(project.has("components/card.xaml"));
        assertFalse(project.has("components/productCard.xaml"));
        assertTrue(project.text("screens/main.xaml").orElseThrow().contains("<Card"), "the tag in the screen is back too");
        assertEquals(List.of(), project.check());
        assertTrue(project.redo());
        assertTrue(project.text("screens/main.xaml").orElseThrow().contains("<ProductCard"));
    }

    @Test
    void refusedOperationsLeaveNoHistoryAndTheManifestIsUndoable() {
        project.createScreen("main");
        int before = (int) java.util.stream.Stream.iterate(0, n -> n + 1).limit(1).count();
        project.rename("screens/main.xaml", "bad name");
        project.rename("screens/missing.xaml", "x");
        assertThrows(IllegalArgumentException.class, () -> project.createScreen("main"));
        assertEquals("Create screens/main.xaml", project.undoLabel().orElseThrow(), "refused operations did not record steps");
        assertEquals(1, before);

        PackManifest old = project.manifest();
        project.manifest(new PackManifest("shop", "2.0", "Shop", "changed"));
        assertEquals("2.0", project.manifest().version());
        assertTrue(project.undo());
        assertEquals(old, project.manifest());
    }

    @Test
    void theHistoryIsBounded() {
        for (int i = 0; i < Project.HISTORY_LIMIT + 10; i++) project.createScreen("s" + i);
        int undone = 0;
        while (project.undo()) undone++;
        assertEquals(Project.HISTORY_LIMIT, undone);
    }
}
