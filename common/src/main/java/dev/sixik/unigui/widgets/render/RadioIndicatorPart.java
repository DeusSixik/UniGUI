package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

/** Общая отрисовка круглого индикатора radio button. */
public final class RadioIndicatorPart {
    private RadioIndicatorPart() {
    }

    /** Рисует внешний круг и анимируемый внутренний круг. */
    public static void render(DrawScope draw,
                              float x,
                              float y,
                              float size,
                              float innerSize,
                              float progress,
                              ColorView borderColor,
                              ColorView indicatorColor) {
        if (draw == null || size <= 0.0f) return;

        draw.circle(x, y, size, size, Paint.stroke(borderColor, 1.0f));
        float normalizedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        if (normalizedProgress <= 0.0f || innerSize <= 0.0f) return;

        float inner = innerSize * normalizedProgress;
        float offset = Math.max(0.0f, (size - inner) * 0.5f);
        draw.circle(x + offset, y + offset, inner, inner,
                Paint.fill(indicatorColor));
    }
}
