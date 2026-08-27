package dev.sixik.unigui.neoforge;

import dev.sixik.unigui.UniGui;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(UniGui.MOD_ID)
public final class UniGuiNeoForge {
    public UniGuiNeoForge() {
        UniGui.init();
        if (FMLEnvironment.dist.isClient()) {
            initClient("dev.sixik.unigui.neoforge.UniGuiNeoForgeClient");
        }
    }

    private static void initClient(String className) {
        try {
            Class.forName(className).getDeclaredMethod("init").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize UniGUI NeoForge client hooks", exception);
        }
    }
}