package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** Проверяет ID сущностей, которые ванильный рендерер читает при разборе модели предмета. */
final class EntityNodeTest {
    /** Загружает ванильные реестры без клиента. */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** Проверяет, что фабрика создаёт превью с уникальными ID, доступными рендереру предметов. */
    @Test
    void previewEntity_outsideWorld_hasDistinctIdsForVanillaItemResolver() {
        Level level = previewLevel();
        var first = MinecraftClientCompat.previewEntity(MinecraftClientCompat.ZOMBIE, level);
        var second = MinecraftClientCompat.previewEntity(MinecraftClientCompat.ZOMBIE, level);

        var model = new ItemStackRenderState();
        new ItemModelResolver(null).updateForLiving(model, ItemStack.EMPTY, ItemDisplayContext.HEAD, first);
        assertTrue(model.isEmpty());
        assertTrue(first.getId() < 0);
        assertTrue(second.getId() < 0);
        assertNotEquals(first.getId(), second.getId());
    }

    /** Проверяет назначение отсутствующего ID и сохранение ID игровой сущности. */
    @Test
    void constructor_forExternalEntity_assignsOnlyMissingId() {
        var entity = MinecraftClientCompat.previewEntity(MinecraftClientCompat.ZOMBIE, previewLevel());
        entity.setId(0);
        assertSame(entity, new EntityNode(entity).entity());
        int previewId = entity.getId();
        assertTrue(previewId < 0);
        assertEquals(previewId, new EntityNode(entity).entity().getId());

        entity.setId(42);
        assertEquals(42, new EntityNode(entity).entity().getId());
    }

    /**
     * Создаёт окружение для фабрики сущности. Сам зомби и его логика ID остаются ванильными.
     * @return заглушка уровня с набором возможностей базовой игры.
     */
    private static Level previewLevel() {
        Level level = mock(Level.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(level.enabledFeatures()).thenReturn(FeatureFlags.DEFAULT_FLAGS);
        return level;
    }
}
