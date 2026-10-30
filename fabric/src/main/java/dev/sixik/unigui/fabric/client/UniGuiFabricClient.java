package dev.sixik.unigui.fabric.client;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftUiRuntimeBackend;
import net.fabricmc.api.ClientModInitializer;

public final class UniGuiFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UniGuiRuntime.install(new MinecraftUiRuntimeBackend());
    }
}
