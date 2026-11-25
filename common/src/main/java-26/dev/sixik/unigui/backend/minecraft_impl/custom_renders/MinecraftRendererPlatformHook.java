package dev.sixik.unigui.backend.minecraft_impl.custom_renders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;

/**
 * Точка расширения платформы для предметов со своим рендером (26.x).
 *
 * <p>В 1.20/1.21 платформа ещё рисовала такие предметы через {@code renderByItem}. В 26.x предметы рисует
 * ванильный GUI (через {@code GuiGraphicsExtractor}), поэтому от платформы нужна только проверка, есть ли у
 * предмета свой рендер.</p>
 */
@Environment(EnvType.CLIENT)
public abstract class MinecraftRendererPlatformHook {

    private static MinecraftRendererPlatformHook instance = new MinecraftRendererPlatformHook() {
    };

    protected boolean hasCustomItemRenderer(ItemStack itemStack) {
        return false;
    }

    public static void set(MinecraftRendererPlatformHook hook) {
        MinecraftRendererPlatformHook.instance = hook;
    }

    public static boolean hasCustomItemRendererImpl(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && instance.hasCustomItemRenderer(itemStack);
    }
}
