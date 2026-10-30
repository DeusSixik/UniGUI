package dev.sixik.unigui.cleanroom.backend;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Pixel format conversions for texture uploads. No OpenGL, no Minecraft.
 */
final class CleanroomPixels {
    private CleanroomPixels() {
    }

    /**
     * Converts tightly packed RGBA bytes (red first, rows top to bottom) into the {@code 0xAARRGGBB} ints vanilla's
     * {@code TextureUtil.uploadTexture} expects. The buffer's position and limit are not changed.
     *
     * @param width  image width in pixels, positive
     * @param height image height in pixels, positive
     * @param rgba   at least {@code width * height * 4} readable bytes starting at the buffer's position
     * @return a new array of {@code width * height} ARGB pixels
     * @throws IllegalArgumentException for a non-positive size or a buffer that is too short
     */
    static int[] rgbaToArgb(int width, int height, ByteBuffer rgba) {
        Objects.requireNonNull(rgba, "rgba");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive: " + width + "x" + height);
        }
        long required = (long) width * height * 4L;
        if (required > Integer.MAX_VALUE || rgba.remaining() < required) {
            throw new IllegalArgumentException("RGBA buffer must contain at least " + required + " bytes");
        }
        ByteBuffer source = rgba.slice();
        int pixels = width * height;
        int[] argb = new int[pixels];
        for (int i = 0, offset = 0; i < pixels; i++, offset += 4) {
            int r = source.get(offset) & 0xFF;
            int g = source.get(offset + 1) & 0xFF;
            int b = source.get(offset + 2) & 0xFF;
            int a = source.get(offset + 3) & 0xFF;
            argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return argb;
    }
}
