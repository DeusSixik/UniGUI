package dev.sixik.unigui.fabric;

import dev.sixik.unigui.UniGui;
import net.fabricmc.api.ModInitializer;

public final class UniGuiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        UniGui.init();
    }
}
