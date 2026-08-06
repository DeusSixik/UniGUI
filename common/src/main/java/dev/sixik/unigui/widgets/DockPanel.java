package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class DockPanel extends PanelWidget {
    private final Map<Widget, DockSide> docks = new IdentityHashMap<>();
    private boolean lastChildFill = true;

    public void addChild(Widget child, DockSide dockSide) {
        if (child == null) return;
        docks.put(child, dockSide == null ? DockSide.LEFT : dockSide);
        super.addChild(child);
    }

    public DockSide dockSide(Widget child) {
        return docks.getOrDefault(child, DockSide.LEFT);
    }

    public DockPanel dockSide(Widget child, DockSide dockSide) {
        if (child == null) return this;
        DockSide normalized = dockSide == null ? DockSide.LEFT : dockSide;
        if (dockSide(child) == normalized) return this;
        docks.put(child, normalized);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean lastChildFill() {
        return lastChildFill;
    }

    public DockPanel lastChildFill(boolean lastChildFill) {
        if (this.lastChildFill == lastChildFill) return this;
        this.lastChildFill = lastChildFill;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void addChild(Widget child) {
        addChild(child, DockSide.LEFT);
    }

    @Override
    public void removeChild(Widget child) {
        docks.remove(child);
        super.removeChild(child);
    }

    @Override
    public void clearChildren() {
        docks.clear();
        super.clearChildren();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();

        List<Widget> snapshot = visibleLayoutChildren();
        float dockedWidth = 0.0f;
        float dockedHeight = 0.0f;
        float desiredWidth = 0.0f;
        float desiredHeight = 0.0f;

        for (int index = 0; index < snapshot.size(); index++) {
            Widget child = snapshot.get(index);
            child.measure(context);
            float childWidth = StackPanel.outerDesiredWidth(child);
            float childHeight = StackPanel.outerDesiredHeight(child);
            boolean fill = lastChildFill && index == snapshot.size() - 1;
            if (fill) {
                desiredWidth = Math.max(desiredWidth, dockedWidth + childWidth);
                desiredHeight = Math.max(desiredHeight, dockedHeight + childHeight);
                continue;
            }

            switch (dockSide(child)) {
                case LEFT, RIGHT -> {
                    dockedWidth += childWidth;
                    desiredWidth = Math.max(desiredWidth, dockedWidth);
                    desiredHeight = Math.max(desiredHeight, dockedHeight + childHeight);
                }
                case TOP, BOTTOM -> {
                    dockedHeight += childHeight;
                    desiredWidth = Math.max(desiredWidth, dockedWidth + childWidth);
                    desiredHeight = Math.max(desiredHeight, dockedHeight);
                }
            }
        }

        setDesiredSize(resolveDesiredSize(context, desiredWidth, desiredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();

        List<Widget> snapshot = visibleLayoutChildren();
        if (snapshot.isEmpty()) return;

        float left = bounds.x();
        float top = bounds.y();
        float right = bounds.x() + bounds.width();
        float bottom = bounds.y() + bounds.height();

        for (int index = 0; index < snapshot.size(); index++) {
            Widget child = snapshot.get(index);
            boolean fill = lastChildFill && index == snapshot.size() - 1;
            if (fill) {
                StackPanel.arrangeChild(child, left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
                continue;
            }

            DockSide side = dockSide(child);
            switch (side) {
                case LEFT -> {
                    float width = Math.min(Math.max(0.0f, right - left), StackPanel.preferredWidth(child, Math.max(0.0f, right - left)));
                    StackPanel.arrangeChild(child, left, top, width, Math.max(0.0f, bottom - top));
                    left += width;
                }
                case RIGHT -> {
                    float width = Math.min(Math.max(0.0f, right - left), StackPanel.preferredWidth(child, Math.max(0.0f, right - left)));
                    StackPanel.arrangeChild(child, right - width, top, width, Math.max(0.0f, bottom - top));
                    right -= width;
                }
                case TOP -> {
                    float height = Math.min(Math.max(0.0f, bottom - top), StackPanel.preferredHeight(child, Math.max(0.0f, bottom - top)));
                    StackPanel.arrangeChild(child, left, top, Math.max(0.0f, right - left), height);
                    top += height;
                }
                case BOTTOM -> {
                    float height = Math.min(Math.max(0.0f, bottom - top), StackPanel.preferredHeight(child, Math.max(0.0f, bottom - top)));
                    StackPanel.arrangeChild(child, left, bottom - height, Math.max(0.0f, right - left), height);
                    bottom -= height;
                }
            }
        }
    }

    private List<Widget> visibleLayoutChildren() {
        List<Widget> output = new ArrayList<>();
        for (Widget child : children()) {
            if (child.visibility() != Visibility.COLLAPSED) {
                output.add(child);
            }
        }
        return output;
    }
}
