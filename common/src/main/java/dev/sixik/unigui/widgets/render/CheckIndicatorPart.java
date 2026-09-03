package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

/** Общая отрисовка квадратного индикатора checkbox. */
public final class CheckIndicatorPart {
    private CheckIndicatorPart() {
    }

    /** Рисует рамку индикатора и его состояние checked/indeterminate. */
    public static void render(DrawScope draw,
                              float x,
                              float y,
                              float size,
                              float innerSize,
                              boolean checked,
                              boolean indeterminate,
                              ColorView borderColor,
                              ColorView indicatorColor) {
        if (draw == null || size <= 0.0f) return;

        draw.roundedRect(x, y, size, size, 2.0f,
                Paint.stroke(borderColor, 1.0f));

        if (indeterminate) {
            float dashWidth = Math.max(1.0f, innerSize);
            float dashHeight = Math.max(1.0f, innerSize * 0.28f);
            float offsetX = Math.max(0.0f, (size - dashWidth) * 0.5f);
            float offsetY = Math.max(0.0f, (size - dashHeight) * 0.5f);
            draw.rect(x + offsetX, y + offsetY, dashWidth, dashHeight,
                    Paint.fill(indicatorColor));
        } else if (checked) {
            float inner = Math.max(0.0f, innerSize);
            float offset = Math.max(0.0f, (size - inner) * 0.5f);
            if (inner > 0.0f) {
                draw.rect(x + offset, y + offset, inner, inner,
                        Paint.fill(indicatorColor));
            }
        }
    }
}
