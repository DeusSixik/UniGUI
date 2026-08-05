package dev.sixik.unigui.forge;

import dev.sixik.unigui.UniGui;
import net.minecraftforge.fml.common.Mod;

@Mod(UniGui.MOD_ID)
public final class UniGuiForge {
    public UniGuiForge() {
        // Run our common setup.
        UniGui.init();
    }
}
