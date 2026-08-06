package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;

/**
 * Small shared text layout helper for retained widgets.
 *
 * <p>The current renderer still delegates glyph drawing to the backend, but widgets should not each guess
 * their own baseline. Keep these constants and alignment math in one place until a richer font metrics
 * backend is wired in.</p>
 */
public final class TextEngine {
    public static final float APPROX_CHAR_WIDTH = 6.0f;
    public static final float LINE_HEIGHT = 10.0f;

    private TextEngine() {
    }

    public static float measureLineWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return text.codePointCount(0, text.length()) * APPROX_CHAR_WIDTH;
    }

    public static void draw(RenderContext context, String text,
                            float x, float y, float width, float height,
                            Paint paint, Transform transform,
                            Alignment horizontalAlignment, Alignment verticalAlignment) {
        if (context == null || text == null || text.isEmpty()) return;

        float textWidth = Math.min(Math.max(0.0f, width), measureLineWidth(text));
        float textHeight = Math.min(Math.max(0.0f, height), LINE_HEIGHT);
        float drawX = alignedStart(x, Math.max(0.0f, width), textWidth, horizontalAlignment);
        float drawY = alignedStart(y, Math.max(0.0f, height), textHeight, verticalAlignment);
        context.text(text, drawX, drawY, Math.max(0.0f, width - (drawX - x)), textHeight, paint, transform);
    }

    private static float alignedStart(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.START : alignment) {
            case CENTER -> start + Math.max(0.0f, available - size) * 0.5f;
            case END -> start + Math.max(0.0f, available - size);
            case START, STRETCH -> start;
        };
    }
}
