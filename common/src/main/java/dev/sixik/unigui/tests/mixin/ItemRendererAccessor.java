package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {

    @Accessor
    BlockEntityWithoutLevelRenderer getBlockEntityRenderer();

    @Invoker("hasAnimatedTexture")
    static boolean ug$hasAnimatedTexture(ItemStack arg) {
        return true;
    }
}
