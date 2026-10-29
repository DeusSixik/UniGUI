package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;

@XmlWidgetName("GridOverlay")
public class GridOverlay extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.GRID_OVERLAY;

    private static final float DEFAULT_SPACING = 16.0f;
    private static final float DEFAULT_LINE_THICKNESS = 1.0f;
    private static final float DEFAULT_MAJOR_LINE_THICKNESS = 1.25f;
    private static final int DEFAULT_MAJOR_EVERY = 4;
    private static final int MAX_RENDERED_LINES_PER_AXIS = 4096;

    private final MutableColor lineColor = new MutableColor(0.32f, 0.48f, 0.62f, 0.24f);
    private final MutableColor majorLineColor = new MutableColor(0.32f, 0.78f, 1.0f, 0.40f);
    private boolean gridVisible = true;
    private boolean snapEnabled = true;
    private float spacing = DEFAULT_SPACING;
    private float snapSize = DEFAULT_SPACING;
    private int majorEvery = DEFAULT_MAJOR_EVERY;
    private float offsetX;
    private float offsetY;
    private float lineThickness = DEFAULT_LINE_THICKNESS;
    private float majorLineThickness = DEFAULT_MAJOR_LINE_THICKNESS;

    public GridOverlay() {
        focusable(false);
        mouseCursor(MouseCursor.DEFAULT);
        lineColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        majorLineColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public boolean gridVisible() {
        return gridVisible;
    }

    @XmlAttribute(value = "gridVisible", category = "Behavior", defaultValue = "true", description = "Whether the design canvas grid lines are rendered.")
    public GridOverlay gridVisible(boolean gridVisible) {
        if (this.gridVisible == gridVisible) return this;
        this.gridVisible = gridVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float spacing() {
        return spacing;
    }

    @XmlAttribute(value = "spacing", category = "Layout", defaultValue = "16", description = "Distance between minor grid lines in UI pixels.")
    public GridOverlay spacing(float spacing) {
        float normalized = positiveOrDefault(spacing, DEFAULT_SPACING);
        if (this.spacing == normalized) return this;
        this.spacing = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int majorEvery() {
        return majorEvery;
    }

    @XmlAttribute(value = "majorEvery", category = "Layout", defaultValue = "4", description = "Number of minor grid intervals between emphasized major grid lines; 0 disables major lines.")
    public GridOverlay majorEvery(int majorEvery) {
        int normalized = Math.max(0, majorEvery);
        if (this.majorEvery == normalized) return this;
        this.majorEvery = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float offsetX() {
        return offsetX;
    }

    @XmlAttribute(value = "offsetX", category = "Layout", defaultValue = "0", description = "Horizontal grid origin offset in local UI pixels.")
    public GridOverlay offsetX(float offsetX) {
        float normalized = finiteOrZero(offsetX);
        if (this.offsetX == normalized) return this;
        this.offsetX = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float offsetY() {
        return offsetY;
    }

    @XmlAttribute(value = "offsetY", category = "Layout", defaultValue = "0", description = "Vertical grid origin offset in local UI pixels.")
    public GridOverlay offsetY(float offsetY) {
        float normalized = finiteOrZero(offsetY);
        if (this.offsetY == normalized) return this;
        this.offsetY = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean snapEnabled() {
        return snapEnabled;
    }

    @XmlAttribute(value = "snapEnabled", category = "Behavior", defaultValue = "true", description = "Whether snap helper methods quantize coordinates to the snap size.")
    public GridOverlay snapEnabled(boolean snapEnabled) {
        this.snapEnabled = snapEnabled;
        return this;
    }

    public float snapSize() {
        return snapSize;
    }

    @XmlAttribute(value = "snapSize", category = "Layout", defaultValue = "16", description = "Coordinate interval used by snap helper methods.")
    public GridOverlay snapSize(float snapSize) {
        this.snapSize = positiveOrDefault(snapSize, DEFAULT_SPACING);
        return this;
    }

    public float lineThickness() {
        return lineThickness;
    }

    @XmlAttribute(value = "lineThickness", category = "Appearance", defaultValue = "1", description = "Minor grid line stroke thickness in UI pixels.")
    public GridOverlay lineThickness(float lineThickness) {
        float normalized = positiveOrDefault(lineThickness, DEFAULT_LINE_THICKNESS);
        if (this.lineThickness == normalized) return this;
        this.lineThickness = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float majorLineThickness() {
        return majorLineThickness;
    }

    @XmlAttribute(value = "majorLineThickness", category = "Appearance", defaultValue = "1.25", description = "Major grid line stroke thickness in UI pixels.")
    public GridOverlay majorLineThickness(float majorLineThickness) {
        float normalized = positiveOrDefault(majorLineThickness, DEFAULT_MAJOR_LINE_THICKNESS);
        if (this.majorLineThickness == normalized) return this;
        this.majorLineThickness = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor lineColor() {
        return lineColor;
    }

    @XmlAttribute(value = "lineColor", category = "Appearance", defaultValue = "#527A9E3D", description = "Minor grid line color.")
    public GridOverlay lineColor(ColorView color) {
        if (color != null) lineColor.set(color);
        return this;
    }

    public MutableColor majorLineColor() {
        return majorLineColor;
    }

    @XmlAttribute(value = "majorLineColor", category = "Appearance", defaultValue = "#52C7FF66", description = "Major grid line color.")
    public GridOverlay majorLineColor(ColorView color) {
        if (color != null) majorLineColor.set(color);
        return this;
    }

    public float snap(float value) {
        return snapTo(value, 0.0f);
    }

    public float snapX(float x) {
        return snapTo(x, offsetX);
    }

    public float snapY(float y) {
        return snapTo(y, offsetY);
    }

    public DrawPoint snapPoint(float x, float y) {
        return new DrawPoint(snapX(x), snapY(y));
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        float width = context == null || !Float.isFinite(context.availableWidth()) ? 0.0f : context.availableWidth();
        float height = context == null || !Float.isFinite(context.availableHeight()) ? 0.0f : context.availableHeight();
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE || context == null || !gridVisible) return;
        RectView bounds = layoutBounds();
        if (bounds == null || !isDrawable(bounds.width()) || !isDrawable(bounds.height())) return;

        pushOpacity(context);
        try {
            renderVerticalLines(context, bounds);
            renderHorizontalLines(context, bounds);
        } finally {
            popOpacity(context);
        }
    }

    private void renderVerticalLines(RenderContext context, RectView bounds) {
        long firstIndex = firstGridIndex(bounds.width(), offsetX);
        for (long index = firstIndex, count = 0; count < MAX_RENDERED_LINES_PER_AXIS; index++, count++) {
            float localX = offsetX + index * spacing;
            if (localX > bounds.width() + 0.001f) break;
            if (localX < -0.001f) continue;
            boolean major = isMajorLine(index);
            context.line(
                    bounds.x() + localX,
                    bounds.y(),
                    bounds.x() + localX,
                    bounds.y() + bounds.height(),
                    Paint.stroke(major ? majorLineColor : lineColor, major ? majorLineThickness : lineThickness));
        }
    }

    private void renderHorizontalLines(RenderContext context, RectView bounds) {
        long firstIndex = firstGridIndex(bounds.height(), offsetY);
        for (long index = firstIndex, count = 0; count < MAX_RENDERED_LINES_PER_AXIS; index++, count++) {
            float localY = offsetY + index * spacing;
            if (localY > bounds.height() + 0.001f) break;
            if (localY < -0.001f) continue;
            boolean major = isMajorLine(index);
            context.line(
                    bounds.x(),
                    bounds.y() + localY,
                    bounds.x() + bounds.width(),
                    bounds.y() + localY,
                    Paint.stroke(major ? majorLineColor : lineColor, major ? majorLineThickness : lineThickness));
        }
    }

    private long firstGridIndex(float length, float offset) {
        if (!Float.isFinite(length) || length <= 0.0f) return 0L;
        return (long) Math.ceil((0.0f - offset) / spacing);
    }

    private boolean isMajorLine(long index) {
        return majorEvery > 0 && Math.floorMod(index, majorEvery) == 0;
    }

    private float snapTo(float value, float offset) {
        if (!snapEnabled || !Float.isFinite(value)) return value;
        float interval = positiveOrDefault(snapSize, spacing);
        return offset + Math.round((value - offset) / interval) * interval;
    }

    private static float positiveOrDefault(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static boolean isDrawable(float value) {
        return Float.isFinite(value) && value > 0.0f;
    }
}
