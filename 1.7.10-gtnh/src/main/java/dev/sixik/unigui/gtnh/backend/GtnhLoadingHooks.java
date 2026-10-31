package dev.sixik.unigui.gtnh.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry points for the loading-screen mixin and the terrain-download screen hook.
 *
 * <p>Nothing here may throw into the vanilla loading code: a failure only disables the layers for the rest of the
 * session and is logged once.</p>
 */
public final class GtnhLoadingHooks {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static boolean broken;

    private GtnhLoadingHooks() {
    }

    /**
     * 1.7.10 has one entry point for both screens; the title tells them apart ("Saving level" or a loading text).
     */
    public static void levelTitle(String title) {
        if (title != null && title.equals(I18n.format("menu.savingLevel", new Object[0]))) levelSaving(title);
        else levelLoading(title);
    }

    public static void levelLoading(String title) {
        GtnhLoadingScreen.begin(GtnhLoadingScreen.Kind.LEVEL_LOADING, title);
    }

    public static void levelSaving(String title) {
        GtnhLoadingScreen.begin(GtnhLoadingScreen.Kind.LEVEL_SAVING, title);
    }

    public static void message(String text) {
        if (!GtnhLoadingScreen.isLoading()) {
            GtnhLoadingScreen.begin(GtnhLoadingScreen.Kind.LEVEL_LOADING, "");
        }
        GtnhLoadingScreen.message(text);
    }

    public static void progress(int percent) {
        GtnhLoadingScreen.progress(percent);
    }

    public static void done() {
        if (GtnhLoadingScreen.kind() != GtnhLoadingScreen.Kind.TERRAIN_DOWNLOAD) {
            GtnhLoadingScreen.end();
        }
    }

    /**
     * Draws the loading layers over the level loading / saving screen, just before the buffer swap.
     */
    public static void renderLevelLoading() {
        if (!GtnhLoadingScreen.isLoading()) return;
        render();
    }

    /**
     * Called every frame the terrain download screen is current.
     */
    public static void terrainDownload(int width, int height, float partialTick) {
        GtnhLoadingScreen.begin(GtnhLoadingScreen.Kind.TERRAIN_DOWNLOAD, "");
        draw(width, height, partialTick);
    }

    /**
     * Called when the current screen changes: leaving the terrain screen ends its loading state.
     */
    public static void screenChanged() {
        if (GtnhLoadingScreen.kind() == GtnhLoadingScreen.Kind.TERRAIN_DOWNLOAD) {
            GtnhLoadingScreen.end();
        }
    }

    private static void render() {
        if (broken) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            // framebufferRender left a pixel-sized projection behind; layers expect the scaled GUI one.
            minecraft.entityRenderer.setupOverlayRendering();
            ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
            draw(resolution.getScaledWidth(), resolution.getScaledHeight(), 0.0f);
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOGGER.error("UniGUI loading-screen layers disabled after an error", e);
        }
    }

    private static void draw(int width, int height, float partialTick) {
        if (broken) return;
        try {
            GtnhClientBootstrap.backend().renderLoading(width, height, partialTick);
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOGGER.error("UniGUI loading-screen layers disabled after an error", e);
        }
    }
}