package dev.sixik.unigui.backend.shared;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Platform-independent pixel format conversions shared by the legacy backends. */
public final class PixelFormats {
    private PixelFormats() {
    }

    /**
     * Converts tightly packed RGBA bytes (red first, rows top to bottom) to {@code 0xAARRGGBB} pixels.
     * The buffer's position and limit are not changed.
     */
    public static int[] rgbaToArgb(int width, int height, ByteBuffer rgba) {
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
