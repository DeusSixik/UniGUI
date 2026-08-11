package dev.sixik.unigui.backend.minecraft.custom_renders;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.tests.mixin.ItemRendererAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public abstract class MinecraftRendererPlatformHook {

    private static MinecraftRendererPlatformHook instance = new MinecraftRendererPlatformHook() {
        @Override
        protected void renderByItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext,
                                    PoseStack poseStack, MultiBufferSource multiBufferSource,
                                    int i, int j) {
            final ItemRenderer itemRender = Minecraft.getInstance().getItemRenderer();
            final ItemRendererAccessor accessor = (ItemRendererAccessor) itemRender;
            accessor.getBlockEntityRenderer().renderByItem(itemStack, itemDisplayContext, poseStack, multiBufferSource, i, j);
        }
    };

    protected abstract void renderByItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext,
                                         PoseStack poseStack, MultiBufferSource multiBufferSource,
                                         int i, int j);

    protected boolean hasCustomItemRenderer(ItemStack itemStack) {
        return false;
    }

    public static void set(MinecraftRendererPlatformHook hook) {
        MinecraftRendererPlatformHook.instance = hook;
    }

    public static void renderByItemImpl(ItemStack itemStack, ItemDisplayContext itemDisplayContext,
                                    PoseStack poseStack, MultiBufferSource multiBufferSource,
                                    int i, int j) {
        instance.renderByItem(itemStack, itemDisplayContext, poseStack, multiBufferSource, i, j);
    }

    public static boolean hasCustomItemRendererImpl(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && instance.hasCustomItemRenderer(itemStack);
    }
}
