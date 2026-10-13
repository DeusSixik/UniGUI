package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;

@XmlWidgetName("Path")
public final class Path extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.PATH;

    private final VectorPath path = new VectorPath();
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
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

    public boolean stroke() {
        return stroke;
    }

    @XmlAttribute(value = "stroke", category = "Appearance", defaultValue = "true", description = "Whether the vector path is stroked instead of filled.")
    public Path stroke(boolean stroke) {
        if (this.stroke == stroke) return this;
        this.stroke = stroke;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float strokeWidth() {
        return strokeWidth;
    }

    @XmlAttribute(value = "strokeWidth", category = "Appearance", defaultValue = "1", description = "Path stroke width in UI pixels.")
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
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderCustomVisual(draw)) {
                return;
            }
            Paint paint = stroke
                    ? Paint.stroke(color, strokeWidth)
                    : Paint.fill(color);
            draw.path(path, layoutBounds().x(), layoutBounds().y(),
                    layoutBounds().width(), layoutBounds().height(), paint);
        } finally {
            popOpacity(context);
        }
    }
}
