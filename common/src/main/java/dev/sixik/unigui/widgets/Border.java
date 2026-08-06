package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

public final class Border extends WidgetBase {
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private float thickness = 1.0f;
    private float radius;

    public Border() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public MutableColor color() {
        return color;
    }

    public float thickness() {
        return thickness;
    }

    public Border thickness(float thickness) {
        if (this.thickness == thickness) return this;
        this.thickness = thickness;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    public Border radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        pushOpacity(context);
        try {
            context.roundedRect(
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height(),
                    radius,
                    Paint.stroke(color, thickness),
                    transform());
        } finally {
            popOpacity(context);
        }
    }
}
