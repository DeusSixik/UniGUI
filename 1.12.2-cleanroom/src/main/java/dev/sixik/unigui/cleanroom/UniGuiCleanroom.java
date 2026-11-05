package dev.sixik.unigui.cleanroom;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import dev.sixik.unigui.cleanroom.backend.CleanroomClientBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cleanroom entry point for the Minecraft 1.12.2 UniGUI module.
 */
@Mod(modid = UniGuiCleanroom.MOD_ID, name = "UniGUI", version = UniGuiVersion.MOD_VERSION)
public final class UniGuiCleanroom {

    /** Stable mod identifier declared in {@code mcmod.info}. */
    public static final String MOD_ID = "unigui";

    /** Logger used by the Cleanroom integration during startup. */
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /**
     * Logs that the Minecraft 1.12.2 integration has reached pre-initialization.
     *
     * @param event Forge pre-initialization event supplied by Cleanroom
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("UniGUI Cleanroom integration initialized");
        if (event.getSide() == Side.CLIENT) {
            CleanroomClientBootstrap.install();
        }
    }
}
