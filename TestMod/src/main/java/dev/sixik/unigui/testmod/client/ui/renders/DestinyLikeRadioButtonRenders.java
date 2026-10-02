package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.interaction.RadioButton;

public final class DestinyLikeRadioButtonRenders {
    private static final float DOMINION_RADIO_BORDER_WIDTH = 0.16f;
    private static final float DOMINION_RADIO_TEXT_SIZE = 2.2f * 2.0f;
    private static final float DOMINION_RADIO_TEXT_TRACKING = 0.34f;
    private static final ColorView DOMINION_RADIO_TEXT_COLOR = MutableColor.rgba255(245, 247, 255, 255);

    private static final ColorView DOMINION_RADIO_OUTER_OFF = MutableColor.rgba255(45, 47, 53, 255);
    private static final ColorView DOMINION_RADIO_OUTER_ON = MutableColor.rgba255(24, 76, 43, 255);
    private static final ColorView DOMINION_RADIO_BORDER_OFF = MutableColor.rgba255(126, 129, 138, 255);
    private static final ColorView DOMINION_RADIO_BORDER_ON = MutableColor.rgba255(90, 165, 106, 255);
    private static final ColorView DOMINION_RADIO_BORDER_HOVER = MutableColor.rgba255(255, 255, 255, 255);
    private static final ColorView DOMINION_RADIO_INNER = MutableColor.rgba255(90, 165, 106, 255);

    public static final WidgetRender DOMINION_RADIO_BUTTON_RENDERER = WidgetRender.of(RadioButton.class, (draw, radioButton) -> {
        RectView bounds = radioButton.layoutBounds();
        float outerSize = Math.max(0.0f, radioButton.outerSize());
        float innerMaxSize = Math.max(0.0f, radioButton.innerSize());
        if (outerSize <= 0.0f || innerMaxSize <= 0.0f) return;

        RichText richText = radioButton.richText();
        boolean hasText = richText != null && !richText.isEmpty();
        float progress = clamp01(radioButton.selectionProgress());
        float labelGap = hasText ? Math.max(0.0f, radioButton.textGap()) : 0.0f;
        float labelWidth = hasText
                ? Math.min(Math.max(0.0f, TextEngine.measureLineWidth(draw.context(), richText)), Math.max(0.0f, bounds.width() - outerSize - labelGap))
                : 0.0f;
        float outerX = radioButton.labelLeft() ? bounds.x() + labelWidth + labelGap : bounds.x();
        float outerY = bounds.y() + Math.max(0.0f, bounds.height() - outerSize) * 0.5f;

        draw.circle(outerX, outerY, outerSize, outerSize,
                Paint.fill(mix(DOMINION_RADIO_OUTER_OFF, DOMINION_RADIO_OUTER_ON, progress)));
        draw.circle(outerX, outerY, outerSize, outerSize,
                Paint.stroke(radioButton.hovered() && radioButton.enabled()
                        ? DOMINION_RADIO_BORDER_HOVER
                        : mix(DOMINION_RADIO_BORDER_OFF, DOMINION_RADIO_BORDER_ON, progress), DOMINION_RADIO_BORDER_WIDTH));

        if (progress > 0.0f) {
            float innerSize = innerMaxSize * progress;
            float innerX = outerX + (outerSize - innerSize) * 0.5f;
            float innerY = outerY + (outerSize - innerSize) * 0.5f;
            draw.circle(innerX, innerY, innerSize, innerSize, Paint.fill(DOMINION_RADIO_INNER));
        }

        if (radioButton.labelLeft()) {
            drawDominionRadioLabel(draw, radioButton, bounds.x(), labelWidth);
        } else {
            drawDominionRadioLabel(draw, radioButton, outerX + outerSize + labelGap);
        }
    });

    public static RichText dominionRadioText(String text) {
        return RichText.builder()
                .size(DOMINION_RADIO_TEXT_SIZE)
                .tracking(DOMINION_RADIO_TEXT_TRACKING)
                .uppercase()
                .color(DOMINION_RADIO_TEXT_COLOR)
                .append(text)
                .font(Fonts.defaultFace())
                .build();
    }

    private static void drawDominionRadioLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                               RadioButton radioButton,
                                               float contentX,
                                               float contentWidth) {
        RichText richText = radioButton.richText();
        if (richText == null || richText.isEmpty()) return;
        if (contentWidth <= 0.0f) return;

        RectView bounds = radioButton.layoutBounds();
        float drawHeight = Math.min(Math.max(0.0f, bounds.height()), Math.max(0.0f, TextEngine.measureTextHeight(draw.context(), richText)));
        float drawY = bounds.y() + Math.max(0.0f, bounds.height() - drawHeight) * 0.5f;
        draw.text(richText, contentX, drawY + 0.2f, contentWidth, drawHeight, Paint.fill(radioButton.textColor()));
    }

    private static void drawDominionRadioLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                               RadioButton radioButton,
                                               float contentX) {
        RichText richText = radioButton.richText();
        if (richText == null || richText.isEmpty()) return;

        RectView bounds = radioButton.layoutBounds();
        float contentWidth = Math.max(0.0f, bounds.width() - (contentX - bounds.x()));
        drawDominionRadioLabel(draw, radioButton, contentX, contentWidth);
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

    private DestinyLikeRadioButtonRenders() {
    }
}