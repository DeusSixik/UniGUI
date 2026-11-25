package dev.sixik.unigui.forge.custom_renders;

import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import net.minecraft.world.item.ItemStack;

public class ForgeMinecraftRendererPlatformHook extends MinecraftRendererPlatformHook {

    @Override
    protected boolean hasCustomItemRenderer(ItemStack itemStack) {
        return true;
    }
}
