package dev.sixik.unigui.forge;

import dev.sixik.unigui.UniGui;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(UniGui.MOD_ID)
public final class UniGuiForge {
    public UniGuiForge() {
        UniGui.init();
        if (FMLEnvironment.dist.isClient()) {
            initClient("dev.sixik.unigui.forge.UniGuiForgeClient");
        }
    }

    private static void initClient(String className) {
        try {
            Class.forName(className).getDeclaredMethod("init").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize UniGUI Forge client hooks", exception);
        }
    }
}