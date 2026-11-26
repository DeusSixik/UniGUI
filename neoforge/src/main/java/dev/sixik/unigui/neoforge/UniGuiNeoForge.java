package dev.sixik.unigui.neoforge;

import dev.sixik.unigui.UniGui;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;

@Mod(UniGui.MOD_ID)
public final class UniGuiNeoForge {
    /**
     * Сторона приходит параметром конструктора: так работает и FML 4 (1.21.1), и FML 11 (26.1), где
     * {@code FMLEnvironment.dist} заменён на {@code getDist()}.
     */
    public UniGuiNeoForge(Dist dist) {
        UniGui.init();
        if (dist.isClient()) {
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