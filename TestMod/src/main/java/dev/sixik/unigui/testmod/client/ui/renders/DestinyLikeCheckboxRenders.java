package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.interaction.Checkbox;

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

    public static final WidgetRender DOMINION_CHECKBOX_RENDERER = WidgetRender.of(Checkbox.class, (draw, checkbox) -> {
        RectView bounds = checkbox.layoutBounds();
        float boxSize = Math.max(0.0f, checkbox.boxSize());
        float innerMaxSize = Math.max(0.0f, checkbox.checkSize());
        if (boxSize <= 0.0f || innerMaxSize <= 0.0f) return;

        RichText richText = checkbox.richText();
        boolean hasText = richText != null && !richText.isEmpty();
        float progress = clamp01(checkbox.checkProgress());
        float labelGap = hasText ? Math.max(0.0f, checkbox.textGap()) : 0.0f;
        float labelWidth = hasText
                ? Math.min(Math.max(0.0f, TextEngine.measureLineWidth(draw.context(), richText)), Math.max(0.0f, bounds.width() - boxSize - labelGap))
                : 0.0f;
        float boxX = checkbox.labelLeft() ? bounds.x() + labelWidth + labelGap : bounds.x();
        float boxY = bounds.y() + Math.max(0.0f, bounds.height() - boxSize) * 0.5f;

        draw.rect(boxX, boxY, boxSize, boxSize,
                Paint.fill(mix(DOMINION_CHECKBOX_BOX_OFF, DOMINION_CHECKBOX_BOX_ON, progress)));
        DestinyLikeRenderPrimitives.rectBorder(draw, boxX, boxY, boxSize, boxSize,
                checkbox.hovered() && checkbox.enabled()
                        ? DOMINION_CHECKBOX_BORDER_HOVER
                        : mix(DOMINION_CHECKBOX_BORDER_OFF, DOMINION_CHECKBOX_BORDER_ON, progress),
                DOMINION_CHECKBOX_BORDER_WIDTH);

        if (progress > 0.0f) {
            if (checkbox.indeterminate()) {
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

        if (checkbox.labelLeft()) {
            drawDominionCheckboxLabel(draw, checkbox, bounds.x(), labelWidth);
        } else {
            drawDominionCheckboxLabel(draw, checkbox, boxX + boxSize + labelGap);
        }
    });

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
                                                   Checkbox checkbox,
                                                   float contentX,
                                                   float contentWidth) {
        RichText richText = checkbox.richText();
        if (richText == null || richText.isEmpty()) return;
        if (contentWidth <= 0.0f) return;

        RectView bounds = checkbox.layoutBounds();
        float drawHeight = Math.min(Math.max(0.0f, bounds.height()), Math.max(0.0f, TextEngine.measureTextHeight(draw.context(), richText)));
        float drawY = bounds.y() + Math.max(0.0f, bounds.height() - drawHeight) * 0.5f;
        draw.text(richText, contentX, drawY + 0.2f, contentWidth, drawHeight, Paint.fill(checkbox.textColor()));
    }

    private static void drawDominionCheckboxLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                                   Checkbox checkbox,
                                                   float contentX) {
        RichText richText = checkbox.richText();
        if (richText == null || richText.isEmpty()) return;

        RectView bounds = checkbox.layoutBounds();
        float contentWidth = Math.max(0.0f, bounds.width() - (contentX - bounds.x()));
        drawDominionCheckboxLabel(draw, checkbox, contentX, contentWidth);
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
