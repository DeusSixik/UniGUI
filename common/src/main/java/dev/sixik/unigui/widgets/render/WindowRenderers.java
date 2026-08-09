package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class WindowRenderers {
    public static final WindowRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;

        float headerHeight = Math.min(state.headerHeight(), state.height());
        draw.rect(state.x(), state.y(), state.width(), headerHeight, Paint.fill(state.headerColor()));
        draw.line(state.x(), state.y() + state.headerHeight(),
                state.x() + state.width(), state.y() + state.headerHeight(),
                Paint.stroke(state.headerSeparatorColor(), 1.0f));
        if (state.active() || state.dragging()) {
            draw.line(state.x(), state.y(),
                    state.x() + state.width(), state.y(),
                    Paint.stroke(state.titleColor(), state.dragging() ? 2.0f : 1.25f));
        }
        if (state.resizing()) {
            float marker = 7.0f;
            draw.line(state.x() + state.width() - marker, state.y() + state.height(),
                    state.x() + state.width(), state.y() + state.height() - marker,
                    Paint.stroke(state.titleColor(), 1.5f));
            draw.line(state.x() + state.width() - marker * 1.8f, state.y() + state.height(),
                    state.x() + state.width(), state.y() + state.height() - marker * 1.8f,
                    Paint.stroke(state.titleColor(), 1.0f));
        }

        float closeReserved = state.closeButtonVisible() ? state.closeButtonWidth() + 6.0f : 0.0f;
        float titleX = state.x() + state.paddingLeft();
        float titleWidth = Math.max(0.0f, state.width() - state.paddingLeft() - state.paddingRight() - closeReserved);
        draw.pushClip(titleX, state.y(), titleWidth, headerHeight);
        try {
            if (state.title() != null && !state.title().isEmpty()) {
                float drawHeight = Math.min(headerHeight, Math.max(0.0f, state.titleTextHeight()));
                float drawY = state.y() + Math.max(0.0f, headerHeight - drawHeight) * 0.5f;
                draw.text(state.title(), titleX, drawY, titleWidth, drawHeight, Paint.fill(state.titleColor()));
            }
        } finally {
            draw.popClip();
        }
    };

    private WindowRenderers() {
    }
}
