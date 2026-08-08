package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;

public record BoxState(
        float x,
        float y,
        float width,
        float height,
        boolean backgroundVisible,
        ColorView background,
        TextureHandle backgroundTexture,
        ColorView backgroundTextureTint,
        TexturePlacement backgroundTexturePlacement,
        ImageFit backgroundTextureFit,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth
) {
}
