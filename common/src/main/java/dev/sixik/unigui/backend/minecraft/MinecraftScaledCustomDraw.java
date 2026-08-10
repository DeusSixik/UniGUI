package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.render.CustomDraw;
import dev.sixik.unigui.api.render.RenderBackend;

interface MinecraftScaledCustomDraw extends CustomDraw {
    void draw(RenderBackend backend, float scale);

    @Override
    default void draw(RenderBackend backend) {
        draw(backend, 1.0f);
    }
}
