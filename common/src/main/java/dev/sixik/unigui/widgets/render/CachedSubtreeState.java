package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.widgets.caching.CachedSubtreeMissReason;

public record CachedSubtreeState(
        float x,
        float y,
        float width,
        float height,
        TextureHandle texture,
        ColorView tint,
        boolean debugVisible,
        boolean cacheHit,
        CachedSubtreeMissReason missReason,
        String stateText,
        String statsText,
        float overlayWidth,
        ColorView debugHitColor,
        ColorView debugMissColor,
        ColorView debugTextColor,
        ColorView debugBackgroundColor
) {
}
