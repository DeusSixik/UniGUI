package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;

public class Box extends PanelWidget {
    private final MutableColor background = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private final MutableColor borderColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean backgroundVisible;
    private boolean borderVisible;
    private float borderWidth = 1.0f;
    private float radius;

    public Box() {
        background.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        borderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public MutableColor background() {
        return background;
    }

    public Box backgroundVisible(boolean backgroundVisible) {
        if (this.backgroundVisible == backgroundVisible) return this;
        this.backgroundVisible = backgroundVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    public MutableColor borderColor() {
        return borderColor;
    }

    public Box borderVisible(boolean borderVisible) {
        if (this.borderVisible == borderVisible) return this;
        this.borderVisible = borderVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean borderVisible() {
        return borderVisible;
    }

    public float borderWidth() {
        return borderWidth;
    }

    public Box borderWidth(float borderWidth) {
        if (this.borderWidth == borderWidth) return this;
        this.borderWidth = borderWidth;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    public Box radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        renderBox(context);
        renderContent(context);
    }

    protected void renderBox(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();

        if (backgroundVisible) {
            context.roundedRect(x, y, width, height, radius, Paint.fill(background), transform());
        }

        if (borderVisible) {
            context.roundedRect(x, y, width, height, radius, Paint.stroke(borderColor, borderWidth), transform());
        }
    }

    protected void renderContent(RenderContext context) {
        super.render(context);
    }
}
