package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import net.minecraft.client.Minecraft;

/**
 * The vanilla 1.7.10 {@code FontRenderer} as a UniGUI font face. Text with this face is drawn by the vanilla renderer
 * (scaled by {@code pixelSize / 9}); text with any SDF-capable face is drawn by the SDF renderer instead.
 *
 * <p>Metrics are derived from {@code FONT_HEIGHT}: the baseline sits 7/9 below the glyph top, matching vanilla
 * glyph bitmaps.</p>
 */
public final class MinecraftFontFace implements FontFace {
    /**
     * Height of the vanilla glyph box in pixels at scale 1; a run of pixel size 9 is drawn unscaled.
     */
    public static final float BASE_SIZE = 9.0f;

    private final String id;

    MinecraftFontFace(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    // The layout asks for the metrics of every run of every text command each frame: remember the last answer.
    private float lastMetricsSize = Float.NaN;
    private int lastMetricsFontHeight = -1;
    private FontMetrics lastMetrics;

    @Override
    public FontMetrics metrics(float pixelSize) {
        int fontHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        float size = normalizedSize(pixelSize);
        FontMetrics cached = lastMetrics;
        if (cached != null && size == lastMetricsSize && fontHeight == lastMetricsFontHeight) return cached;
        float height = fontHeight * size / BASE_SIZE;
        cached = new FontMetrics(height * (7.0f / 9.0f), height * (2.0f / 9.0f), 0.0f, height);
        lastMetrics = cached;
        lastMetricsSize = size;
        lastMetricsFontHeight = fontHeight;
        return cached;
    }

    @Override
    public float advance(int codePoint, float pixelSize) {
        // Called for every glyph of every vanilla text each frame: ask for the width of the char itself instead of
        // building a one-character string. Code points outside the BMP are not drawable by this font anyway.
        int width = codePoint <= Character.MAX_VALUE
            ? Minecraft.getMinecraft().fontRenderer.getCharWidth((char) codePoint)
            : Minecraft.getMinecraft().fontRenderer.getStringWidth(new String(Character.toChars(codePoint)));
        return width * normalizedSize(pixelSize) / BASE_SIZE;
    }

    private static float normalizedSize(float pixelSize) {
        return Float.isFinite(pixelSize) && pixelSize > 0.0f ? pixelSize : BASE_SIZE;
    }
}
