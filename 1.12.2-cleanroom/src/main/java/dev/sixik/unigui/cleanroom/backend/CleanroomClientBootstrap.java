package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
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
        UniGuiRuntime.install(current);
    }

    public static CleanroomUiRuntimeBackend backend() {
        CleanroomUiRuntimeBackend current = backend;
        if (current == null) throw new IllegalStateException("Cleanroom client backend is not installed");
        return current;
    }
}
