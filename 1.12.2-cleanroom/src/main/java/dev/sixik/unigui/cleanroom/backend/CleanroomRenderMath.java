package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.ColorView;

/** Small backend-independent conversions shared by the legacy renderer. */
final class CleanroomRenderMath {
    private CleanroomRenderMath() {
    }

    static int argb(ColorView color, float opacity) {
        if (color == null) return 0;
        int alpha = channel(color.a() * clamp01(opacity));
        return alpha << 24 | channel(color.r()) << 16 | channel(color.g()) << 8 | channel(color.b());
    }

    private static int channel(float value) {
        return Math.round(clamp01(value) * 255.0f);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
