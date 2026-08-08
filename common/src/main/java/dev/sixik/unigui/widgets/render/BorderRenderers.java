package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class BorderRenderers {
    public static final BorderRenderer DEFAULT = (draw, state) ->
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.color(), state.thickness()));

    private BorderRenderers() {
    }
}
