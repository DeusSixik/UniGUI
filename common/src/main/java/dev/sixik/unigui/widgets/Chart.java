package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.ChartBarClickEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.SparklineRenderer;
import dev.sixik.unigui.widgets.render.SparklineState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Chart extends Sparkline {
    private static final float ZERO_BAR_HEIGHT = 1.0f;
    private static final float BAR_HIT_MIN_HEIGHT = 8.0f;

    private final MutableColor axisColor = new MutableColor(0.35f, 0.45f, 0.58f, 0.85f);
    private final MutableColor barColor = new MutableColor(0.25f, 0.72f, 1.0f, 0.55f);
    private final MutableColor hoveredBarColor = new MutableColor(1.0f, 0.78f, 0.28f, 0.70f);
    private final MutableColor valueColor = new MutableColor(0.92f, 0.96f, 1.0f, 1.0f);
    private final MutableColor tooltipBackground = new MutableColor(0.025f, 0.030f, 0.040f, 0.96f);
    private final MutableColor tooltipBorder = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);
    private final List<Float> values = new ArrayList<>();
    private Type type = Type.LINE;
    private boolean barValuesVisible = true;
    private BarValuePlacement barValuePlacement = BarValuePlacement.HEAD;
    private BarRenderer barRenderer;
    private BarValueRenderer barValueRenderer;
    private BarTooltipRenderer barTooltipRenderer;
    private int hoveredBarIndex = -1;

    public enum Type {
        LINE,
        BAR
    }

    public Chart values(List<? extends Number> values) {
        this.values.clear();
        if (values != null) {
            for (Number value : values) {
                this.values.add(value == null ? 0.0f : value.floatValue());
            }
        }
        hoveredBarIndex = -1;
        super.values(values);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public List<Float> values() {
        return Collections.unmodifiableList(values);
    }

    public Chart type(Type type) {
        this.type = type == null ? Type.LINE : type;
        hoveredBarIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Type type() {
        return type;
    }

    public MutableColor axisColor() {
        return axisColor;
    }

    public MutableColor barColor() {
        return barColor;
    }

    public MutableColor hoveredBarColor() {
        return hoveredBarColor;
    }

    public MutableColor valueColor() {
        return valueColor;
    }

    public MutableColor tooltipBackground() {
        return tooltipBackground;
    }

    public MutableColor tooltipBorder() {
        return tooltipBorder;
    }

    public Chart barValuesVisible(boolean barValuesVisible) {
        if (this.barValuesVisible == barValuesVisible) return this;
        this.barValuesVisible = barValuesVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Chart barValuePlacement(BarValuePlacement barValuePlacement) {
        this.barValuePlacement = barValuePlacement == null ? BarValuePlacement.HEAD : barValuePlacement;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Chart barRenderer(BarRenderer barRenderer) {
        this.barRenderer = barRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Chart barValueRenderer(BarValueRenderer barValueRenderer) {
        this.barValueRenderer = barValueRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Chart barTooltipRenderer(BarTooltipRenderer barTooltipRenderer) {
        this.barTooltipRenderer = barTooltipRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public Chart renderer(SparklineRenderer renderer) {
        super.renderer(renderer);
        return this;
    }

    @Override
    public Chart useDefaultRenderer() {
        return renderer(null);
    }

    public EventSubscription onBarClick(EventListener<? super ChartBarClickEvent> listener) {
        return on(ChartBarClickEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, 220.0f, 120.0f));
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled() || visibility() != Visibility.VISIBLE || type != Type.BAR) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerMovedEvent pointer) {
            int next = hitBar(pointer.rootX(), pointer.rootY());
            if (next != hoveredBarIndex) {
                hoveredBarIndex = next;
                invalidate(InvalidationFlags.VISUAL);
            }
        } else if (event instanceof PointerExitedEvent) {
            clearHoveredBar();
        } else if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            int hit = hitBar(pointer.rootX(), pointer.rootY());
            if (hit >= 0) {
                Bar bar = bar(hit, true);
                if (bar != null) {
                    clickBar(bar);
                    event.cancel();
                }
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;
        effectiveRenderer().render(new DrawScope(context, transform()), snapshot());
    }

    @Override
    protected SparklineRenderer effectiveRenderer() {
        return renderer() == null ? WidgetsRender.chart() : renderer();
    }

    @Override
    protected SparklineState snapshot() {
        SparklineState base = super.snapshot();
        return new SparklineState(
                base.x(),
                base.y(),
                base.width(),
                base.height(),
                base.points(),
                base.fillVisible(),
                base.pointLabelsVisible(),
                base.pointLabelPlacement(),
                base.lineColor(),
                base.fillColor(),
                base.pointColor(),
                base.hoveredPointColor(),
                base.labelColor(),
                tooltipBackground.copy(),
                tooltipBorder.copy(),
                base.pointRenderer(),
                base.pointLabelRenderer(),
                base.pointTooltipRenderer(),
                type,
                computeBars(),
                barValuesVisible,
                barValuePlacement,
                axisColor.copy(),
                barColor.copy(),
                hoveredBarColor.copy(),
                valueColor.copy(),
                barRenderer,
                barValueRenderer,
                barTooltipRenderer);
    }

    private List<Bar> computeBars() {
        if (values.isEmpty()) return List.of();
        PlotArea plot = plotArea();
        float gap = 3.0f;
        float barWidth = Math.max(1.0f, (plot.width() - gap * Math.max(0, values.size() - 1)) / values.size());
        List<Bar> bars = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            float value = values.get(i);
            float valueY = plot.valueY(value);
            float top = Math.min(valueY, plot.baseline());
            float bottom = Math.max(valueY, plot.baseline());
            float barHeight = Math.max(ZERO_BAR_HEIGHT, bottom - top);
            if (value == 0.0f) {
                top = plot.baseline() - ZERO_BAR_HEIGHT * 0.5f;
            }
            float bx = plot.x() + i * (barWidth + gap);
            bars.add(new Bar(i, value, bx, top, barWidth, barHeight, plot.baseline(), i == hoveredBarIndex));
        }
        return bars;
    }

    private PlotArea plotArea() {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        float plotX = x + 14.0f;
        float plotY = y + 8.0f;
        float plotWidth = Math.max(0.0f, width - 22.0f);
        float plotHeight = Math.max(0.0f, height - 22.0f);
        float min = 0.0f;
        float max = 0.0f;
        for (float value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (Math.abs(max - min) < 0.0001f) {
            max = min + 1.0f;
        }
        return new PlotArea(plotX, plotY, plotWidth, plotHeight, min, max);
    }

    private int hitBar(float rootX, float rootY) {
        for (Bar bar : computeBars()) {
            float hitY = bar.value() == 0.0f ? bar.baseline() - BAR_HIT_MIN_HEIGHT * 0.5f : bar.y();
            float hitHeight = Math.max(BAR_HIT_MIN_HEIGHT, bar.height());
            if (rootX >= bar.x() && rootX <= bar.x() + bar.width()
                    && rootY >= hitY && rootY <= hitY + hitHeight) {
                return bar.index();
            }
        }
        return -1;
    }

    private Bar bar(int index, boolean hovered) {
        for (Bar bar : computeBars()) {
            if (bar.index() == index) {
                return new Bar(bar.index(), bar.value(), bar.x(), bar.y(), bar.width(), bar.height(), bar.baseline(), hovered);
            }
        }
        return null;
    }

    private ChartBarClickEvent clickBar(Bar bar) {
        ChartBarClickEvent event = new ChartBarClickEvent(this,
                bar.index(), bar.value(), bar.x(), bar.y(), bar.width(), bar.height(), bar.baseline());
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    private void clearHoveredBar() {
        if (hoveredBarIndex == -1) return;
        hoveredBarIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
    }

    public enum BarValuePlacement {
        NONE,
        HEAD,
        CENTER,
        BASE,
        BELOW
    }

    public record Bar(int index, float value, float x, float y, float width, float height, float baseline, boolean hovered) {
        public float centerX() {
            return x + width * 0.5f;
        }

        public float centerY() {
            return y + height * 0.5f;
        }
    }

    private record PlotArea(float x, float y, float width, float height, float min, float max) {
        private float baseline() {
            return valueY(0.0f);
        }

        private float valueY(float value) {
            float range = Math.max(0.0001f, max - min);
            return y + height - ((value - min) / range) * height;
        }
    }

    @FunctionalInterface
    public interface BarRenderer {
        void render(DrawScope draw, Bar bar);
    }

    @FunctionalInterface
    public interface BarValueRenderer {
        void render(DrawScope draw, Bar bar);
    }

    @FunctionalInterface
    public interface BarTooltipRenderer {
        void render(DrawScope draw, Bar bar);
    }
}
