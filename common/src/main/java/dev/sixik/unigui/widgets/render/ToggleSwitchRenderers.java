package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

/** Стандартные renderer'ы toggle switch. */
public final class ToggleSwitchRenderers {
    /** Стандартный renderer с track, thumb и label. */
    public static final ToggleSwitchRenderer DEFAULT = ToggleSwitchRenderers::renderDefault;

    private ToggleSwitchRenderers() {
    }

    /** Создаёт переходник для старого ButtonRenderer. */
    public static ToggleSwitchRenderer legacy(ButtonRenderer renderer) {
        if (renderer == null) return null;
        return (draw, state) -> renderer.render(draw, state.toLegacyButtonState());
    }

    private static void renderDefault(DrawScope draw, ToggleSwitchRenderState state) {
        if (state == null) return;
        float trackWidth = Math.max(0.0f, state.trackWidth());
        float trackHeight = Math.max(0.0f, state.trackHeight());
        float thumbSize = Math.max(0.0f, state.thumbSize());
        if (trackWidth <= 0.0f || trackHeight <= 0.0f || thumbSize <= 0.0f) return;

        float labelGap = state.hasText() ? Math.max(0.0f, state.labelGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()),
                Math.max(0.0f, state.width() - trackWidth - labelGap))
                : 0.0f;
        float trackX = state.labelLeft()
                ? state.x() + labelWidth + labelGap
                : state.x();
        float trackY = state.y() + Math.max(0.0f, state.height() - trackHeight) * 0.5f;
        draw.roundedRect(trackX, trackY, trackWidth, trackHeight, trackHeight * 0.5f,
                Paint.fill(state.trackColor()));

        float thumbPadding = Math.max(1.0f, (trackHeight - thumbSize) * 0.5f);
        float thumbTravel = Math.max(0.0f, trackWidth - thumbSize - thumbPadding * 2.0f);
        float thumbX = trackX + thumbPadding + (state.checked() ? thumbTravel : 0.0f);
        float thumbY = trackY + Math.max(0.0f, trackHeight - thumbSize) * 0.5f;
        draw.circle(thumbX, thumbY, thumbSize, thumbSize, Paint.fill(state.thumbColor()));

        if (!state.hasText()) return;
        float contentX;
        float contentWidth;
        if (state.labelLeft()) {
            contentX = state.x();
            contentWidth = labelWidth;
        } else {
            contentX = trackX + trackWidth + labelGap;
            contentWidth = Math.max(0.0f, state.width() - (contentX - state.x()));
        }
        float drawHeight = Math.min(Math.max(0.0f, state.height()),
                Math.max(0.0f, state.textHeight()));
        if (contentWidth <= 0.0f || drawHeight <= 0.0f) return;
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        LabelPart.render(draw, state.richText(), contentX, state.y(), contentWidth, state.height(),
                contentX, drawY, contentWidth, drawHeight, state.textColor());
    }
}
