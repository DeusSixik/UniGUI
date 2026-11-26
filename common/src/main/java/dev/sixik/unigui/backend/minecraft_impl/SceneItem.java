package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.ItemNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.Objects;

/**
 * Разрешённый предмет {@link dev.sixik.unigui.api.scene.ItemNode}: собственная копия стека и кэш границ его
 * модели.
 */
final class SceneItem {
    /**
     * Локальная зона выбора в блоках для моделей без доступных вершин или границ.
     */
    private static final Aabb FALLBACK_BOUNDS = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);

    /**
     * Собственная копия стека с компонентами предмета.
     */
    private final ItemStack stack;
    /**
     * Набор моделей, по которому вычислены границы, или null до первого расчёта.
     */
    private WeakReference<Object> boundsModels;
    /**
     * Мир разрешённой модели или null для превью без мира.
     */
    private WeakReference<ClientLevel> boundsLevel;
    /**
     * Границы модели в локальных единицах сцены.
     */
    private final EnumMap<ItemNode.Display, Aabb> cachedBounds = new EnumMap<>(ItemNode.Display.class);

    /**
     * Запоминает стек. Вызывающий код передаёт копию, которую больше не меняет.
     *
     * @param stack стек предмета.
     */
    SceneItem(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    /**
     * Читает стек для рендера без копирования.
     *
     * @return собственный стек узла.
     */
    ItemStack stack() {
        return stack;
    }

    /**
     * Вычисляет границы модели в расположении. Результат пересчитывается после перезагрузки моделей или смены
     * мира.
     *
     * @param display расположение модели.
     * @return локальные границы, null для пустого стека.
     */
    Aabb bounds(ItemNode.Display display) {
        if (stack.isEmpty()) return null;
        Minecraft client = Minecraft.getInstance();
        Object models = client == null ? null : SceneItemModel.generation(client);
        if (models == null) return FALLBACK_BOUNDS;
        ClientLevel level = client.level;
        boolean sameLevel = boundsLevel == null ? level == null : level != null && boundsLevel.get() == level;
        if (boundsModels == null || boundsModels.get() != models || !sameLevel) {
            cachedBounds.clear();
            boundsModels = new WeakReference<>(models);
            boundsLevel = level == null ? null : new WeakReference<>(level);
        }
        return cachedBounds.computeIfAbsent(display, key -> {
            Aabb modelBounds = SceneItemModel.bounds(client, stack, MinecraftScene.context(key));
            return modelBounds == null ? FALLBACK_BOUNDS : modelBounds;
        });
    }
}
