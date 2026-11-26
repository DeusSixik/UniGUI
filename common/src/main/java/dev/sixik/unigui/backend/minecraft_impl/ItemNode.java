package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Ванильная 3D-модель предмета с трансформацией для рамки и началом в центре.
 */
public final class ItemNode extends SceneNode {
    /**
     * Локальная зона выбора в блоках для моделей без доступных вершин или границ.
     */
    private static final Aabb FALLBACK_BOUNDS = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
    /**
     * Собственная копия стека с компонентами предмета.
     */
    private ItemStack stack;
    /**
     * Набор моделей, по которому вычислены границы, или null до первого расчёта.
     */
    private WeakReference<Object> boundsModels;
    /**
     * Мир разрешённой модели или null для превью без мира.
     */
    private WeakReference<ClientLevel> boundsLevel;
    /**
     * Границы дополнительного меша при последнем расчёте или null при его отсутствии.
     */
    private Aabb boundsMesh;
    /**
     * Границы модели и дополнительного меша в локальных единицах сцены.
     */
    private Aabb cachedBounds;

    /**
     * Создаёт модель предмета.
     *
     * @param stack исходный стек, копируется.
     */
    public ItemNode(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack").copy();
    }

    /**
     * Читает стек для передачи вызывающему коду.
     *
     * @return копия текущего стека.
     */
    public ItemStack stack() {
        return stack.copy();
    }

    /**
     * Обновляет стек и запрашивает перерисовку.
     *
     * @param stack новое состояние предмета, копируется.
     * @return этот узел.
     */
    public ItemNode setStack(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (!ItemStack.matches(this.stack, stack)) {
            this.stack = stack.copy();
            boundsModels = null;
            invalidateContent();
        }
        return this;
    }

    /**
     * Читает стек в синхронном рендер-проходе.
     *
     * @return стек только для чтения.
     */
    ItemStack renderStack() {
        return stack;
    }

    /**
     * Читает локальные границы модели в контексте FIXED, обновляя их после смены стека, мира или ресурсов.
     *
     * @return AABB модели и дополнительного меша, запасной куб для модели без границ или null для пустого узла.
     */
    @Override
    protected Aabb contentBounds() {
        Aabb meshBounds = super.contentBounds();
        if (stack.isEmpty()) return meshBounds;
        Minecraft client = Minecraft.getInstance();
        Object models = client == null ? null : SceneItemModel.generation(client);
        if (models == null) return includeMesh(FALLBACK_BOUNDS, meshBounds);
        ClientLevel level = client.level;
        boolean sameLevel = boundsLevel == null ? level == null : level != null && boundsLevel.get() == level;
        if (boundsModels == null || boundsModels.get() != models || !sameLevel || boundsMesh != meshBounds) {
            Aabb modelBounds = SceneItemModel.bounds(client, stack);
            cachedBounds = includeMesh(modelBounds == null ? FALLBACK_BOUNDS : modelBounds, meshBounds);
            boundsModels = new WeakReference<>(models);
            boundsLevel = level == null ? null : new WeakReference<>(level);
            boundsMesh = meshBounds;
        }
        return cachedBounds;
    }

    /**
     * Объединяет границы, выбирая по каждой оси наименьшую нижнюю и наибольшую верхнюю координату.
     *
     * @param model границы ванильной модели в координатах узла.
     * @param mesh  границы дополнительного меша в той же системе координат или null.
     * @return границы всего рисуемого содержимого.
     */
    private static Aabb includeMesh(Aabb model, Aabb mesh) {
        if (mesh == null) return model;
        return new Aabb(Math.min(model.minX(), mesh.minX()), Math.min(model.minY(), mesh.minY()),
                Math.min(model.minZ(), mesh.minZ()), Math.max(model.maxX(), mesh.maxX()),
                Math.max(model.maxY(), mesh.maxY()), Math.max(model.maxZ(), mesh.maxZ()));
    }
}
