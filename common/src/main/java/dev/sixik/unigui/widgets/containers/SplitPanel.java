package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.v3.LayoutV3SplitAdapter;
import dev.sixik.unigui.widgets.core.Orientation;

public class SplitPanel extends PanelWidget {
    private final Splitter splitter = new Splitter(this);
    private Widget first;
    private Widget second;
    private Orientation orientation = Orientation.HORIZONTAL;
    private float splitRatio = 0.5f;
    private float splitterThickness = 5.0f;
    private float minFirstSize = 32.0f;
    private float minSecondSize = 32.0f;
    private float dragStartRoot;
    private float dragStartRatio;

    public SplitPanel() {
        super.addChild(splitter);
    }

    public SplitPanel(Widget first, Widget second) {
        this();
        first(first);
        second(second);
    }

    public Widget first() {
        return first;
    }

    public SplitPanel first(Widget first) {
        if (this.first == first || first == splitter) return this;
        if (this.first != null) {
            super.removeChild(this.first);
        }
        this.first = first;
        if (first != null) {
            super.addChild(first);
        }
        ensureSplitterOnTop();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Widget second() {
        return second;
    }

    public SplitPanel second(Widget second) {
        if (this.second == second || second == splitter) return this;
        if (this.second != null) {
            super.removeChild(this.second);
        }
        this.second = second;
        if (second != null) {
            super.addChild(second);
        }
        ensureSplitterOnTop();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Splitter splitter() {
        return splitter;
    }

    public Orientation orientation() {
        return orientation;
    }

    public SplitPanel orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float splitRatio() {
        return splitRatio;
    }

    public SplitPanel splitRatio(float splitRatio) {
        float normalized = clamp01(splitRatio);
        if (this.splitRatio == normalized) return this;
        this.splitRatio = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public SplitPanel silentSplitRatio(float splitRatio) {
        return splitRatio(splitRatio);
    }

    public float splitterThickness() {
        return splitterThickness;
    }

    public SplitPanel splitterThickness(float splitterThickness) {
        float normalized = Float.isFinite(splitterThickness) ? Math.max(1.0f, splitterThickness) : 5.0f;
        if (this.splitterThickness == normalized) return this;
        this.splitterThickness = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float minFirstSize() {
        return minFirstSize;
    }

    public SplitPanel minFirstSize(float minFirstSize) {
        this.minFirstSize = sanitizeMin(minFirstSize);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float minSecondSize() {
        return minSecondSize;
    }

    public SplitPanel minSecondSize(float minSecondSize) {
        this.minSecondSize = sanitizeMin(minSecondSize);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean dragging() {
        return splitter.dragging();
    }

    @Override
    public void addChild(Widget child) {
        if (child == null) return;
        if (first == null) {
            first(child);
        } else if (second == null) {
            second(child);
        }
    }

    @Override
    public void removeChild(Widget child) {
        if (child == null || child == splitter) return;
        if (child == first) {
            first(null);
        } else if (child == second) {
            second(null);
        } else {
            super.removeChild(child);
        }
    }

    @Override
    public void clearChildren() {
        first(null);
        second(null);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        float fallbackWidth = orientation == Orientation.HORIZONTAL ? 200.0f : 120.0f;
        float fallbackHeight = orientation == Orientation.HORIZONTAL ? 120.0f : 160.0f;
        float width = finiteOr(context == null ? Float.NaN : context.availableWidth(), fallbackWidth);
        float height = finiteOr(context == null ? Float.NaN : context.availableHeight(), fallbackHeight);
        LayoutSize desired = resolveDesiredSize(context, width, height);
        LayoutV3SplitAdapter.measure(first, second, splitter,
                desired.width(), desired.height(),
                orientation, splitRatio, splitterThickness, minFirstSize, minSecondSize);
        setDesiredSize(desired);
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3SplitAdapter.arrange(first, second, splitter, bounds,
                orientation, splitRatio, splitterThickness, minFirstSize, minSecondSize);
    }

    @Override
    protected void renderChildren(RenderContext context) {
        applyQueuedMutations();
        renderChildWithInheritedTransform(context, first);
        renderChildWithInheritedTransform(context, second);
        renderChildWithInheritedTransform(context, splitter);
    }

    void beginSplitterDrag(float rootX, float rootY) {
        dragStartRoot = orientation == Orientation.HORIZONTAL ? rootX : rootY;
        dragStartRatio = splitRatio;
    }

    void dragSplitterTo(float rootX, float rootY) {
        float current = orientation == Orientation.HORIZONTAL ? rootX : rootY;
        float total = orientation == Orientation.HORIZONTAL ? layoutBounds().width() : layoutBounds().height();
        float available = Math.max(1.0f, total - splitterThickness);
        splitRatio(dragStartRatio + (current - dragStartRoot) / available);
    }

    @Override
    public void dispose() {
        splitter.cancelDrag();
        super.dispose();
    }

    private void ensureSplitterOnTop() {
        super.removeChild(splitter);
        super.addChild(splitter);
    }

    private LayoutRects layoutRects(float x, float y, float width, float height) {
        float available = Math.max(0.0f, (orientation == Orientation.HORIZONTAL ? width : height) - splitterThickness);
        float firstSize = available * splitRatio;
        firstSize = clamp(firstSize, Math.min(minFirstSize, available), Math.max(0.0f, available - minSecondSize));
        float secondSize = Math.max(0.0f, available - firstSize);

        if (orientation == Orientation.HORIZONTAL) {
            return new LayoutRects(
                    new MutableRect(x, y, firstSize, height),
                    new MutableRect(x + firstSize, y, splitterThickness, height),
                    new MutableRect(x + firstSize + splitterThickness, y, secondSize, height));
        }
        return new LayoutRects(
                new MutableRect(x, y, width, firstSize),
                new MutableRect(x, y + firstSize, width, splitterThickness),
                new MutableRect(x, y + firstSize + splitterThickness, width, secondSize));
    }

    private static float sanitizeMin(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.5f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private record LayoutRects(MutableRect first, MutableRect splitter, MutableRect second) {
    }
}
