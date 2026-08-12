package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.PathRenderer;
import dev.sixik.unigui.widgets.render.PathState;

public final class Path extends WidgetBase {
    private final VectorPath path = new VectorPath();
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private PathRenderer renderer;
    private boolean stroke = true;
    private float strokeWidth = 1.0f;

    public Path() {
        path.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public VectorPath path() {
        return path;
    }

    public MutableColor color() {
        return color;
    }

    public PathRenderer renderer() {
        return renderer;
    }

    public Path renderer(PathRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Path useDefaultRenderer() {
        return renderer(null);
    }

    public boolean stroke() {
        return stroke;
    }

    public Path stroke(boolean stroke) {
        if (this.stroke == stroke) return this;
        this.stroke = stroke;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float strokeWidth() {
        return strokeWidth;
    }

    public Path strokeWidth(float strokeWidth) {
        if (this.strokeWidth == strokeWidth) return this;
        this.strokeWidth = strokeWidth;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        if (path.isEmpty()) return;
        pushOpacity(context);
        try {
            effectiveRenderer().render(new DrawScope(context, transform()), snapshot());
        } finally {
            popOpacity(context);
        }
    }

    private PathRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(PathRenderer.class, WidgetsRender.path()) : renderer;
    }

    private PathState snapshot() {
        return new PathState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                path.copy(),
                color.copy(),
                stroke,
                strokeWidth);
    }
}
