package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.SparkPointClickEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.SparklineRenderer;
import dev.sixik.unigui.widgets.render.SparklineState;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

public class Sparkline extends WidgetBase {
    public static final float DEFAULT_PREFERRED_WIDTH = 140.0f;
    public static final float DEFAULT_PREFERRED_HEIGHT = 34.0f;

    private static final float POINT_HIT_RADIUS = 7.0f;

    private final FloatArrayList values = new FloatArrayList();
    private final MutableColor lineColor = new MutableColor(0.25f, 0.85f, 1.0f, 1.0f);
    private final MutableColor fillColor = new MutableColor(0.25f, 0.85f, 1.0f, 0.18f);
    private final MutableColor pointColor = new MutableColor(0.90f, 0.96f, 1.0f, 0.96f);
    private final MutableColor hoveredPointColor = new MutableColor(1.0f, 0.84f, 0.34f, 1.0f);
    private final MutableColor labelColor = new MutableColor(0.92f, 0.96f, 1.0f, 1.0f);
    private final MutableColor tooltipBackground = new MutableColor(0.025f, 0.030f, 0.040f, 0.96f);
    private final MutableColor tooltipBorder = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);
    private boolean fillVisible = true;
    private PointMode pointMode = PointMode.EXTREMA;
    private boolean pointLabelsVisible;
    private PointLabelPlacement pointLabelPlacement = PointLabelPlacement.ABOVE;
    private SparkPointRenderer pointRenderer;
    private SparkPointLabelRenderer pointLabelRenderer;
    private SparkPointTooltipRenderer pointTooltipRenderer;
    private SparklineRenderer renderer;
    private int hoveredPointIndex = -1;
    private float preferredWidth;
    private float preferredHeight;

    public Sparkline() {
        this(DEFAULT_PREFERRED_WIDTH, DEFAULT_PREFERRED_HEIGHT);
    }

    protected Sparkline(float preferredWidth, float preferredHeight) {
        this.preferredWidth = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        this.preferredHeight = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        lineColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        fillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        pointColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        hoveredPointColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        labelColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        tooltipBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        tooltipBorder.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Sparkline values(List<? extends Number> values) {
        this.values.clear();
        if (values != null) {
            for (Number value : values) {
                this.values.add(value == null ? 0.0f : value.floatValue());
            }
        }
        hoveredPointIndex = -1;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<Float> values() {
        return Collections.unmodifiableList(values);
    }

    public MutableColor lineColor() {
        return lineColor;
    }

    public MutableColor fillColor() {
        return fillColor;
    }

    public MutableColor pointColor() {
        return pointColor;
    }

    public MutableColor hoveredPointColor() {
        return hoveredPointColor;
    }

    public MutableColor labelColor() {
        return labelColor;
    }

    public MutableColor tooltipBackground() {
        return tooltipBackground;
    }

    public MutableColor tooltipBorder() {
        return tooltipBorder;
    }

    public Sparkline fillVisible(boolean fillVisible) {
        if (this.fillVisible == fillVisible) return this;
        this.fillVisible = fillVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline pointMode(PointMode pointMode) {
        PointMode normalized = pointMode == null ? PointMode.NONE : pointMode;
        if (this.pointMode == normalized) return this;
        this.pointMode = normalized;
        hoveredPointIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline pointLabelsVisible(boolean pointLabelsVisible) {
        if (this.pointLabelsVisible == pointLabelsVisible) return this;
        this.pointLabelsVisible = pointLabelsVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline pointLabelPlacement(PointLabelPlacement pointLabelPlacement) {
        this.pointLabelPlacement = pointLabelPlacement == null ? PointLabelPlacement.ABOVE : pointLabelPlacement;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline pointRenderer(SparkPointRenderer pointRenderer) {
        this.pointRenderer = pointRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline pointLabelRenderer(SparkPointLabelRenderer pointLabelRenderer) {
        this.pointLabelRenderer = pointLabelRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline pointTooltipRenderer(SparkPointTooltipRenderer pointTooltipRenderer) {
        this.pointTooltipRenderer = pointTooltipRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public SparklineRenderer renderer() {
        return renderer;
    }

    public Sparkline renderer(SparklineRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline useDefaultRenderer() {
        return renderer(null);
    }

    public float preferredWidth() {
        return preferredWidth;
    }

    public Sparkline preferredWidth(float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float preferredHeight() {
        return preferredHeight;
    }

    public Sparkline preferredHeight(float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Sparkline preferredSize(float width, float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    public EventSubscription onPointClick(EventListener<? super SparkPointClickEvent> listener) {
        return on(SparkPointClickEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled() || visibility() != Visibility.VISIBLE) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerMovedEvent pointer) {
            int next = hitPoint(pointer.rootX(), pointer.rootY());
            if (next != hoveredPointIndex) {
                hoveredPointIndex = next;
                invalidate(InvalidationFlags.VISUAL);
            }
        } else if (event instanceof PointerExitedEvent) {
            clearHoveredPoint();
        } else if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            int hit = hitPoint(pointer.rootX(), pointer.rootY());
            if (hit >= 0) {
                SparkPoint point = sparkPoint(hit, true);
                if (point != null) {
                    clickPoint(point);
                    event.cancel();
                }
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE || values.size() < 2) return;
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot());
    }

    protected SparklineState snapshot() {
        return new SparklineState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                computePoints(),
                fillVisible,
                pointLabelsVisible,
                pointLabelPlacement,
                lineColor.copy(),
                fillColor.copy(),
                pointColor.copy(),
                hoveredPointColor.copy(),
                labelColor.copy(),
                tooltipBackground.copy(),
                tooltipBorder.copy(),
                pointRenderer,
                pointLabelRenderer,
                pointTooltipRenderer);
    }

    protected SparklineRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(SparklineRenderer.class, WidgetsRender.sparkline()) : renderer;
    }

    protected List<SparkPoint> computePoints() {
        if (values.size() < 2) return List.of();
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) return List.of();

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < values.size(); i++) {
            float value = values.getFloat(i);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (!Float.isFinite(min) || !Float.isFinite(max)) return List.of();
        float range = Math.max(0.0001f, max - min);

        List<SparkPoint> points = new ObjectArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            float value = values.getFloat(i);
            float px = x + width * i / Math.max(1.0f, values.size() - 1.0f);
            float py = y + height - ((value - min) / range) * height;
            points.add(new SparkPoint(i, value, px, py, isPointRenderable(i), i == hoveredPointIndex));
        }
        return points;
    }

    private int hitPoint(float rootX, float rootY) {
        if (pointMode == PointMode.NONE || values.size() < 2) return -1;
        List<SparkPoint> points = computePoints();
        float radiusSquared = POINT_HIT_RADIUS * POINT_HIT_RADIUS;
        for (SparkPoint point : points) {
            if (!point.renderable()) continue;
            float dx = rootX - point.x();
            float dy = rootY - point.y();
            if (dx * dx + dy * dy <= radiusSquared) {
                return point.index();
            }
        }
        return -1;
    }

    private SparkPoint sparkPoint(int index, boolean hovered) {
        List<SparkPoint> points = computePoints();
        for (SparkPoint point : points) {
            if (point.index() == index) {
                return new SparkPoint(point.index(), point.value(), point.x(), point.y(), point.renderable(), hovered);
            }
        }
        return null;
    }

    private SparkPointClickEvent clickPoint(SparkPoint point) {
        SparkPointClickEvent event = new SparkPointClickEvent(this, point.index(), point.value(), point.x(), point.y());
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    private boolean isPointRenderable(int index) {
        return switch (pointMode) {
            case NONE -> false;
            case ALL -> true;
            case EXTREMA -> values.size() <= 2 || isExtrema(index);
        };
    }

    private boolean isExtrema(int index) {
        if (index <= 0 || index >= values.size() - 1) return false;
        float previous = values.getFloat(index - 1);
        float current = values.getFloat(index);
        float next = values.getFloat(index + 1);
        return (current >= previous && current >= next && (current > previous || current > next))
                || (current <= previous && current <= next && (current < previous || current < next));
    }

    private void clearHoveredPoint() {
        if (hoveredPointIndex == -1) return;
        hoveredPointIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    public enum PointMode {
        NONE,
        EXTREMA,
        ALL
    }

    public enum PointLabelPlacement {
        NONE,
        CENTER,
        ABOVE,
        BELOW
    }

    public record SparkPoint(int index, float value, float x, float y, boolean renderable, boolean hovered) {
    }

    @FunctionalInterface
    public interface SparkPointRenderer {
        void render(DrawScope draw, SparkPoint point);
    }

    @FunctionalInterface
    public interface SparkPointLabelRenderer {
        void render(DrawScope draw, SparkPoint point);
    }

    @FunctionalInterface
    public interface SparkPointTooltipRenderer {
        void render(DrawScope draw, SparkPoint point);
    }
}
