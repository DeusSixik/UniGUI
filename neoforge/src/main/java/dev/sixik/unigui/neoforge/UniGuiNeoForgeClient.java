package dev.sixik.unigui.neoforge;

import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import dev.sixik.unigui.neoforge.custom_renders.NeoForgeMinecraftRendererPlatformHook;

public final class UniGuiNeoForgeClient {
    private UniGuiNeoForgeClient() {
    }

    public static void init() {
        MinecraftRendererPlatformHook.set(new NeoForgeMinecraftRendererPlatformHook());
    }
}