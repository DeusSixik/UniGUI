package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class TextInputRenderers {
    public static final TextInputRenderer DEFAULT = (draw, state) -> {
        draw.pushClip(state.viewportX(), state.viewportY(), state.viewportWidth(), state.viewportHeight());
        try {
            if (state.focused() && state.hasSelection() && !state.showingPlaceholder()) {
                float selectionX = state.viewportX() + state.prefixWidth(state.selectionStart()) - state.horizontalScrollPixels();
                float selectionWidth = Math.max(1.0f,
                        state.prefixWidth(state.selectionEnd()) - state.prefixWidth(state.selectionStart()));
                draw.rect(selectionX,
                        state.viewportY(),
                        selectionWidth,
                        state.viewportHeight(),
                        Paint.fill(state.caretColor()));
            }

            if (state.hasVisibleText()) {
                draw.text(state.richText(),
                        state.viewportX() - state.horizontalScrollPixels(),
                        state.textY(),
                        Math.max(state.viewportWidth(), state.measuredTextWidth()),
                        state.textHeight(),
                        Paint.fill(state.showingPlaceholder() ? state.placeholderColor() : state.textColor()));
            }

            if (state.focused()) {
                float caretX = state.viewportX() + state.prefixWidth(state.cursorIndex()) - state.horizontalScrollPixels();
                draw.rect(caretX,
                        state.viewportY(),
                        1.0f,
                        state.viewportHeight(),
                        Paint.fill(state.caretColor()));
            }
        } finally {
            draw.popClip();
        }
    };

    public static final TextInputRenderer SEARCH_FIELD = (draw, state) -> {
        DEFAULT.render(draw, state);
        if (state.clearButtonVisible()) {
            draw.text(dev.sixik.unigui.api.text.RichText.plain("x"),
                    state.clearButtonX(),
                    state.clearButtonY(),
                    state.clearButtonWidth(),
                    state.clearButtonHeight(),
                    Paint.fill(state.placeholderColor()));
        }
    };

    private TextInputRenderers() {
    }
}
