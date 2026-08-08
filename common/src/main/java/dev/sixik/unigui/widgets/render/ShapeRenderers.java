package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.Shape;

public final class ShapeRenderers {
    public static final ShapeRenderer DEFAULT = (draw, state) -> {
        Paint paint = state.stroke()
                ? Paint.stroke(state.color(), state.strokeWidth())
                : Paint.fill(state.color());

        switch (state.type() == null ? Shape.Type.RECT : state.type()) {
            case RECT -> draw.rect(state.x(), state.y(), state.width(), state.height(), paint);
            case ROUNDED_RECT -> draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(), paint);
            case CIRCLE -> draw.circle(state.x(), state.y(), state.width(), state.height(), paint);
            case LINE -> draw.line(state.x(), state.y(), state.x() + state.width(), state.y() + state.height(), paint);
        }
    };

    private ShapeRenderers() {
    }
}
