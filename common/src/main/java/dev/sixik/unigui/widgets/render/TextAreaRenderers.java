package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

public final class TextAreaRenderers {
    public static final TextAreaRenderer DEFAULT = (draw, state) -> {
        draw.pushClip(state.viewportX(), state.viewportY(), state.viewportWidth(), state.viewportHeight());
        try {
            if (state.focused() && state.hasSelection() && !state.showingPlaceholder()) {
                renderSelection(draw, state);
            }

            for (TextAreaLineState line : state.lines()) {
                if (!intersectsViewport(line, state)) continue;
                if (line.hasVisibleText()) {
                    draw.text(line.richText(),
                            state.viewportX() - state.horizontalScrollPixels(),
                            line.y(),
                            Math.max(state.viewportWidth(), line.measuredWidth()),
                            state.textHeight(),
                            Paint.fill(state.showingPlaceholder() ? state.placeholderColor() : state.textColor()));
                }
            }

            if (state.focused()) {
                TextAreaLineState caretLine = state.cursorLine();
                if (caretLine != null) {
                    int localCursor = Math.max(0, Math.min(caretLine.length(), state.cursorIndex() - caretLine.startIndex()));
                    float caretX = state.viewportX() + caretLine.prefixWidth(localCursor) - state.horizontalScrollPixels();
                    draw.rect(caretX,
                            caretLine.y(),
                            1.0f,
                            caretLine.height(),
                            Paint.fill(state.caretColor()));
                }
            }
        } finally {
            draw.popClip();
        }
    };

    private TextAreaRenderers() {
    }

    private static void renderSelection(DrawScope draw, TextAreaState state) {
        int selectionStart = Math.min(state.selectionStart(), state.selectionEnd());
        int selectionEnd = Math.max(state.selectionStart(), state.selectionEnd());
        for (TextAreaLineState line : state.lines()) {
            if (!intersectsViewport(line, state)) continue;

            boolean intersectsText = selectionStart < line.endIndex() && selectionEnd > line.startIndex();
            boolean selectsLineBreak = line.lineIndex() < state.totalLineCount() - 1
                    && selectionStart <= line.endIndex()
                    && selectionEnd > line.endIndex();
            boolean selectsEmptyLine = line.startIndex() == line.endIndex()
                    && selectionStart <= line.startIndex()
                    && selectionEnd > line.startIndex();
            if (!intersectsText && !selectsLineBreak && !selectsEmptyLine) continue;

            int localStart = clamp(selectionStart - line.startIndex(), 0, line.length());
            int localEnd = intersectsText ? clamp(selectionEnd - line.startIndex(), 0, line.length()) : localStart;
            float selectionX = line.prefixWidth(localStart);
            float selectionEndX = line.prefixWidth(localEnd);
            if (selectsLineBreak || selectsEmptyLine || selectionEndX <= selectionX) {
                selectionEndX = selectionX + state.approximateCharWidth();
            }
            draw.rect(state.viewportX() + selectionX - state.horizontalScrollPixels(),
                    line.y(),
                    Math.max(1.0f, selectionEndX - selectionX),
                    line.height(),
                    Paint.fill(state.caretColor()));
        }
    }

    private static boolean intersectsViewport(TextAreaLineState line, TextAreaState state) {
        return line.y() + line.height() >= state.viewportY()
                && line.y() <= state.viewportY() + state.viewportHeight();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
