package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

public class Shape extends WidgetBase {
    private Type type = Type.RECT;
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean stroke;
    private float strokeWidth = 1.0f;
    private float radius;

    public Shape() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Type type() {
        return type;
    }

    public Shape type(Type type) {
        Type normalized = type == null ? Type.RECT : type;
        if (this.type == normalized) return this;
        this.type = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor color() {
        return color;
    }

    public boolean stroke() {
        return stroke;
    }

    public Shape stroke(boolean stroke) {
        if (this.stroke == stroke) return this;
        this.stroke = stroke;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float strokeWidth() {
        return strokeWidth;
    }

    public Shape strokeWidth(float strokeWidth) {
        if (this.strokeWidth == strokeWidth) return this;
        this.strokeWidth = strokeWidth;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    public Shape radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        pushOpacity(context);
        try {
            Paint paint = stroke ? Paint.stroke(color, strokeWidth) : Paint.fill(color);
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();

            switch (type) {
                case RECT -> context.rect(x, y, width, height, paint, transform());
                case ROUNDED_RECT -> context.roundedRect(x, y, width, height, radius, paint, transform());
                case CIRCLE -> context.circle(x, y, width, height, paint, transform());
                case LINE -> context.line(x, y, x + width, y + height, paint, transform());
            }
        } finally {
            popOpacity(context);
        }
    }

    public enum Type {
        RECT,
        ROUNDED_RECT,
        CIRCLE,
        LINE
    }
}
