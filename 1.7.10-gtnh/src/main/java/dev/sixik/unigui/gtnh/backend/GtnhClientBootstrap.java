package dev.sixik.unigui.gtnh.backend;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.demo.ui.DemoAssets;
import dev.sixik.unigui.gtnh.commands.CommandUniGUI;
import dev.sixik.unigui.gtnh.commands.GtnhDemoLauncher;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Version;

/**
 * Installs the Minecraft 1.7.10 (GTNH) client runtime bridge.
 */
@SideOnly(Side.CLIENT)
public final class GtnhClientBootstrap {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final String AUTO_OPEN_PROPERTY = "unigui.autoOpen";
    private static volatile GtnhUiRuntimeBackend backend;

    private GtnhClientBootstrap() {
    }

    /**
     * Installs the runtime backend, the client command and the first-frame GL probe. Safe to call more than once.
     */
    public static synchronized void install() {
        if (backend != null) return;
        GtnhUiRuntimeBackend current = new GtnhUiRuntimeBackend();
        backend = current;
        GtnhClientTasks.markClientThread();
        // The vanilla font is the default face (A3); registering custom fonts must not replace it.
        MinecraftFonts.useVanillaAsDefault();
        DemoAssets.provider(GtnhDemoAssets::load);
        UniGuiRuntime.install(current);
        MinecraftForge.EVENT_BUS.register(new GtnhClientEvents());
        ClientCommandHandler.instance.registerCommand(new CommandUniGUI());
        FMLCommonHandler.instance().bus().register(new GtnhClientTasks.Pump());
        FMLCommonHandler.instance().bus().register(new FirstFrameProbe());
    }

    /**
     * @return the installed backend
     * @throws IllegalStateException if {@link #install()} has not run
     */
    public static GtnhUiRuntimeBackend backend() {
        GtnhUiRuntimeBackend current = backend;
        if (current == null) throw new IllegalStateException("GTNH client backend is not installed");
        return current;
    }

    /**
     * Probes the GL context once, on the first render tick (the context is current only on the render thread), and
     * logs the LWJGL 3 version in use, which shows that the module runs against the real LWJGL 3 supplied by
     * lwjgl3ify and not against its LWJGL 2 compatibility layer.
     */
    public static final class FirstFrameProbe {
        private boolean done;

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent event) {
            if (done || event.phase != TickEvent.Phase.END) return;
            done = true;
            LOGGER.info("UniGUI LWJGL: {}", Version.getVersion());
            GtnhGlCapabilities.get();
            FMLCommonHandler.instance().bus().unregister(this);
            String demo = System.getProperty(AUTO_OPEN_PROPERTY);
            if (demo != null && !demo.isEmpty()) FMLCommonHandler.instance().bus().register(new AutoOpen(demo));
        }
    }

    /**
     * Development aid: opens a demo screen a couple of seconds after start-up ({@code -Dunigui.autoOpen=widgets},
     * set by {@code ./gradlew runClient21 -PuniguiAutoOpen=widgets}), so a smoke test needs no typing in the game.
     */
    public static final class AutoOpen {
        private static final int DELAY_FRAMES = 240;
        private final String demo;
        private int frames;

        AutoOpen(String demo) {
            this.demo = demo;
        }

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.END || ++frames < DELAY_FRAMES) return;
            FMLCommonHandler.instance().bus().unregister(this);
            LOGGER.info("UniGUI auto-opening demo '{}'", demo);
            if (Boolean.getBoolean("unigui.autoOpenViaCommand")) {
                // Same order as typing the command in chat: the command runs, then GuiChat dismisses itself.
                new CommandUniGUI().processCommand(null, new String[]{demo});
                Minecraft.getMinecraft().displayGuiScreen(null);
                return;
            }
            if (!GtnhDemoLauncher.open(demo)) LOGGER.warn("UniGUI unknown demo '{}'", demo);
        }
    }
}
