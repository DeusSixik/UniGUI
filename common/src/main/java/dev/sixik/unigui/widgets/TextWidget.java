package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Objects;

public class TextWidget extends WidgetBase {
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
}
