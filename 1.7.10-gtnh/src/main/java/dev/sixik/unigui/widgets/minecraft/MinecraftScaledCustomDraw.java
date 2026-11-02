package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.render.CustomDraw;
import dev.sixik.unigui.api.render.RenderBackend;

/**
 * A custom draw that is told the coordinate scale it runs at; kept for parity with the 1.20 / 1.21 widgets.
 */
public interface MinecraftScaledCustomDraw extends CustomDraw {
    void draw(RenderBackend backend, float scale);

    @Override
    default void draw(RenderBackend backend) {
        draw(backend, 1.0f);
    }
}