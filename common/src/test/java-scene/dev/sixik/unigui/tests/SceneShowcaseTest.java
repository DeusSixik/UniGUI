package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftScene;
import dev.sixik.unigui.demo.ui.scene.SceneShowcase;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Проверяет, что общая демонстрация 3D-сцены собирается из строк текущей версии без ошибок.
 */
class SceneShowcaseTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ScenePlatform.install(MinecraftScene.PLATFORM);
    }

    @Test
    void create_modernBlocks_resolvesEveryNodeAndStartsAssembly() {
        Scene3D viewport = SceneShowcase.create(SceneShowcase.Blocks.MODERN);
        var roots = viewport.scene().roots();

        StructureNode house = assertInstanceOf(StructureNode.class, roots.get(0));
        assertSame(MinecraftScene.PLATFORM, house.platform());
        assertSame(Blocks.STONE_BRICKS, ((BlockState) house.block(3, 0, 3)).getBlock());
        assertNull(house.block(3, 1, 0), "door opening");
        assertSame(Blocks.GLOWSTONE, ((BlockState) house.block(3, 8, 3)).getBlock());
        assertNotNull(MinecraftScene.requireState(assertInstanceOf(BlockNode.class, roots.get(1))));
        assertNotNull(assertInstanceOf(ItemNode.class, roots.get(2)).id());
        assertNotNull(assertInstanceOf(EntityNode.class, roots.get(3)).requireNativeEntity(MinecraftScene.PLATFORM));
        assertNotNull(viewport.content());

        assertEquals(-1, house.topVisibleY(), "assembly starts below the lowest layer");
        for (int frame = 1; frame <= 40; frame++) {
            viewport.tick(new FrameContext(frame, 0.25f, 0, FramePhase.ANIMATION));
        }
        assertEquals(Integer.MAX_VALUE, house.topVisibleY());
    }
}
