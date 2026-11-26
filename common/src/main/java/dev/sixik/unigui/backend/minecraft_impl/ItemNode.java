package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Ванильная 3D-модель предмета с трансформацией для рамки и началом в центре.
 */
public final class ItemNode extends SceneNode {
    /**
     * Локальная зона выбора предмета по умолчанию, в блоках.
     */
    private static final Aabb ITEM_BOUNDS = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
    /**
     * Собственная копия стека с компонентами предмета.
     */
    private ItemStack stack;

    /**
     * Создаёт модель предмета.
     *
     * @param stack исходный стек, копируется.
     */
    public ItemNode(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack").copy();
        setBounds(ITEM_BOUNDS);
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
}
