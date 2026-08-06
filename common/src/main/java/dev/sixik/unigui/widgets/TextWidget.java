package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Objects;

public class TextWidget extends WidgetBase {
    protected static final float APPROX_CHAR_WIDTH = TextEngine.APPROX_CHAR_WIDTH;
    protected static final float LINE_HEIGHT = TextEngine.LINE_HEIGHT;

    private String text = "";
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean wrap;
    private TextOverflowMode overflowMode = TextOverflowMode.VISIBLE;
    private float marqueeSpeed = 24.0f;
    private float marqueeGap = 24.0f;
    private float marqueeOffset;

    public TextWidget() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public TextWidget(String text) {
        this();
        this.text = normalize(text);
    }

    public String text() {
        return text;
    }

    public TextWidget text(String text) {
        String normalized = normalize(text);
        if (Objects.equals(this.text, normalized)) return this;
        this.text = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor color() {
        return color;
    }

    public boolean wrap() {
        return wrap;
    }

    public TextWidget wrap(boolean wrap) {
        if (this.wrap == wrap) return this;
        this.wrap = wrap;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public TextOverflowMode overflowMode() {
        return overflowMode;
    }

    public TextWidget overflowMode(TextOverflowMode overflowMode) {
        TextOverflowMode normalized = overflowMode == null ? TextOverflowMode.VISIBLE : overflowMode;
        if (this.overflowMode == normalized) return this;
        this.overflowMode = normalized;
        marqueeOffset = 0.0f;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextWidget clipOverflow() {
        return overflowMode(TextOverflowMode.CLIP);
    }

    public TextWidget shrinkToFit() {
        return overflowMode(TextOverflowMode.SHRINK_TO_FIT);
    }

    public TextWidget marqueeOnHover() {
        return overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
    }

    public float marqueeSpeed() {
        return marqueeSpeed;
    }

    public TextWidget marqueeSpeed(float marqueeSpeed) {
        float normalized = Math.max(0.0f, marqueeSpeed);
        if (this.marqueeSpeed == normalized) return this;
        this.marqueeSpeed = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float marqueeGap() {
        return marqueeGap;
    }

    public TextWidget marqueeGap(float marqueeGap) {
        float normalized = Math.max(0.0f, marqueeGap);
        if (this.marqueeGap == normalized) return this;
        this.marqueeGap = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, measuredTextWidth(context), measuredTextHeight(context)));
    }

    @Override
    public void render(RenderContext context) {
        if (text.isEmpty()) return;
        pushOpacity(context);
        try {
            switch (overflowMode) {
                case CLIP -> renderClipped(context);
                case SHRINK_TO_FIT -> renderShrinkToFit(context);
                case MARQUEE_ON_HOVER -> renderMarquee(context);
                case VISIBLE -> renderVisible(context);
            }
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (overflowMode != TextOverflowMode.MARQUEE_ON_HOVER || !hovered() || text.isEmpty()) {
            if (marqueeOffset != 0.0f) {
                marqueeOffset = 0.0f;
                invalidate(InvalidationFlags.VISUAL);
            }
            return;
        }

        float textWidth = TextEngine.measureLineWidth(text);
        if (textWidth <= Math.max(0.0f, layoutBounds().width())) {
            if (marqueeOffset != 0.0f) {
                marqueeOffset = 0.0f;
                invalidate(InvalidationFlags.VISUAL);
            }
            return;
        }

        float deltaSeconds = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
        marqueeOffset += marqueeSpeed * deltaSeconds;
        float period = Math.max(1.0f, textWidth + marqueeGap);
        if (marqueeOffset >= period) {
            marqueeOffset %= period;
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    protected Alignment textVerticalAlignment() {
        return Alignment.CENTER;
    }

    private void renderVisible(RenderContext context) {
        TextEngine.draw(context,
                text,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                Paint.fill(color),
                transform(),
                Alignment.START,
                textVerticalAlignment());
    }

    private void renderClipped(RenderContext context) {
        context.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
        renderVisible(context);
        context.popClip();
    }

    private void renderShrinkToFit(RenderContext context) {
        float availableWidth = Math.max(0.0f, layoutBounds().width());
        float availableHeight = Math.max(0.0f, layoutBounds().height());
        float textWidth = TextEngine.measureLineWidth(context, text);
        float scale = textWidth <= 0.0f || availableWidth <= 0.0f ? 1.0f : Math.min(1.0f, availableWidth / textWidth);
        float textHeight = Math.min(availableHeight, LINE_HEIGHT * scale);
        float drawY = TextEngine.alignedStart(layoutBounds().y(), availableHeight, textHeight, textVerticalAlignment());
        Transform scaled = scaledTransform(scale);
        context.pushClip(layoutBounds().x(), layoutBounds().y(), availableWidth, availableHeight);
        context.text(text,
                layoutBounds().x(),
                drawY,
                textWidth,
                LINE_HEIGHT,
                Paint.fill(color),
                scaled);
        context.popClip();
    }

    private void renderMarquee(RenderContext context) {
        float availableWidth = Math.max(0.0f, layoutBounds().width());
        float availableHeight = Math.max(0.0f, layoutBounds().height());
        float textWidth = TextEngine.measureLineWidth(context, text);
        if (textWidth <= availableWidth) {
            renderVisible(context);
            return;
        }

        float textHeight = Math.min(availableHeight, LINE_HEIGHT);
        float drawY = TextEngine.alignedStart(layoutBounds().y(), availableHeight, textHeight, textVerticalAlignment());
        float period = Math.max(1.0f, textWidth + marqueeGap);
        float offset = hovered() ? marqueeOffset % period : 0.0f;
        float firstX = layoutBounds().x() - offset;

        context.pushClip(layoutBounds().x(), layoutBounds().y(), availableWidth, availableHeight);
        context.text(text, firstX, drawY, textWidth, textHeight, Paint.fill(color), transform());
        if (hovered()) {
            context.text(text, firstX + textWidth + marqueeGap, drawY, textWidth, textHeight, Paint.fill(color), transform());
        }
        context.popClip();
    }

    private Transform scaledTransform(float scale) {
        Transform scaled = transform().copy();
        scaled.scale().set(transform().scale().x() * scale, transform().scale().y() * scale);
        return scaled;
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    private float measuredTextWidth(LayoutContext context) {
        if (text.isEmpty()) return 0.0f;
        float intrinsicWidth = maxLineCodePoints() * APPROX_CHAR_WIDTH;
        if (!wrap) return intrinsicWidth;
        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        return Float.isFinite(availableWidth) && availableWidth > 0.0f
                ? Math.min(intrinsicWidth, availableWidth)
                : intrinsicWidth;
    }

    private float measuredTextHeight(LayoutContext context) {
        if (text.isEmpty()) return 0.0f;
        if (!wrap) return textLines().length * LINE_HEIGHT;

        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        if (!Float.isFinite(availableWidth) || availableWidth <= 0.0f) {
            return textLines().length * LINE_HEIGHT;
        }

        int charsPerLine = Math.max(1, (int) Math.floor(availableWidth / APPROX_CHAR_WIDTH));
        int visualLines = 0;
        for (String line : textLines()) {
            int codePoints = line.codePointCount(0, line.length());
            visualLines += Math.max(1, (int) Math.ceil(codePoints / (double) charsPerLine));
        }
        return visualLines * LINE_HEIGHT;
    }

    private int maxLineCodePoints() {
        int max = 0;
        for (String line : textLines()) {
            max = Math.max(max, line.codePointCount(0, line.length()));
        }
        return max;
    }

    private String[] textLines() {
        return text.split("\\R", -1);
    }
}
