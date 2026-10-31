package dev.sixik.unigui.gtnh;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dev.sixik.unigui.gtnh.backend.GtnhClientBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GTNH (Minecraft 1.7.10) entry point of the UniGUI module.
 */
@Mod(modid = UniGuiGtnh.MOD_ID, name = "UniGUI", version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public final class UniGuiGtnh {

    /**
     * Stable mod identifier declared in {@code mcmod.info}.
     */
    public static final String MOD_ID = "unigui";

    /**
     * Logger used by the GTNH integration during startup.
     */
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /**
     * Logs that the Minecraft 1.7.10 integration has reached pre-initialization.
     *
     * @param event FML pre-initialization event
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("UniGUI GTNH 1.7.10 integration initialized");
        if (event.getSide().isClient()) {
            GtnhClientBootstrap.install();
        }
    }
}
