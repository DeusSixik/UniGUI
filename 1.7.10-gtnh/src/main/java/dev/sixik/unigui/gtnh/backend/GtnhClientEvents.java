package dev.sixik.unigui.gtnh.backend;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjglx.input.Mouse;

/**
 * Forge event hooks that place UniGUI layers in the frame.
 *
 * <p>HUD: {@link RenderGameOverlayEvent.Post} with {@code ElementType.ALL} is the last thing {@code GuiIngameForge}
 * fires, after hotbar, chat and the player list.</p>
 *
 * <p>Screens: {@link GuiScreenEvent.DrawScreenEvent.Post} fires after the whole {@code drawScreen} chain of the
 * actual screen class, including what subclasses draw after {@code super.drawScreen}. The matching {@code Pre} event
 * starts the tooltip deferral.</p>
 */
public final class GtnhClientEvents {
    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.hideGUI) return;
        ScaledResolution resolution = event.resolution;
        int mouseX = Mouse.getX() * resolution.getScaledWidth() / Math.max(1, minecraft.displayWidth);
        int mouseY = resolution.getScaledHeight()
            - Mouse.getY() * resolution.getScaledHeight() / Math.max(1, minecraft.displayHeight) - 1;
        GtnhClientBootstrap.backend().renderHud(resolution.getScaledWidth(), resolution.getScaledHeight(),
            mouseX, mouseY, event.partialTicks);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onScreenDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GuiScreen screen = event.gui;
        if (GtnhClientBootstrap.backend().hasVisibleOverlay(screen)) GtnhTooltips.begin();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onScreenDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.gui;
        GtnhUiRuntimeBackend backend = GtnhClientBootstrap.backend();
        boolean deferred = GtnhTooltips.deferring();
        GtnhTooltips.end();
        try {
            // The screen closes itself while it is drawn (a click handler calling displayGuiScreen(null)), and Forge
            // still posts the event, with no screen.
            if (screen == null) return;
            // A native UniGUI screen draws its own overlay layers inside drawScreen.
            if (!backend.isNativeScreen(screen)) {
                backend.renderScreen(screen, event.mouseX, event.mouseY, event.renderPartialTicks);
            }
            if (screen instanceof GuiDownloadTerrain) {
                GtnhLoadingHooks.terrainDownload(screen.width, screen.height, event.renderPartialTicks);
            }
        } finally {
            if (deferred) GtnhTooltips.flush();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        GtnhClientBootstrap.backend().releaseKeyRepeat();
        GtnhLoadingHooks.screenChanged();
        // A screen that was replaced mid-draw must not leave queued tooltips behind.
        GtnhTooltips.discard();
    }

    /**
     * 1.7.10 has no "loading finished" call in {@code LoadingScreenRenderer}, so a loaded client world ends the
     * level loading state.
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world != null && event.world.isRemote) GtnhLoadingHooks.done();
    }
}
