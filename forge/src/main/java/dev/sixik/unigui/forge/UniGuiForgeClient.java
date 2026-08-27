package dev.sixik.unigui.forge;

import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import dev.sixik.unigui.forge.custom_renders.ForgeMinecraftRendererPlatformHook;

public final class UniGuiForgeClient {
    private UniGuiForgeClient() {
    }

    public static void init() {
        MinecraftRendererPlatformHook.set(new ForgeMinecraftRendererPlatformHook());
    }
}