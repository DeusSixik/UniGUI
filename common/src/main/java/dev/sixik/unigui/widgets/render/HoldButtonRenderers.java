package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.math.MutableColor;

public final class HoldButtonRenderers {
    public static final HoldButtonRenderer DEFAULT = (draw, state) -> {
        ControlChromePart.render(draw, state.x(), state.y(), state.width(), state.height(),
                state.radius(), state.backgroundVisible(), state.backgroundColor(),
                state.borderVisible(), state.borderColor(), state.borderWidth());
        float progress = Math.max(0.0f, Math.min(1.0f, state.holdProgress()));
        if (progress > 0.0f) {
            draw.rect(state.x(), state.y(), state.width() * progress, state.height(),
                    Paint.fill(state.holdColor()));
        }
        if (!state.hasText()) return;
        float contentWidth = state.textContentWidth();
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawWidth = Math.min(contentWidth, Math.max(0.0f, state.textWidth()));
        if (contentWidth <= 0.0f || drawWidth <= 0.0f || drawHeight <= 0.0f) return;
        float drawX = state.textContentX() + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        LabelPart.render(draw, state.richText(), state.textContentX(), state.y(),
                contentWidth, state.height(), drawX, drawY, drawWidth, drawHeight, state.textColor());
    };

    private HoldButtonRenderers() {
    }

    public static MutableColor colorWithAlpha(ColorView source, float alphaMultiplier) {
        if (source == null) return new MutableColor(1.0f, 1.0f, 1.0f, Math.max(0.0f, Math.min(1.0f, alphaMultiplier)));
        float alpha = source.a() * Math.max(0.0f, Math.min(1.0f, alphaMultiplier));
        return new MutableColor(source.r(), source.g(), source.b(), alpha);
    }
}
