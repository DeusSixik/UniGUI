package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry points for the loading-screen mixin and the terrain-download screen hook.
 *
 * <p>Nothing here may throw into the vanilla loading code: a failure only disables the layers for the rest of the
 * session and is logged once.</p>
 */
public final class CleanroomLoadingHooks {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static boolean broken;

    private CleanroomLoadingHooks() {
    }

    public static void levelLoading(String title) {
        CleanroomLoadingScreen.begin(CleanroomLoadingScreen.Kind.LEVEL_LOADING, title);
    }

    public static void levelSaving(String title) {
        CleanroomLoadingScreen.begin(CleanroomLoadingScreen.Kind.LEVEL_SAVING, title);
    }

    public static void message(String text) {
        if (!CleanroomLoadingScreen.isLoading()) {
            CleanroomLoadingScreen.begin(CleanroomLoadingScreen.Kind.LEVEL_LOADING, "");
        }
        CleanroomLoadingScreen.message(text);
    }

    public static void progress(int percent) {
        CleanroomLoadingScreen.progress(percent);
    }

    public static void done() {
        if (CleanroomLoadingScreen.kind() != CleanroomLoadingScreen.Kind.TERRAIN_DOWNLOAD) {
            CleanroomLoadingScreen.end();
        }
    }

    /** Draws the loading layers over the level loading / saving screen, just before the buffer swap. */
    public static void renderLevelLoading() {
        if (!CleanroomLoadingScreen.isLoading()) return;
        render();
    }

    /** Called every frame the terrain download screen is current. */
    public static void terrainDownload(int width, int height, float partialTick) {
        CleanroomLoadingScreen.begin(CleanroomLoadingScreen.Kind.TERRAIN_DOWNLOAD, "");
        draw(width, height, partialTick);
    }

    /** Called when the current screen changes: leaving the terrain screen ends its loading state. */
    public static void screenChanged() {
        if (CleanroomLoadingScreen.kind() == CleanroomLoadingScreen.Kind.TERRAIN_DOWNLOAD) {
            CleanroomLoadingScreen.end();
        }
    }

    private static void render() {
        if (broken) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            // framebufferRender left a pixel-sized projection behind; layers expect the scaled GUI one.
            minecraft.entityRenderer.setupOverlayRendering();
            ScaledResolution resolution = new ScaledResolution(minecraft);
            draw(resolution.getScaledWidth(), resolution.getScaledHeight(), 0.0f);
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOGGER.error("UniGUI loading-screen layers disabled after an error", e);
        }
    }

    private static void draw(int width, int height, float partialTick) {
        if (broken) return;
        try {
            CleanroomClientBootstrap.backend().renderLoading(width, height, partialTick);
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOGGER.error("UniGUI loading-screen layers disabled after an error", e);
        }
    }
}