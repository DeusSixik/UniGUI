package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

final class DestinyLikeRenderPrimitives {
    private DestinyLikeRenderPrimitives() {
    }

    static void rectBorder(DrawScope draw,
                           float x,
                           float y,
                           float width,
                           float height,
                           ColorView color,
                           float strokeWidth) {
        if (draw == null || color == null || width <= 0.0f || height <= 0.0f) return;
        draw.rect(x, y, width, height, Paint.stroke(color, Math.max(0.01f, strokeWidth)));
    }
}
