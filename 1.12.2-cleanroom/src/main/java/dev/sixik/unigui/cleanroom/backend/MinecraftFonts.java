package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import net.minecraft.client.Minecraft;

/** Font faces backed by Minecraft 1.12.2's native FontRenderer. */
public final class MinecraftFonts {
    private static final float BASE_SIZE = 9.0f;
    private static final FontFace DEFAULT = new FontFace() {
        @Override
        public String id() {
            return "minecraft:default";
        }

        @Override
        public FontMetrics metrics(float pixelSize) {
            float scale = normalizedSize(pixelSize) / BASE_SIZE;
            float height = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT * scale;
            return new FontMetrics(height * (7.0f / 9.0f), height * (2.0f / 9.0f), 0.0f, height);
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            String value = new String(Character.toChars(codePoint));
            int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(value);
            return width * normalizedSize(pixelSize) / BASE_SIZE;
        }
    };

    private MinecraftFonts() {
    }

    /** Returns the Minecraft 1.12.2 vanilla font face. */
    public static FontFace defaultFace() {
        return DEFAULT;
    }

    private static float normalizedSize(float pixelSize) {
        return Float.isFinite(pixelSize) && pixelSize > 0.0f ? pixelSize : BASE_SIZE;
    }
}
