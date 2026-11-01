package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.demo.ui.DemoAssets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Installs the Minecraft 1.12.2 client runtime bridge.
 */
@SideOnly(Side.CLIENT)
public final class CleanroomClientBootstrap {
    private static volatile CleanroomUiRuntimeBackend backend;

    private CleanroomClientBootstrap() {
    }

    public static void install() {
        CleanroomUiRuntimeBackend current = backend;
        if (current != null) return;
        current = new CleanroomUiRuntimeBackend();
        backend = current;
        // Fonts.defaultFace() stays the system face (DefaultFontRegistry.registerSystem), as in the modern backend.
        // The vanilla font is opt-in: MinecraftFonts.defaultFace() / MinecraftFonts.useVanillaAsDefault().
        // Screens without an explicit default font still fall back to the vanilla face.
        DemoAssets.provider(CleanroomDemoAssets::load);
        UniGuiRuntime.install(current);
        MinecraftForge.EVENT_BUS.register(new CleanroomClientEvents());
    }

    public static CleanroomUiRuntimeBackend backend() {
        CleanroomUiRuntimeBackend current = backend;
        if (current == null) throw new IllegalStateException("Cleanroom client backend is not installed");
        return current;
    }
}
