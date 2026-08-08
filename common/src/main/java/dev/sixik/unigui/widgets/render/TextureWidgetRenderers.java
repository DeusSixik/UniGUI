package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TexturePlacement;

public final class TextureWidgetRenderers {
    public static final TextureWidgetRenderer DEFAULT = (draw, state) -> {
        if (state.texture() == null) return;
        TexturePlacement placement = state.placement();
        if (placement == null) return;
        draw.texture(state.texture(), placement, state.radius(), Paint.fill(state.tint()));
    };

    private TextureWidgetRenderers() {
    }
}
