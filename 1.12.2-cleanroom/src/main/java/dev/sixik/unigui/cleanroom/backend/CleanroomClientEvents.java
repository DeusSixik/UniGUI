package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.cleanroom.commands.CleanroomDemoLauncher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Forge event hooks that place UniGUI layers in the frame.
 *
 * <p>HUD: {@code GuiIngameForge.renderGameOverlay} replaces the vanilla method without calling it, so a mixin on
 * {@code GuiIngame.renderGameOverlay} never runs under Forge; {@link RenderGameOverlayEvent.Post} with
 * {@code ElementType.ALL} is the last thing {@code GuiIngameForge} fires, after hotbar, chat and the player list.</p>
 *
 * <p>Screens: {@link GuiScreenEvent.DrawScreenEvent.Post} fires after the whole {@code drawScreen} chain of the
 * actual screen class, including what subclasses draw after {@code super.drawScreen}. A mixin on
 * {@code GuiScreen.drawScreen} would sit under that. The matching {@code Pre} event starts the tooltip deferral.</p>
 */
public final class CleanroomClientEvents {
    /**
     * Reports failures when running deferred client commands.
     */
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    /**
     * Client commands waiting for the end of the tick in submission order.
     */
    private static final Queue<Runnable> COMMANDS = new ConcurrentLinkedQueue<>();
    /**
     * Development demo to open once the main menu is ready, or null for ordinary launches.
     */
    private String startupDemo = System.getProperty("unigui.autoOpen");

    /**
     * Defers a client command until chat has finished handling the Enter key.
     *
     * @param command client-side action to run.
     */
    public static void runAfterChat(Runnable command) {
        COMMANDS.add(command);
    }

    /**
     * Runs deferred commands and opens the requested development demo from the main menu.
     *
     * @param event client tick and its current phase.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Runnable command;
        while ((command = COMMANDS.poll()) != null) {
            try {
                command.run();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("UniGUI client command failed", failure);
            }
        }
        if (startupDemo == null || !(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu)) return;
        String demo = startupDemo;
        startupDemo = null;
        if (!CleanroomDemoLauncher.open(demo)) LOGGER.warn("UniGUI unknown demo '{}'", demo);
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.hideGUI) return;
        ScaledResolution resolution = event.getResolution();
        int mouseX = Mouse.getX() * resolution.getScaledWidth() / Math.max(1, minecraft.displayWidth);
        int mouseY = resolution.getScaledHeight()
                - Mouse.getY() * resolution.getScaledHeight() / Math.max(1, minecraft.displayHeight) - 1;
        CleanroomClientBootstrap.backend().renderHud(resolution.getScaledWidth(), resolution.getScaledHeight(),
                mouseX, mouseY, event.getPartialTicks());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onScreenDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GuiScreen screen = event.getGui();
        if (CleanroomClientBootstrap.backend().hasVisibleOverlay(screen)) CleanroomTooltips.begin();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onScreenDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();
        CleanroomUiRuntimeBackend backend = CleanroomClientBootstrap.backend();
        boolean deferred = CleanroomTooltips.deferring();
        CleanroomTooltips.end();
        try {
            // A native UniGUI screen draws its own overlay layers inside drawScreen.
            if (!backend.isNativeScreen(screen)) {
                backend.renderScreen(screen, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
            }
            if (screen instanceof GuiDownloadTerrain) {
                CleanroomLoadingHooks.terrainDownload(screen.width, screen.height, event.getRenderPartialTicks());
            }
        } finally {
            if (deferred) CleanroomTooltips.flush();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        CleanroomClientBootstrap.backend().releaseKeyRepeat();
        CleanroomLoadingHooks.screenChanged();
        // A screen that was replaced mid-draw must not leave queued tooltips behind.
        CleanroomTooltips.discard();
    }
}
