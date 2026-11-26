package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.Scene;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Проверяет чтение границ из разрешённого состояния предмета в Minecraft 26.x.
 */
final class SceneItemModelTest {
    /**
     * Проверяет запасные границы до загрузки модели и их сброс для пустого стека.
     */
    @Test
    void bounds_withoutClient_fallsBackUntilStackBecomesEmpty() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ScenePlatform.install(MinecraftScene.PLATFORM);
        ItemNode node = ItemNode.of(new ItemStack(Holder.direct(Items.DIAMOND_SWORD, DataComponentMap.EMPTY)));
        Scene scene = new Scene().addRoot(node);
        long revision = scene.revision();

        assertEquals(new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f), node.bounds());
        node.setItem(ItemStack.EMPTY);

        assertEquals(revision + 1, scene.revision());
        assertNull(node.bounds());
    }

    /**
     * Проверяет объединение двух слоёв с сохранением малой толщины по Z.
     */
    @Test
    void bounds_thinModelWithLayerTransforms_includesBothLayers() {
        ItemStackRenderState model = new ItemStackRenderState();
        var first = model.newLayer();
        first.setExtents(() -> new Vector3fc[]{
                new Vector3f(0.25f, 0, 0.46875f), new Vector3f(0.75f, 1, 0.53125f)});
        first.setLocalTransform(new Matrix4f().translation(0.25f, 0, 0));
        first.setItemTransform(new ItemTransform(new Vector3f(), new Vector3f(0.125f, 0.25f, 0.5f),
                new Vector3f(2, 0.5f, 0.5f)));
        var second = model.newLayer();
        second.setExtents(() -> new Vector3fc[]{new Vector3f(0.25f, 0.5f, 1), new Vector3f(0.5f, 0.75f, 1)});

        assertEquals(new Aabb(-0.25f, 0, 0.484375f, 1.125f, 0.5f, 0.515625f), SceneItemModel.bounds(model));
    }

    /**
     * Проверяет, что плоский квад сохраняет нулевую толщину.
     */
    @Test
    void bounds_flatModel_keepsZeroThickness() {
        ItemStackRenderState model = new ItemStackRenderState();
        model.newLayer().setExtents(() -> new Vector3fc[]{new Vector3f(0, 0, 0.5f), new Vector3f(1, 1, 0.5f)});

        assertEquals(new Aabb(-0.5f, -0.5f, 0, 0.5f, 0.5f, 0), SceneItemModel.bounds(model));
    }

    /**
     * Проверяет отсутствие границ, когда модель не предоставляет вершины.
     */
    @Test
    void bounds_noExtents_returnsNull() {
        ItemStackRenderState model = new ItemStackRenderState();
        model.newLayer();

        assertNull(SceneItemModel.bounds(model));
    }
}
