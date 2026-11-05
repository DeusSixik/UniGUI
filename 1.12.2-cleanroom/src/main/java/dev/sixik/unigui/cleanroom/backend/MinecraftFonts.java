package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;

/**
 * Font faces backed by Minecraft 1.12.2's native FontRenderer.
 */
public final class MinecraftFonts {
    private static final MinecraftFontFace DEFAULT = new MinecraftFontFace("minecraft:default");

    private MinecraftFonts() {
    }

    /**
     * Returns the Minecraft 1.12.2 vanilla font face.
     */
    public static MinecraftFontFace defaultFace() {
        return DEFAULT;
    }

    /**
     * Makes the vanilla font the process-wide default face of the font registry, so {@code Fonts.defaultFace()}
     * returns it. Opt-in: the backend does not call this at startup, and without it {@code Fonts.defaultFace()}
     * is the system face from {@code DefaultFontRegistry.registerSystem}.
     *
     * @return the vanilla face
     */
    public static FontFace useVanillaAsDefault() {
        Fonts.global().defaultFace(DEFAULT);
        return DEFAULT;
    }
}
