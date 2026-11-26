package dev.sixik.unigui.impl.xml.minecraft;

import dev.sixik.unigui.backend.minecraft_impl.MinecraftScene;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.editor.ui.UiEditor;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет декларации сцены на настоящих реестрах Minecraft. */
final class MinecraftSceneXmlTest {
    /** Запускает статические реестры для тестов без клиента. */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ScenePlatform.install(MinecraftScene.PLATFORM);
    }

    /** Проверяет преобразование тегов в 3D-узлы и сохранение деклараций в снимке. */
    @Test
    void load_withVanillaDeclarations_createsSceneRootsAndExportsTags() {
        Schema schema = Schema.builtIns();
        Scene3D widget = assertInstanceOf(Scene3D.class, Xaml.load("""
                <Scene3D camera="orbit" yaw="30" pitch="25">
                    <Block state="minecraft:furnace[facing=east]" pos="1,2,-3"/>
                    <Structure/>
                    <Entity type="minecraft:zombie" pos="2,0,0"/>
                    <Item item="minecraft:diamond_sword" pos="0, 1, 0"/>
                </Scene3D>
                """, schema, LoadOptions.DEFAULT));

        var roots = widget.scene().roots();
        assertEquals(4, roots.size());
        var block = assertInstanceOf(BlockNode.class, roots.get(0));
        assertSame(Blocks.FURNACE, MinecraftScene.state(block).getBlock());
        assertEquals(Direction.EAST, MinecraftScene.state(block).getValue(FurnaceBlock.FACING));
        var position = block.position(new MutableVec3());
        assertEquals(1, position.x(), 0);
        assertEquals(2, position.y(), 0);
        assertEquals(-3, position.z(), 0);
        assertInstanceOf(StructureNode.class, roots.get(1));
        assertInstanceOf(EntityNode.class, roots.get(2));
        assertEquals("minecraft:diamond_sword", assertInstanceOf(ItemNode.class, roots.get(3)).id());
        assertTrue(widget.children().isEmpty());

        var snapshot = new Snapshotter(schema).snapshot(widget).document();
        assertEquals(List.of("Block", "Structure", "Entity", "Item"),
                snapshot.root().children().stream().map(child -> child.type().local()).toList());
    }

    /** Проверяет диагностику некорректного состояния блока при загрузке. */
    @Test
    void load_withUnknownBlockProperty_reportsTheDeclarationError() {
        Schema schema = Schema.builtIns();
        var failure = assertThrows(XamlLoadException.class, () -> Xaml.load("""
                <Scene3D><Block state="minecraft:furnace[unknown=true]"/></Scene3D>
                """, schema, LoadOptions.DEFAULT));
        assertTrue(failure.getMessage().contains("Invalid block state"), failure::getMessage);
    }

    /** Проверяет, что неизвестные предмет и сущность сообщаются при загрузке. */
    @Test
    void load_withUnknownItemOrEntity_reportsTheDeclarationError() {
        Schema schema = Schema.builtIns();
        assertThrows(XamlLoadException.class, () -> Xaml.load("""
                <Scene3D><Entity type="minecraft:not_a_mob"/></Scene3D>
                """, schema, LoadOptions.DEFAULT));
        assertThrows(XamlLoadException.class, () -> Xaml.load("""
                <Scene3D><Structure src="minecraft:not_a_structure"/></Scene3D>
                """, schema, LoadOptions.DEFAULT));
    }

    /** Проверяет загрузку NBT по идентификатору ресурса и начальную высоту среза. */
    @Test
    void load_withStructureResource_resolvesNbtAndAppliesLayerLimit() {
        Schema schema = Schema.builtIns();
        Scene3D widget = assertInstanceOf(Scene3D.class, Xaml.load("""
                <Scene3D><Structure src="minecraft:igloo/top" topVisibleY="0"/></Scene3D>
                """, schema, LoadOptions.DEFAULT));
        StructureNode structure = assertInstanceOf(StructureNode.class, widget.scene().roots().get(0));
        assertEquals(0, structure.topVisibleY());
        assertTrue(structure.bounds() == null || structure.bounds().maxY() <= 1);
        structure.setTopVisibleY(Integer.MAX_VALUE);
        assertTrue(structure.bounds().maxY() > 1);
    }

    /** Проверяет, что реальный редактор помещает декларации в живой виджет сцены. */
    @Test
    void editorViewport_withVanillaScene_displaysNodesAndStartsAssembly() {
        Schema schema = Schema.builtIns();
        EditorSession session = EditorSession.create("""
                <Scene3D width="320" height="200" highlightBlocks="true">
                    <Structure src="minecraft:igloo/top" assemblySeconds="0.5"/>
                    <Block state="minecraft:chest" pos="2,0,0"/>
                </Scene3D>
                """, schema);
        var shell = UiEditor.shell(new XamlEditorDocument("scene", "Scene", session));
        Scene3D widget = findScene(shell.dock().findPane(UiEditor.VIEWPORT).content());
        assertNotNull(widget);
        assertTrue(widget.highlightBlocks());
        var structure = assertInstanceOf(StructureNode.class, widget.scene().roots().get(0));
        assertInstanceOf(BlockNode.class, widget.scene().roots().get(1));
        assertNull(structure.bounds());
        widget.tick(new FrameContext(1, 0.25f, 0, FramePhase.ANIMATION));
        widget.tick(new FrameContext(2, 0.25f, 0, FramePhase.ANIMATION));
        assertEquals(Integer.MAX_VALUE, structure.topVisibleY());
        assertNotNull(structure.bounds());
    }

    /**
     * Ищет виджет в дереве панели после применения отложенных вставок.
     * @param widget корень обхода.
     * @return виджет сцены или null, если в дереве сцены нет.
     */
    private static Scene3D findScene(Widget widget) {
        if (widget instanceof Scene3D scene) return scene;
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) {
            Scene3D scene = findScene(child);
            if (scene != null) return scene;
        }
        return null;
    }
}
