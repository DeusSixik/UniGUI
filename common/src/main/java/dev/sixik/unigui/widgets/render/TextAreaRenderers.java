package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.impl.text.TextEngine;

public final class TextAreaRenderers {
    private static final float SCROLLBAR_SIZE = 3.0f;
    private static final float SCROLLBAR_MIN_THUMB = 8.0f;
    private static final MutableColor SCROLLBAR_TRACK = new MutableColor(0.0f, 0.0f, 0.0f, 0.24f);
    private static final MutableColor SCROLLBAR_THUMB = new MutableColor(0.55f, 0.62f, 0.72f, 0.72f);

    public static final TextAreaRenderer DEFAULT = (draw, state) -> {
        renderChrome(draw, state);
        draw.pushTextClip(state.viewportX(), state.viewportY(), state.viewportWidth(), state.viewportHeight());
        try {
            if (state.focused() && state.hasSelection() && !state.showingPlaceholder()) {
                renderSelection(draw, state);
            }

            for (TextAreaLineState line : state.lines()) {
                if (!intersectsViewport(line, state)) continue;
                if (line.hasVisibleText()) {
                    TextEngine.drawInline(draw,
                            line.richText(),
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
        renderScrollbars(draw, state);
    };

    private TextAreaRenderers() {
    }

    private static void renderChrome(DrawScope draw, TextAreaState state) {
        if (state.backgroundVisible()) {
            draw.roundedRect(
                    state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.fill(state.backgroundColor()));
        }
        if (state.borderVisible() && state.borderWidth() > 0.0f) {
            draw.roundedRect(
                    state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth()));
        }
    }

    private static void renderScrollbars(DrawScope draw, TextAreaState state) {
        float contentHeight = state.totalLineCount() * state.lineHeight();
        float maxY = Math.max(0.0f, contentHeight - state.viewportHeight());
        float maxX = Math.max(0.0f, state.measuredTextWidth() - state.viewportWidth());
        if (maxY > 0.0f && state.viewportHeight() > 0.0f) {
            float trackX = state.viewportX() + Math.max(0.0f, state.viewportWidth() - SCROLLBAR_SIZE);
            float thumbHeight = Math.max(SCROLLBAR_MIN_THUMB,
                    state.viewportHeight() * Math.min(1.0f, state.viewportHeight() / Math.max(state.viewportHeight(), contentHeight)));
            float thumbTravel = Math.max(1.0f, state.viewportHeight() - thumbHeight);
            float thumbY = state.viewportY() + thumbTravel * Math.min(1.0f, state.verticalScrollPixels() / maxY);
            draw.rect(trackX, state.viewportY(), SCROLLBAR_SIZE, state.viewportHeight(), Paint.fill(SCROLLBAR_TRACK));
            draw.rect(trackX, thumbY, SCROLLBAR_SIZE, thumbHeight, Paint.fill(SCROLLBAR_THUMB));
        }
        if (maxX > 0.0f && state.viewportWidth() > 0.0f) {
            float trackY = state.viewportY() + Math.max(0.0f, state.viewportHeight() - SCROLLBAR_SIZE);
            float thumbWidth = Math.max(SCROLLBAR_MIN_THUMB,
                    state.viewportWidth() * Math.min(1.0f, state.viewportWidth() / Math.max(state.viewportWidth(), state.measuredTextWidth())));
            float thumbTravel = Math.max(1.0f, state.viewportWidth() - thumbWidth);
            float thumbX = state.viewportX() + thumbTravel * Math.min(1.0f, state.horizontalScrollPixels() / maxX);
            draw.rect(state.viewportX(), trackY, state.viewportWidth(), SCROLLBAR_SIZE, Paint.fill(SCROLLBAR_TRACK));
            draw.rect(thumbX, trackY, thumbWidth, SCROLLBAR_SIZE, Paint.fill(SCROLLBAR_THUMB));
        }
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
            if (selectsLineBreak) {
                selectionEndX = Math.max(selectionEndX, line.measuredWidth() + state.approximateCharWidth());
            }
            if (selectsEmptyLine || selectionEndX <= selectionX) {
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
