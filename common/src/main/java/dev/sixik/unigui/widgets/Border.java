package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.BorderRenderer;
import dev.sixik.unigui.widgets.render.BorderState;

public final class Border extends WidgetBase {
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private BorderRenderer renderer;
    private float thickness = 1.0f;
    private float radius;

    public Border() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public MutableColor color() {
        return color;
    }

    public BorderRenderer renderer() {
        return renderer;
    }

    public Border renderer(BorderRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Border useDefaultRenderer() {
        return renderer(null);
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
            effectiveRenderer().render(new DrawScope(context, transform()), snapshot());
        } finally {
            popOpacity(context);
        }
    }

    private BorderRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(BorderRenderer.class, WidgetsRender.border()) : renderer;
    }

    private BorderState snapshot() {
        return new BorderState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                color.copy(),
                thickness,
                radius);
    }
}
