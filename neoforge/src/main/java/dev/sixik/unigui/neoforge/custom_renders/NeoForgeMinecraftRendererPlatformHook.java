package dev.sixik.unigui.neoforge.custom_renders;

import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge используется только для проверки кеша: у любого предмета может быть свой рендер.
 */
public final class NeoForgeMinecraftRendererPlatformHook extends MinecraftRendererPlatformHook {
    @Override
    protected boolean hasCustomItemRenderer(ItemStack itemStack) {
        return true;
    }
}
