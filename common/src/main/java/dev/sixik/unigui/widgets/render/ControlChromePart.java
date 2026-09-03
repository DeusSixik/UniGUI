package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

/** Общая отрисовка фона и рамки визуального контрола. */
public final class ControlChromePart {
    private ControlChromePart() {
    }

    /** Рисует фон и рамку в том же порядке, что и стандартные renderer-ы контролов. */
    public static void render(DrawScope draw,
                              float x,
                              float y,
                              float width,
                              float height,
                              float radius,
                              boolean backgroundVisible,
                              ColorView backgroundColor,
                              boolean borderVisible,
                              ColorView borderColor,
                              float borderWidth) {
        if (draw == null) return;

        if (backgroundVisible && backgroundColor != null) {
            draw.roundedRect(x, y, width, height, Math.max(0.0f, radius),
                    Paint.fill(backgroundColor));
        }
        if (borderVisible && borderColor != null && borderWidth > 0.0f) {
            draw.roundedRect(x, y, width, height, Math.max(0.0f, radius),
                    Paint.stroke(borderColor, borderWidth));
        }
    }
}
