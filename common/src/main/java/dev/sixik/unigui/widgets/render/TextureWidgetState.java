package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;

public record TextureWidgetState(
        float x,
        float y,
        float width,
        float height,
        TextureHandle texture,
        RectView source,
        ImageFit fit,
        float radius,
        ColorView tint,
        TexturePlacement placement
) {
}
