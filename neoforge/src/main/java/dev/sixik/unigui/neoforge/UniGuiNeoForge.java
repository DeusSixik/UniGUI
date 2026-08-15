package dev.sixik.unigui.neoforge;

import dev.sixik.unigui.UniGui;
import dev.sixik.unigui.backend.minecraft.custom_renders.MinecraftRendererPlatformHook;
import dev.sixik.unigui.backend.minecraft.custom_renders.NeoForgeMinecraftRendererPlatformHook;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(UniGui.MOD_ID)
public final class UniGuiNeoForge {
    public UniGuiNeoForge() {
        UniGui.init();

        if (FMLEnvironment.dist.isClient()) {
            MinecraftRendererPlatformHook.set(new NeoForgeMinecraftRendererPlatformHook());
        }
    }
}
