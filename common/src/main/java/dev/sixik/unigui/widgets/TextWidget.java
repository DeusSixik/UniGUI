package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Objects;

public class TextWidget extends WidgetBase {
    protected static final float APPROX_CHAR_WIDTH = 6.0f;
    protected static final float LINE_HEIGHT = 10.0f;

    private String text = "";
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean wrap;

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
        context.text(text,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                Paint.fill(color),
                transform());
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
