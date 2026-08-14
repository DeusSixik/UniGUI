package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;

import java.util.Locale;

public final class DestinyLikeProgressBarRenders {

    private static final String DEFAULT_OBJECTIVE_TEXT = "Kill Zombie";
    private static final String DEFAULT_SEASON_RANK_TEXT = "SEASON RANK 84";

    private static final ColorView PADDED_WHITE_COLOR = MutableColor.rgba255(255, 255, 255, 255);
    private static final ColorView PADDED_WHITE_COLOR_ALPHA = MutableColor.rgba255(255, 255, 255, 50);
    private static final ColorView PADDED_FILL_BACKGROUND_COLOR = MutableColor.rgba255(96, 92, 89, 255);
    private static final ColorView PADDED_FILL_COLOR = MutableColor.rgba255(91, 139, 98, 255);

    private static final Paint PADDED_WHITE_PAINT_ALPHA = Paint.fill(PADDED_WHITE_COLOR_ALPHA);
    private static final Paint PADDED_FILL_BACKGROUND_PAINT = Paint.fill(PADDED_FILL_BACKGROUND_COLOR);
    private static final Paint PADDED_FILL_PAINT = Paint.fill(PADDED_FILL_COLOR);

    private static final ColorView BORDER_COLOR = MutableColor.rgba255(105, 109, 112, 255);
    private static final ColorView TRACK_COLOR = MutableColor.rgba255(5, 7, 10, 235);
    private static final ColorView FILL_COLOR = MutableColor.rgba255(166, 160, 105, 245);
    private static final ColorView FILL_EDGE_COLOR = MutableColor.rgba255(214, 207, 145, 90);
    private static final ColorView TEXT_COLOR = MutableColor.rgba255(245, 247, 250, 255);
    private static final ColorView TEXT_SHADOW_COLOR = MutableColor.rgba255(0, 0, 0, 115);

    private static final Paint TRACK_PAINT = Paint.fill(TRACK_COLOR);
    private static final Paint FILL_PAINT = Paint.fill(FILL_COLOR);
    private static final Paint FILL_EDGE_PAINT = Paint.fill(FILL_EDGE_COLOR);
    private static final Paint TEXT_PAINT = Paint.fill(TEXT_COLOR);
    private static final Paint TEXT_SHADOW_PAINT = Paint.fill(TEXT_SHADOW_COLOR);

    private static final ColorView SEASON_LABEL_COLOR = MutableColor.rgba255(218, 223, 229, 255);
    private static final ColorView SEASON_VALUE_COLOR = MutableColor.rgba255(248, 250, 252, 255);
    private static final ColorView SEASON_TRACK_COLOR = MutableColor.rgba255(39, 42, 47, 255);
    private static final ColorView SEASON_FILL_COLOR = MutableColor.rgba255(245, 247, 250, 255);
    private static final ColorView SEASON_BAR_BORDER_COLOR = MutableColor.rgba255(96, 101, 108, 205);

    private static final Paint SEASON_TRACK_PAINT = Paint.fill(SEASON_TRACK_COLOR);
    private static final Paint SEASON_FILL_PAINT = Paint.fill(SEASON_FILL_COLOR);
    private static final Paint SEASON_LABEL_PAINT = Paint.fill(SEASON_LABEL_COLOR);
    private static final Paint SEASON_VALUE_PAINT = Paint.fill(SEASON_VALUE_COLOR);

    private static final float SEASON_TEXT_SIZE = 2.2f;
    private static final float SEASON_BAR_HEIGHT = 1.45f;
    private static final float SEASON_BAR_BORDER_WIDTH = 0.12f;
    private static final float SEASON_TEXT_TO_BAR_GAP = 0.55f;
    private static final float BORDER_WIDTH = 0.16f;
    private static final float TEXT_PADDING_X = 1.15f;
    private static final float VALUE_GAP = 1.5f;
    private static final float VALUE_MIN_WIDTH = 8.0f;
    private static final float FILL_EDGE_WIDTH = 0.12f;
    private static final float PADDED_BORDER_WIDTH = 0.2f;
    private static final float PADDED_FILL_SPACE = 1.2f;
    private static final float PADDED_FILL_SPACE_MUL_2 = PADDED_FILL_SPACE * 2.0f;

    public static final ProgressBarRenderer PROGRESS_BAR_RENDERER = objectiveProgressBar(DEFAULT_OBJECTIVE_TEXT);
    public static final ProgressBarRenderer SEASON_RANK_PROGRESS_BAR_RENDERER =
            seasonRankProgressBar(DEFAULT_SEASON_RANK_TEXT);

    public static final ProgressBarRenderer PADDED_PROGRESS_BAR_RENDERER = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(0.0f, state.width());
        float height = Math.max(0.0f, state.height());
        if (width <= 0.0f || height <= 0.0f) return;

        float innerX = x + PADDED_FILL_SPACE;
        float innerY = y + PADDED_FILL_SPACE;
        float innerWidth = Math.max(0.0f, width - PADDED_FILL_SPACE_MUL_2);
        float innerHeight = Math.max(0.0f, height - PADDED_FILL_SPACE_MUL_2);

        draw.rect(x, y, width, height, PADDED_WHITE_PAINT_ALPHA);
        draw.addLine(x, y + height, x + width, y + height, PADDED_WHITE_COLOR, PADDED_BORDER_WIDTH);
        draw.addLine(x, y, x + width, y, PADDED_WHITE_COLOR, PADDED_BORDER_WIDTH);
        draw.addLine(x, y, x, y + height, PADDED_WHITE_COLOR, PADDED_BORDER_WIDTH);
        draw.addLine(x + width, y, x + width, y + height, PADDED_WHITE_COLOR, PADDED_BORDER_WIDTH);

        if (innerWidth <= 0.0f || innerHeight <= 0.0f) return;

        draw.rect(innerX, innerY, innerWidth, innerHeight, PADDED_FILL_BACKGROUND_PAINT);

        if (state.indeterminate()) {
            float segmentWidth = Math.max(8.0f, innerWidth * 0.32f);
            float travel = innerWidth + segmentWidth;
            float offset = state.indeterminateOffset() - (float) Math.floor(state.indeterminateOffset());
            float segmentX = innerX + offset * travel - segmentWidth;
            float visibleX = Math.max(innerX, segmentX);
            float visibleRight = Math.min(innerX + innerWidth, segmentX + segmentWidth);

            if (visibleRight > visibleX) {
                draw.rect(visibleX, innerY, visibleRight - visibleX, innerHeight, PADDED_FILL_PAINT);
            }
            return;
        }

        float progress = Math.max(0.0f, Math.min(1.0f, state.progress()));
        float fillWidth = Math.max(0.0f, Math.min(innerWidth, innerWidth * progress));
        if (fillWidth > 0.0f) {
            draw.rect(innerX, innerY, fillWidth, innerHeight, PADDED_FILL_PAINT);
        }
    };

    public static ProgressBarRenderer seasonRankProgressBar(String rankText) {
        String label = rankText == null ? "" : rankText;
        return (draw, state) -> {
            float x = state.x();
            float y = state.y();
            float width = Math.max(0.0f, state.width());
            float height = Math.max(0.0f, state.height());
            if (width <= 0.0f || height <= 0.0f) return;

            float barHeight = Math.min(SEASON_BAR_HEIGHT, Math.max(0.8f, height * 0.24f));
            float textSize = Math.max(1.0f, Math.min(4.0f, height - 1.0f));
            float textHeight = Math.max(1.0f, Math.min(height, textSize + 1.0f));
            float groupHeight = textHeight + SEASON_TEXT_TO_BAR_GAP + barHeight;
            float groupY = y + Math.max(0.0f, height - groupHeight) * 0.5f;
            float textY = groupY;
            float barY = textY + textHeight + SEASON_TEXT_TO_BAR_GAP;

            if (barY + barHeight > y + height) {
                barY = y + Math.max(0.0f, height - barHeight);
                textY = Math.max(y, barY - SEASON_TEXT_TO_BAR_GAP - textHeight);
            }

            drawBorder(draw, x, barY, width, barHeight, PADDED_WHITE_COLOR);
            drawSeasonTexts(draw, label, formatSeasonCounter(state.value(), state.max()), x, textY, width, textHeight, true);
            drawSeasonBar(draw, state.progress(), x, barY, width, barHeight);
        };
    }

    public static ProgressBarRenderer superProgressBar(String text, ColorView colorFill) {
        String label = text == null ? "" : text;
        Paint fillPaint = Paint.fill(colorFill == null ? SEASON_FILL_COLOR : colorFill);
        return (draw, state) -> {
            float x = state.x();
            float y = state.y();
            float width = Math.max(0.0f, state.width());
            float height = Math.max(0.0f, state.height());
            if (width <= 0.0f || height <= 0.0f) return;

            float barHeight = Math.min(SEASON_BAR_HEIGHT, Math.max(0.8f, height * 0.24f));
            float textSize = Math.max(1.0f, Math.min(4.0f, height - 1.0f));
            float textHeight = Math.max(1.0f, Math.min(height, textSize + 1.0f));
            float groupHeight = textHeight + SEASON_TEXT_TO_BAR_GAP + barHeight;
            float groupY = y + Math.max(0.0f, height - groupHeight) * 0.5f;
            float textY = groupY;
            float barY = textY + textHeight + SEASON_TEXT_TO_BAR_GAP;

            if (barY + barHeight > y + height) {
                barY = y + Math.max(0.0f, height - barHeight);
                textY = Math.max(y, barY - SEASON_TEXT_TO_BAR_GAP - textHeight);
            }

            drawBorder(draw, x, barY, width, barHeight, PADDED_WHITE_COLOR);
            drawSeasonTexts(draw, label, formatSeasonCounter(state.value(), state.max()), x, textY, width, textHeight, false);
            drawSeasonBar(draw, state.progress(), x, barY, width, barHeight, fillPaint);
        };
    }

    public static ProgressBarRenderer objectiveProgressBar(String objectiveText) {
        String label = objectiveText == null ? "" : objectiveText;
        return (draw, state) -> {
            float x = state.x();
            float y = state.y();
            float width = Math.max(0.0f, state.width());
            float height = Math.max(0.0f, state.height());
            if (width <= 0.0f || height <= 0.0f) return;

            float contentX = x + BORDER_WIDTH;
            float contentY = y + BORDER_WIDTH;
            float contentWidth = Math.max(0.0f, width - BORDER_WIDTH * 2.0f);
            float contentHeight = Math.max(0.0f, height - BORDER_WIDTH * 2.0f);

            draw.rect(x, y, width, height, TRACK_PAINT);
            if (state.indeterminate()) {
                drawIndeterminateFill(draw, state.indeterminateOffset(), contentX, contentY, contentWidth, contentHeight);
            } else {
                drawDeterminateFill(draw, state.progress(), contentX, contentY, contentWidth, contentHeight);
            }

            drawBorder(draw, x, y, width, height);
            drawTexts(draw, label, formatCounter(state.value(), state.max()), x, y, width, height);
        };
    }

    private DestinyLikeProgressBarRenders() {
    }

    private static void drawDeterminateFill(DrawScope draw,
                                            float progress,
                                            float x,
                                            float y,
                                            float width,
                                            float height) {
        if (width <= 0.0f || height <= 0.0f) return;

        float normalizedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        float fillWidth = Math.max(0.0f, Math.min(width, width * normalizedProgress));
        if (fillWidth <= 0.0f) return;

        draw.rect(x, y, fillWidth, height, FILL_PAINT);
        if (fillWidth < width) {
            draw.rect(x + fillWidth - FILL_EDGE_WIDTH, y, FILL_EDGE_WIDTH, height, FILL_EDGE_PAINT);
        }
    }

    private static void drawIndeterminateFill(DrawScope draw,
                                              float indeterminateOffset,
                                              float x,
                                              float y,
                                              float width,
                                              float height) {
        if (width <= 0.0f || height <= 0.0f) return;

        float segmentWidth = Math.max(8.0f, width * 0.34f);
        float travel = width + segmentWidth;
        float offset = indeterminateOffset - (float) Math.floor(indeterminateOffset);
        float segmentX = x + offset * travel - segmentWidth;

        float visibleX = Math.max(x, segmentX);
        float visibleRight = Math.min(x + width, segmentX + segmentWidth);
        if (visibleRight > visibleX) {
            draw.rect(visibleX, y, visibleRight - visibleX, height, FILL_PAINT);
        }
    }

    private static void drawBorder(DrawScope draw,
                                   float x,
                                   float y,
                                   float width,
                                   float height) {
        drawBorder(draw, x, y, width, height, BORDER_COLOR);
    }

    private static void drawBorder(DrawScope draw,
                                   float x,
                                   float y,
                                   float width,
                                   float height,
                                   ColorView color) {
        draw.addLine(x, y, x + width, y, color, BORDER_WIDTH);
        draw.addLine(x, y + height, x + width, y + height, color, BORDER_WIDTH);
        draw.addLine(x, y, x, y + height, color, BORDER_WIDTH);
        draw.addLine(x + width, y, x + width, y + height, color, BORDER_WIDTH);
    }

    private static void drawTexts(DrawScope draw,
                                  String label,
                                  String valueText,
                                  float x,
                                  float y,
                                  float width,
                                  float height) {
        float textSize = Math.max(1.0f, Math.min(10.0f, height - 1.0f));
        float textHeight = Math.max(1.0f, Math.min(height, textSize + 1.0f));
        float textY = y + Math.max(0.0f, height - textHeight) * 0.5f + 1.2f;
        float labelX = x + TEXT_PADDING_X;

        RichText value = RichText.builder().size(textSize).color(TEXT_COLOR).append(valueText).build();
        float valueWidth = Math.max(VALUE_MIN_WIDTH, TextEngine.measureLineWidth(draw.context(), value));
        float valueX = x + Math.max(TEXT_PADDING_X, width - TEXT_PADDING_X - valueWidth);

        if (!label.isEmpty()) {
            RichText objective = RichText.builder().size(textSize).color(TEXT_COLOR).append(label).build();
            float labelWidth = Math.max(0.0f, valueX - VALUE_GAP - labelX);
            if (labelWidth > 0.0f) {
                draw.text(objective, labelX + 0.28f, textY + 0.28f, labelWidth, textHeight, TEXT_SHADOW_PAINT);
                draw.text(objective, labelX, textY, labelWidth, textHeight, TEXT_PAINT);
            }
        }

        draw.text(value, valueX + 0.28f, textY + 0.28f, valueWidth, textHeight, TEXT_SHADOW_PAINT);
        draw.text(value, valueX, textY, valueWidth, textHeight, TEXT_PAINT);
    }

    private static void drawSeasonTexts(DrawScope draw,
                                        String label,
                                        String valueText,
                                        float x,
                                        float y,
                                        float width,
                                        float height,
                                        boolean value) {
        float textSize = Math.max(1.0f, Math.min(10.0f, height - 1.0f));
        float textHeight = Math.max(1.0f, Math.min(height, textSize + 1.0f));

        RichText left = RichText.builder().size(textSize).color(SEASON_VALUE_COLOR).append(label).build();

        RichText right = value ? RichText.builder().size(textSize).color(SEASON_VALUE_COLOR).append(valueText).build() : null;
        float valueWidth = value ? TextEngine.measureLineWidth(draw.context(), right) : 0;
        float valueX = x + Math.max(0.0f, width - valueWidth);

        if (!label.isEmpty()) {
            float labelWidth = Math.max(0.0f, valueX - x - VALUE_GAP);
            if (labelWidth > 0.0f) {
                draw.text(left, x, y, labelWidth, textHeight, SEASON_VALUE_PAINT);
            }
        }

        if(value)
            draw.text(right, valueX, y, valueWidth, textHeight, SEASON_VALUE_PAINT);
    }

    private static void drawSeasonBar(DrawScope draw,
                                      float progress,
                                      float x,
                                      float y,
                                      float width,
                                      float height) {
        drawSeasonBar(draw, progress, x, y, width, height, SEASON_FILL_PAINT);
    }

    private static void drawSeasonBar(DrawScope draw,
                                      float progress,
                                      float x,
                                      float y,
                                      float width,
                                      float height,
                                      Paint fill) {
        if (width <= 0.0f || height <= 0.0f) return;

        draw.rect(x, y, width, height, SEASON_TRACK_PAINT);

        float normalizedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        float fillWidth = Math.max(0.0f, Math.min(width, width * normalizedProgress));
        if (fillWidth > 0.0f) {
            draw.rect(x, y, fillWidth, height, fill);
        }

        draw.addRect(x, y, width, height, SEASON_BAR_BORDER_COLOR, SEASON_BAR_BORDER_WIDTH);
    }

    private static String formatCounter(float value, float max) {
        return formatNumber(value) + "/" + formatNumber(max);
    }

    private static String formatSeasonCounter(float value, float max) {
        return formatGroupedNumber(value) + " / " + formatGroupedNumber(max);
    }

    private static String formatGroupedNumber(float value) {
        if (!Float.isFinite(value)) return "0";
        return String.format(Locale.US, "%,d", Math.round(value));
    }

    private static String formatNumber(float value) {
        if (!Float.isFinite(value)) return "0";
        float rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.001f) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
