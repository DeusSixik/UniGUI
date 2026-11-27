package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType.CUBE;
import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType.EMPTY;
import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType.MINECRAFT_BLOCK;
import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType.MINECRAFT_ENTITY;
import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType.MINECRAFT_ITEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that saved prefabs survive reopening and each instance owns its transform state. */
class UnityPrefabStoreTest {
    /** Isolated folder for persisted assets. */
    @TempDir
    Path directory;
    /** Allows float rounding in stored and reconstructed transforms. */
    private static final float EPSILON = 1e-5f;

    /**
     * Verifies that a nested template is saved, reloaded, renamed and removed by stable identity.
     * @throws Exception if filesystem or serialization operations fail
     */
    @Test
    void saveLoad_nestedObjectPreservesPoseAndInstantiatesIndependentCopies() throws Exception {
        Path assets = directory.resolve("Prefabs");
        UnityPrefabStore store = new UnityPrefabStore(assets);
        assertTrue(store.load(file -> {}).isEmpty());
        assertFalse(Files.exists(assets));
        UnitySceneObject group = new UnitySceneObject("Сборка", EMPTY);
        group.setPosition(1, 2, 3);
        UnitySceneObject cube = new UnitySceneObject("Child", CUBE);
        cube.setPosition(-3, 4, 5);
        cube.setScale(2, 3, 4);
        cube.setOrigin(0.3f, 0.2f, 0.1f);
        cube.setAngles(11, -35, 408);
        cube.setVisible(false);
        group.addChild(cube);

        UnityPrefab prefab = UnityPrefab.capture(group.name(), group);
        store.save(prefab);
        cube.rename("Changed after save");
        UnityPrefabStore reopened = new UnityPrefabStore(assets);
        UnityPrefab loaded = reopened.load(file -> {}).get(0);
        assertEquals(prefab.id(), loaded.id());
        assertEquals("Сборка", loaded.name());
        UnitySceneObject first = loaded.instantiate();
        UnitySceneObject second = loaded.instantiate();
        UnitySceneObject child = (UnitySceneObject) first.children().get(0);
        assertEquals(2, first.position(new MutableVec3()).y(), EPSILON);
        assertEquals("Child", child.name());
        assertFalse(child.visible());
        assertEquals(-3, child.position(new MutableVec3()).x(), EPSILON);
        assertEquals(4, child.scale(new MutableVec3()).z(), EPSILON);
        assertEquals(0.2, child.origin(new MutableVec3()).y(), EPSILON);
        assertEquals(408, child.angles().z(), EPSILON);
        child.setPosition(18, 0, 0);
        assertNotSame(first.children().get(0), second.children().get(0));
        assertEquals(-3, second.children().get(0).position(new MutableVec3()).x(), EPSILON);

        UnityPrefab renamed = loaded.renamed("Renamed");
        reopened.save(renamed);
        assertEquals(List.of("Renamed"), reopened.load(file -> {}).stream().map(UnityPrefab::name).toList());
        assertEquals(prefab.id(), renamed.id());
        reopened.delete(renamed);
        assertTrue(store.load(file -> {}).isEmpty());
    }

    /**
     * Ensures a corrupt asset is reported without preventing other templates from loading.
     * @throws Exception if writing the fixture fails
     */
    @Test
    void load_invalidFile_reportsItAndKeepsValidAssets() throws Exception {
        UnityPrefabStore store = new UnityPrefabStore(directory);
        UnityPrefab prefab = UnityPrefab.capture("Cube", new UnitySceneObject("Cube", CUBE));
        store.save(prefab);
        Path broken = store.path(UUID.randomUUID());
        Files.write(broken, new byte[]{0, 1, 2});
        List<Path> unreadable = new ArrayList<>();

        assertEquals(List.of(prefab.id()), store.load(unreadable::add).stream().map(UnityPrefab::id).toList());
        assertEquals(List.of(broken.getFileName()), unreadable);
    }

    /**
     * Verifies that each Minecraft object recreates its native render node exactly once after a copy or load.
     * @throws Exception if writing or reading the prefab fails
     */
    @Test
    void saveLoad_minecraftObjectsPreserveRegistryIdsAndChildOrder() throws Exception {
        UnitySceneObject block = new UnitySceneObject("Stone", MINECRAFT_BLOCK, "minecraft:stone");
        block.addChild(new UnitySceneObject("Sword", MINECRAFT_ITEM, "minecraft:diamond_sword"));
        block.addChild(new UnitySceneObject("Zombie", MINECRAFT_ENTITY, "minecraft:zombie"));
        UnityPrefabStore store = new UnityPrefabStore(directory);
        store.save(UnityPrefab.capture("Display", block));
        UnitySceneObject loaded = store.load(file -> {}).get(0).instantiate();
        for (UnitySceneObject copy : List.of(loaded, block.copySubtree())) {
            assertEquals("minecraft:stone", copy.minecraftId());
            assertEquals(3, copy.children().size());
            assertTrue(copy.children().get(0) instanceof BlockNode);
            UnitySceneObject sword = (UnitySceneObject) copy.children().get(1);
            UnitySceneObject zombie = (UnitySceneObject) copy.children().get(2);
            assertEquals("minecraft:diamond_sword", sword.minecraftId());
            assertEquals("minecraft:zombie", zombie.minecraftId());
            assertTrue(sword.children().get(0) instanceof ItemNode);
            assertTrue(zombie.children().get(0) instanceof EntityNode);
            assertNotSame(block.children().get(0), copy.children().get(0));
        }
    }

    /**
     * Tests that a prefab file written before Minecraft IDs were introduced remains readable.
     * @throws Exception if saving or loading the version 1 fixture fails
     */
    @Test
    void load_versionOneStillReadsExistingPrefabFiles() throws Exception {
        UnityPrefabStore store = new UnityPrefabStore(directory);
        UnityPrefab cube = UnityPrefab.capture("Cube", new UnitySceneObject("Cube", CUBE));
        store.save(cube);
        Path file = store.path(cube.id());
        byte[] contents = Files.readAllBytes(file);
        // Primitive nodes have identical records in both versions. Only their header differs.
        contents[3] = 1;
        Files.write(file, contents);
        assertEquals(CUBE, store.load(unreadable -> {}).get(0).instantiate().type());
    }
}
