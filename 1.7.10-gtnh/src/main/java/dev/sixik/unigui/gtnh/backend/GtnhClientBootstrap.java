package dev.sixik.unigui.gtnh.backend;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Version;

/**
 * Client side start-up of the GTNH backend.
 *
 * <p>For now it only probes the OpenGL context on the first rendered frame (the context exists and is current only
 * on the render thread) and logs the LWJGL 3 version in use, which proves that the module runs against the real
 * LWJGL 3 supplied by lwjgl3ify and not against its LWJGL 2 compatibility shim.</p>
 */
public final class GtnhClientBootstrap {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static boolean installed;

    private GtnhClientBootstrap() {
    }

    /**
     * Registers the client hooks. Safe to call more than once; client side only.
     */
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        FMLCommonHandler.instance().bus().register(new FirstFrameProbe());
    }

    /**
     * Probes the GL context once, on the first render tick.
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
        }
    }
}
