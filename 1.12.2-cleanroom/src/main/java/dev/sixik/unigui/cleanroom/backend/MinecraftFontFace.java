package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import net.minecraft.client.Minecraft;

/**
 * The vanilla 1.12.2 {@code FontRenderer} as a UniGUI font face. Text with this face is drawn by the vanilla renderer
 * (scaled by {@code pixelSize / 9}); text with any SDF-capable face is drawn by the SDF renderer instead.
 *
 * <p>Metrics are derived from {@code FONT_HEIGHT}: the baseline sits 7/9 below the glyph top, matching vanilla
 * glyph bitmaps.</p>
 */
public final class MinecraftFontFace implements FontFace {
    /** Height of the vanilla glyph box in pixels at scale 1; a run of pixel size 9 is drawn unscaled. */
    public static final float BASE_SIZE = 9.0f;

    private final String id;

    MinecraftFontFace(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public FontMetrics metrics(float pixelSize) {
        float height = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT * normalizedSize(pixelSize) / BASE_SIZE;
        return new FontMetrics(height * (7.0f / 9.0f), height * (2.0f / 9.0f), 0.0f, height);
    }

    @Override
    public float advance(int codePoint, float pixelSize) {
        String value = new String(Character.toChars(codePoint));
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(value);
        return width * normalizedSize(pixelSize) / BASE_SIZE;
    }

    private static float normalizedSize(float pixelSize) {
        return Float.isFinite(pixelSize) && pixelSize > 0.0f ? pixelSize : BASE_SIZE;
    }
}
