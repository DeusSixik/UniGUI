package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;

public final class DestinyLikeToggleSwitchRenders {

    private static final float DOMINION_SWITCH_BORDER_WIDTH = 0.16f;
    private static final float DOMINION_SWITCH_TEXT_SIZE = 2.2f * 2;
    private static final float DOMINION_SWITCH_TEXT_TRACKING = 0.34f;
    private static final ColorView DOMINION_SWITCH_TEXT_COLOR = MutableColor.rgba255(245, 247, 255, 255);

    private static final ColorView DOMINION_SWITCH_TRACK_OFF = MutableColor.rgba255(45, 47, 53, 255);
    private static final ColorView DOMINION_SWITCH_TRACK_ON = MutableColor.rgba255(24, 76, 43, 255);
    private static final ColorView DOMINION_SWITCH_BORDER_OFF = MutableColor.rgba255(126, 129, 138, 255);
    private static final ColorView DOMINION_SWITCH_BORDER_ON = MutableColor.rgba255(90, 165, 106, 255);
    private static final ColorView DOMINION_SWITCH_BORDER_HOVER = MutableColor.rgba255(255, 255, 255, 255);
    private static final ColorView DOMINION_SWITCH_THUMB_OFF = MutableColor.rgba255(192, 195, 202, 255);
    private static final ColorView DOMINION_SWITCH_THUMB_ON = MutableColor.rgba255(90, 165, 106, 255);

    public static final WidgetRender DOMINION_TOGGLE_SWITCH_RENDERER = WidgetRender.of(ToggleSwitch.class, (draw, toggle) -> {
        RectView bounds = toggle.layoutBounds();
        float trackWidth = Math.max(0.0f, toggle.trackWidth());
        float trackHeight = Math.max(0.0f, toggle.trackHeight());
        float thumbSize = Math.max(0.0f, toggle.thumbSize());
        if (trackWidth <= 0.0f || trackHeight <= 0.0f || thumbSize <= 0.0f) return;

        RichText richText = toggle.richText();
        boolean hasText = richText != null && !richText.isEmpty();
        float progress = clamp01(toggle.switchProgress());
        float labelGap = hasText ? Math.max(0.0f, toggle.labelGap()) : 0.0f;
        float labelWidth = hasText
                ? Math.min(Math.max(0.0f, TextEngine.measureLineWidth(draw.context(), richText)), Math.max(0.0f, bounds.width() - trackWidth - labelGap))
                : 0.0f;
        float trackX = toggle.labelLeft() ? bounds.x() + labelWidth + labelGap : bounds.x();
        float trackY = bounds.y() + Math.max(0.0f, bounds.height() - trackHeight) * 0.5f;

        draw.rect(trackX, trackY, trackWidth, trackHeight,
                Paint.fill(mix(DOMINION_SWITCH_TRACK_OFF, DOMINION_SWITCH_TRACK_ON, progress)));
        DestinyLikeRenderPrimitives.rectBorder(draw, trackX, trackY, trackWidth, trackHeight,
                toggle.hovered() && toggle.enabled()
                        ? DOMINION_SWITCH_BORDER_HOVER
                        : mix(DOMINION_SWITCH_BORDER_OFF, DOMINION_SWITCH_BORDER_ON, progress),
                DOMINION_SWITCH_BORDER_WIDTH);

        float thumbPadding = Math.max(0.24f, (trackHeight - thumbSize) * 0.5f);
        float thumbTravel = Math.max(0.0f, trackWidth - thumbSize - thumbPadding * 2.0f);
        float thumbX = trackX + thumbPadding + thumbTravel * progress;
        float thumbY = trackY + Math.max(0.0f, trackHeight - thumbSize) * 0.5f;
        draw.rect(thumbX, thumbY, thumbSize, thumbSize,
                Paint.fill(mix(DOMINION_SWITCH_THUMB_OFF, DOMINION_SWITCH_THUMB_ON, progress)));

        if (toggle.labelLeft()) {
            drawDominionSwitchLabel(draw, toggle, bounds.x(), labelWidth);
        } else {
            drawDominionSwitchLabel(draw, toggle, trackX + trackWidth + labelGap);
        }
    });

    public static RichText dominionSwitchText(String text) {
        return RichText.builder()
                .size(DOMINION_SWITCH_TEXT_SIZE)
                .tracking(DOMINION_SWITCH_TEXT_TRACKING)
                .uppercase()
                .color(DOMINION_SWITCH_TEXT_COLOR)
                .append(text)
                .font(Fonts.defaultFace())
                .build();
    }

    private static void drawDominionSwitchLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                                ToggleSwitch toggle,
                                                float contentX,
                                                float contentWidth) {
        RichText richText = toggle.richText();
        if (richText == null || richText.isEmpty() || contentWidth <= 0.0f) return;

        RectView bounds = toggle.layoutBounds();
        float drawHeight = Math.min(Math.max(0.0f, bounds.height()), Math.max(0.0f, TextEngine.measureTextHeight(draw.context(), richText)));
        float drawY = bounds.y() + Math.max(0.0f, bounds.height() - drawHeight) * 0.5f;
        draw.text(richText, contentX, drawY + 0.2f, contentWidth, drawHeight, Paint.fill(toggle.textColor()));
    }

    private static void drawDominionSwitchLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                                ToggleSwitch toggle,
                                                float contentX) {
        RichText richText = toggle.richText();
        if (richText == null || richText.isEmpty()) return;

        RectView bounds = toggle.layoutBounds();
        float contentWidth = Math.max(0.0f, bounds.width() - (contentX - bounds.x()));
        drawDominionSwitchLabel(draw, toggle, contentX, contentWidth);
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

}
