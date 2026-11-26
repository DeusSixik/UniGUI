package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Разрешает геометрию предмета в том же контексте, который использует сцена Minecraft 26.x.
 */
final class SceneItemModel {
    /**
     * Создание экземпляров не требуется.
     */
    private SceneItemModel() {
    }

    /**
     * Читает идентификатор текущей загрузки моделей.
     *
     * @param client клиент Minecraft.
     * @return объект, меняющийся при перезагрузке ресурсов, или null до загрузки моделей.
     */
    static Object generation(Minecraft client) {
        var manager = client.getModelManager();
        return manager == null || client.getItemModelResolver() == null ? null : manager.getBlockStateModelSet();
    }

    /**
     * Заполняет состояние модели контекстом FIXED, текущим миром и seed 0.
     *
     * @param client клиент с загруженными моделями.
     * @param stack  стек, источник компонентов модели.
     * @param out    состояние для отрисовки или чтения границ.
     */
    static void resolve(Minecraft client, ItemStack stack, ItemDisplayContext context, ItemStackRenderState out) {
        client.getItemModelResolver().updateForTopItem(out, stack, context, client.level, null, 0);
    }

    /**
     * Вычисляет границы модели стека после ванильных трансформаций.
     *
     * @param client клиент с загруженными моделями.
     * @param stack  стек, источник компонентов модели.
     * @return локальный AABB или null, если модель не предоставляет границы.
     */
    static Aabb bounds(Minecraft client, ItemStack stack, ItemDisplayContext context) {
        ItemStackRenderState model = new ItemStackRenderState();
        resolve(client, stack, context, model);
        return bounds(model);
    }

    /**
     * Читает AABB всех слоёв после позы отображения и локальных трансформаций слоёв.
     *
     * @param model уже разрешённое состояние модели.
     * @return локальный AABB, включая плоские границы, или null для пустых и некорректных extents.
     */
    static Aabb bounds(ItemStackRenderState model) {
        var box = model.getModelBoundingBox();
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) return null;
        if (minX == maxX && minY == maxY && minZ == maxZ) return null;
        return new Aabb(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
