package dev.sixik.unigui.backend.minecraft.custom_renders;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.tests.mixin.ItemRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class ForgeMinecraftRendererPlatformHook extends MinecraftRendererPlatformHook {

    @Override
    protected void renderByItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext,
                                PoseStack poseStack, MultiBufferSource multiBufferSource,
                                int i, int j) {
        IClientItemExtensions.of(itemStack).getCustomRenderer()
                .renderByItem(itemStack, itemDisplayContext, poseStack, multiBufferSource, i, j);
    }

    @Override
    protected boolean hasCustomItemRenderer(ItemStack itemStack) {
        try {
            BlockEntityWithoutLevelRenderer renderer = IClientItemExtensions.of(itemStack).getCustomRenderer();
            BlockEntityWithoutLevelRenderer vanillaRenderer = ((ItemRendererAccessor) Minecraft.getInstance()
                    .getItemRenderer()).getBlockEntityRenderer();
            return renderer != vanillaRenderer;
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }
}
