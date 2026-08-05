package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

public final class Separator extends WidgetBase {
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private Orientation orientation = Orientation.HORIZONTAL;
    private float thickness = 1.0f;

    public Separator() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public MutableColor color() {
        return color;
    }

    public Orientation orientation() {
        return orientation;
    }

    public Separator orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float thickness() {
        return thickness;
    }

    public Separator thickness(float thickness) {
        if (this.thickness == thickness) return this;
        this.thickness = thickness;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = orientation == Orientation.HORIZONTAL ? layoutBounds().width() : thickness;
        float height = orientation == Orientation.HORIZONTAL ? thickness : layoutBounds().height();
        context.rect(x, y, width, height, Paint.fill(color), transform());
    }
}
