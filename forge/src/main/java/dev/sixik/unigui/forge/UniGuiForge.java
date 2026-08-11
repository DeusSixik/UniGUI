package dev.sixik.unigui.forge;

import dev.sixik.unigui.UniGui;
import dev.sixik.unigui.backend.minecraft.custom_renders.ForgeMinecraftRendererPlatformHook;
import dev.sixik.unigui.backend.minecraft.custom_renders.MinecraftRendererPlatformHook;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(UniGui.MOD_ID)
public final class UniGuiForge {
    public UniGuiForge() {
        // Run our common setup.
        UniGui.init();

        if(FMLEnvironment.dist.isClient()) {
            MinecraftRendererPlatformHook.set(new ForgeMinecraftRendererPlatformHook());
        }
    }
}
