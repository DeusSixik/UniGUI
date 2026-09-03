package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;

/** Общая отрисовка текстовой части визуального контрола. */
public final class LabelPart {
    private LabelPart() {
    }

    /**
     * Рисует текст с ограничением по области контрола.
     *
     * <p>Обрезка находится в одном месте, чтобы разные контролы одинаково обрабатывали
     * длинные подписи и не вызывали backend без необходимости.</p>
     */
    public static void render(DrawScope draw,
                              RichText text,
                              float clipX,
                              float clipY,
                              float clipWidth,
                              float clipHeight,
                              float drawX,
                              float drawY,
                              float drawWidth,
                              float drawHeight,
                              ColorView color) {
        if (draw == null || text == null || text.isEmpty() || color == null
                || clipWidth <= 0.0f || clipHeight <= 0.0f
                || drawWidth <= 0.0f || drawHeight <= 0.0f) {
            return;
        }

        draw.pushTextClip(clipX, clipY, clipWidth, clipHeight);
        try {
            TextEngine.drawInline(draw, text, drawX, drawY,
                    drawWidth, drawHeight, Paint.fill(color));
        } finally {
            draw.popClip();
        }
    }

    /** Возвращает вертикальную координату текста, выровненного по центру области. */
    public static float centeredY(float y, float height, float textHeight) {
        return y + Math.max(0.0f, height - Math.max(0.0f, textHeight)) * 0.5f;
    }
}
