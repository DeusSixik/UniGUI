package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.widgets.render.ButtonRenderer;

public final class DestinyLikeCheckboxRenders {
    private static final float DOMINION_CHECKBOX_BORDER_WIDTH = 0.16f;
    private static final float DOMINION_CHECKBOX_TEXT_SIZE = 2.2f * 2.0f;
    private static final float DOMINION_CHECKBOX_TEXT_TRACKING = 0.34f;
    private static final ColorView DOMINION_CHECKBOX_TEXT_COLOR = MutableColor.rgba255(245, 247, 255, 255);

    private static final ColorView DOMINION_CHECKBOX_BOX_OFF = MutableColor.rgba255(45, 47, 53, 255);
    private static final ColorView DOMINION_CHECKBOX_BOX_ON = MutableColor.rgba255(24, 76, 43, 255);
    private static final ColorView DOMINION_CHECKBOX_BORDER_OFF = MutableColor.rgba255(126, 129, 138, 255);
    private static final ColorView DOMINION_CHECKBOX_BORDER_ON = MutableColor.rgba255(90, 165, 106, 255);
    private static final ColorView DOMINION_CHECKBOX_BORDER_HOVER = MutableColor.rgba255(255, 255, 255, 255);
    private static final ColorView DOMINION_CHECKBOX_INNER = MutableColor.rgba255(90, 165, 106, 255);

    public static final ButtonRenderer DOMINION_CHECKBOX_RENDERER = (draw, state) -> {
        float boxSize = Math.max(0.0f, state.indicatorSize());
        float innerMaxSize = Math.max(0.0f, state.indicatorInnerSize());
        if (boxSize <= 0.0f || innerMaxSize <= 0.0f) return;

        float progress = clamp01(state.indicatorProgress());
        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()), Math.max(0.0f, state.width() - boxSize - labelGap))
                : 0.0f;
        float boxX = state.labelLeft() ? state.x() + labelWidth + labelGap : state.x();
        float boxY = state.y() + Math.max(0.0f, state.height() - boxSize) * 0.5f;

        draw.rect(boxX, boxY, boxSize, boxSize,
                Paint.fill(mix(DOMINION_CHECKBOX_BOX_OFF, DOMINION_CHECKBOX_BOX_ON, progress)));
        draw.rect(boxX, boxY, boxSize, boxSize,
                Paint.stroke(state.hovered() && state.enabled()
                        ? DOMINION_CHECKBOX_BORDER_HOVER
                        : mix(DOMINION_CHECKBOX_BORDER_OFF, DOMINION_CHECKBOX_BORDER_ON, progress), DOMINION_CHECKBOX_BORDER_WIDTH));

        if (progress > 0.0f) {
            if (state.indeterminate()) {
                float dashWidth = innerMaxSize * progress;
                float dashHeight = Math.max(DOMINION_CHECKBOX_BORDER_WIDTH, innerMaxSize * 0.28f);
                float dashX = boxX + (boxSize - dashWidth) * 0.5f;
                float dashY = boxY + (boxSize - dashHeight) * 0.5f;
                draw.rect(dashX, dashY, dashWidth, dashHeight, Paint.fill(DOMINION_CHECKBOX_INNER));
            } else {
                float innerSize = innerMaxSize * progress;
                float innerX = boxX + (boxSize - innerSize) * 0.5f;
                float innerY = boxY + (boxSize - innerSize) * 0.5f;
                draw.rect(innerX, innerY, innerSize, innerSize, Paint.fill(DOMINION_CHECKBOX_INNER));
            }
        }

        if (state.labelLeft()) {
            drawDominionCheckboxLabel(draw, state, state.x(), labelWidth);
        } else {
            drawDominionCheckboxLabel(draw, state, boxX + boxSize + labelGap);
        }
    };

    public static RichText dominionCheckboxText(String text) {
        return RichText.builder()
                .size(DOMINION_CHECKBOX_TEXT_SIZE)
                .tracking(DOMINION_CHECKBOX_TEXT_TRACKING)
                .uppercase()
                .color(DOMINION_CHECKBOX_TEXT_COLOR)
                .append(text)
                .font(Fonts.defaultFace())
                .build();
    }

    private static void drawDominionCheckboxLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                                  dev.sixik.unigui.widgets.render.ButtonState state,
                                                  float contentX,
                                                  float contentWidth) {
        if (!state.hasText()) return;
        if (contentWidth <= 0.0f) return;

        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        draw.text(state.richText(), contentX, drawY + 0.2f, contentWidth, drawHeight, Paint.fill(state.textColor()));
    }

    private static void drawDominionCheckboxLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                                  dev.sixik.unigui.widgets.render.ButtonState state,
                                                  float contentX) {
        if (!state.hasText()) return;

        float contentWidth = Math.max(0.0f, state.width() - (contentX - state.x()));
        drawDominionCheckboxLabel(draw, state, contentX, contentWidth);
    }

    private static ColorView mix(ColorView from, ColorView to, float amount) {
        float t = clamp01(amount);
        return MutableColor.rgba(
                lerp(from.r(), to.r(), t),
                lerp(from.g(), to.g(), t),
                lerp(from.b(), to.b(), t),
                lerp(from.a(), to.a(), t));
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private DestinyLikeCheckboxRenders() {
    }
}
