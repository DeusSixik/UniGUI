package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.SeparatorRenderer;
import dev.sixik.unigui.widgets.render.SeparatorState;

public final class Separator extends WidgetBase {
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private SeparatorRenderer renderer;
    private Orientation orientation = Orientation.HORIZONTAL;
    private float thickness = 1.0f;

    public Separator() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public MutableColor color() {
        return color;
    }

    public SeparatorRenderer renderer() {
        return renderer;
    }

    public Separator renderer(SeparatorRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Separator useDefaultRenderer() {
        return renderer(null);
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
        pushOpacity(context);
        try {
            effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot());
        } finally {
            popOpacity(context);
        }
    }

    private SeparatorRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(SeparatorRenderer.class, WidgetsRender.separator()) : renderer;
    }

    private SeparatorState snapshot() {
        return new SeparatorState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                orientation,
                thickness,
                color.copy());
    }
}
