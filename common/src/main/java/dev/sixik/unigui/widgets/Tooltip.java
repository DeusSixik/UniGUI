package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Tooltip extends Box implements OverlayHostAware {
    private static final float HORIZONTAL_PADDING = 6.0f;
    private static final float VERTICAL_PADDING = 4.0f;
    private static final float DEFAULT_MAX_WIDTH = 220.0f;

    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private Widget anchor;
    private String text = "";
    private float offsetX = 8.0f;
    private float offsetY = 10.0f;
    private float maxWidth = DEFAULT_MAX_WIDTH;

    public Tooltip() {
        backgroundVisible(true);
        borderVisible(true);
        radius(3.0f);
        background().set(0.02f, 0.025f, 0.035f, 0.94f);
        borderColor().set(0.25f, 0.78f, 1.0f, 0.85f);
        enabled(false);
        layout(style -> style
                .position(PositionType.ABSOLUTE)
                .maxWidthPercent(100.0f)
                .overflow(Overflow.HIDDEN));
        textColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Tooltip(Widget anchor, String text) {
        this();
        anchor(anchor);
        text(text);
    }

    public Widget anchor() {
        return anchor;
    }

    public Tooltip anchor(Widget anchor) {
        if (this.anchor == anchor) return this;
        this.anchor = anchor;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public String text() {
        return text;
    }

    public Tooltip text(String text) {
        String normalized = text == null ? "" : text;
        if (Objects.equals(this.text, normalized)) return this;
        this.text = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Tooltip offset(float x, float y) {
        float normalizedX = Float.isFinite(x) ? x : 8.0f;
        float normalizedY = Float.isFinite(y) ? y : 10.0f;
        if (offsetX == normalizedX && offsetY == normalizedY) return this;
        offsetX = normalizedX;
        offsetY = normalizedY;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float maxWidth() {
        return maxWidth;
    }

    public Tooltip maxWidth(float maxWidth) {
        float normalized = Float.isFinite(maxWidth) ? Math.max(HORIZONTAL_PADDING * 2.0f + TextEngine.APPROX_CHAR_WIDTH, maxWidth) : DEFAULT_MAX_WIDTH;
        if (this.maxWidth == normalized) return this;
        this.maxWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor textColor() {
        return textColor;
    }

    public boolean showing() {
        return visibility() == Visibility.VISIBLE && anchor != null && anchor.hovered() && !text.isEmpty();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        if (text.isEmpty()) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }

        float availableWidth = context == null ? maxWidth : Math.max(0.0f, context.availableWidth());
        float widthLimit = availableWidth > 0.0f ? Math.min(maxWidth, availableWidth) : maxWidth;
        float textWidthLimit = Math.max(TextEngine.APPROX_CHAR_WIDTH, widthLimit - HORIZONTAL_PADDING * 2.0f);
        List<String> lines = wrappedLines(textWidthLimit);
        float widestLine = 0.0f;
        for (String line : lines) {
            widestLine = Math.max(widestLine, TextEngine.measureLineWidth(line));
        }

        float width = Math.min(widthLimit, widestLine + HORIZONTAL_PADDING * 2.0f);
        float height = Math.max(1, lines.size()) * TextEngine.LINE_HEIGHT + VERTICAL_PADDING * 2.0f;
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView bounds) {
        if (visibility() == Visibility.COLLAPSED || anchor == null) {
            mutableLayoutBounds().set(bounds.x(), bounds.y(), 0.0f, 0.0f);
            return;
        }

        float width = desiredSize().width();
        float height = desiredSize().height();
        mutableLayoutBounds().set(AbsoluteLayoutEngine.placeBelow(
                bounds, anchor.layoutBounds(), width, height, offsetX, offsetY, true, true));
    }

    @Override
    public void render(RenderContext context) {
        if (!showing()) return;
        super.render(context);
    }

    @Override
    protected void renderContent(RenderContext context) {
        float textX = layoutBounds().x() + HORIZONTAL_PADDING;
        float textY = layoutBounds().y() + VERTICAL_PADDING;
        float textWidth = Math.max(0.0f, layoutBounds().width() - HORIZONTAL_PADDING * 2.0f);
        float textHeight = Math.max(0.0f, layoutBounds().height() - VERTICAL_PADDING * 2.0f);
        context.pushClip(textX, textY, textWidth, textHeight);
        try {
            List<String> lines = wrappedLines(textWidth);
            for (int index = 0; index < lines.size(); index++) {
                float lineY = textY + index * TextEngine.LINE_HEIGHT;
                if (lineY >= textY + textHeight) break;
                TextEngine.draw(context,
                        lines.get(index),
                        textX,
                        lineY,
                        textWidth,
                        TextEngine.LINE_HEIGHT,
                        Paint.fill(textColor),
                        transform(),
                        Alignment.START,
                        Alignment.CENTER);
            }
        } finally {
            context.popClip();
        }
    }

    private List<String> wrappedLines(float textWidthLimit) {
        int maxCodePoints = Math.max(1, (int) Math.floor(Math.max(TextEngine.APPROX_CHAR_WIDTH, textWidthLimit) / TextEngine.APPROX_CHAR_WIDTH));
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            appendWrappedParagraph(lines, paragraph, maxCodePoints);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static void appendWrappedParagraph(List<String> lines, String paragraph, int maxCodePoints) {
        if (paragraph == null || paragraph.isEmpty()) {
            lines.add("");
            return;
        }

        String remaining = paragraph.stripLeading();
        while (remaining.codePointCount(0, remaining.length()) > maxCodePoints) {
            int hardBreak = remaining.offsetByCodePoints(0, maxCodePoints);
            int breakIndex = lastWhitespaceBefore(remaining, hardBreak);
            if (breakIndex <= 0) {
                breakIndex = hardBreak;
            }
            lines.add(remaining.substring(0, breakIndex).stripTrailing());
            remaining = remaining.substring(breakIndex).stripLeading();
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
    }

    private static int lastWhitespaceBefore(String text, int endExclusive) {
        for (int index = Math.min(endExclusive, text.length()) - 1; index > 0; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

}
