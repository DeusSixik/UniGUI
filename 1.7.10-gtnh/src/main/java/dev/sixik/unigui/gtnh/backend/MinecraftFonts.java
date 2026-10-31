package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;

/**
 * Font faces backed by Minecraft 1.7.10's native FontRenderer.
 */
public final class MinecraftFonts {
    private static final MinecraftFontFace DEFAULT = new MinecraftFontFace("minecraft:default");

    private MinecraftFonts() {
    }

    /**
     * Returns the Minecraft 1.7.10 vanilla font face.
     */
    public static MinecraftFontFace defaultFace() {
        return DEFAULT;
    }

    /**
     * Makes the vanilla font the process-wide default face of the font registry.
     *
     * @return the vanilla face
     */
    public static FontFace useVanillaAsDefault() {
        Fonts.global().defaultFace(DEFAULT);
        return DEFAULT;
    }
}
