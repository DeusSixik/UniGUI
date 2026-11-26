package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.render.TextureHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Быстрый путь иконок предметов для Minecraft 26.1.
 *
 * <p>В 1.20/1.21 этот класс запекал иконки предметов в собственные текстуры, чтобы не вызывать
 * {@code ItemRenderer} на каждый кадр. В 26.1 так делает сам Minecraft: {@code GuiRenderer} рисует предметы GUI
 * в общий атлас и переиспользует их между кадрами. Поэтому собственный кэш отключён, а предметы всегда идут
 * через ванильный {@code GuiGraphicsExtractor.item}. Класс сохраняет API других версий, чтобы бэкенд не
 * отличался по структуре.</p>
 */
final class FastItemRenderer implements AutoCloseable {
    FastItemRenderer(Minecraft minecraft) {
    }

    void beginFrame() {
    }

    /**
     * @return всегда {@code null}: запечённых иконок в 26.1 нет
     */
    TextureHandle cachedTexture(ItemStack stack, float requestedSize) {
        return null;
    }

    /**
     * @return всегда {@code false}: предметы рисуются ванильным путём
     */
    boolean prefersCachedPath(ItemStack stack) {
        return false;
    }

    /**
     * @return всегда {@code null}
     */
    TextureHandle bakeIfBudget(ItemStack stack, float requestedSize, MinecraftRenderTarget restoreTarget) {
        return null;
    }

    void clear() {
    }

    @Override
    public void close() {
    }
}
