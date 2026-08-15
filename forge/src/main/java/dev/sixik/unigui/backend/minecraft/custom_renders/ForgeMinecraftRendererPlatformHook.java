package dev.sixik.unigui.backend.minecraft.custom_renders;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ForgeMinecraftRendererPlatformHook extends MinecraftRendererPlatformHook {

    @Override
    protected void renderByItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext,
                                PoseStack poseStack, MultiBufferSource multiBufferSource,
                                int i, int j) {
        // Kept as a platform hook point; UniGUI currently uses Forge hook only for cache safety checks.
    }

    @Override
    protected boolean hasCustomItemRenderer(ItemStack itemStack) {
        return true;
    }
}
