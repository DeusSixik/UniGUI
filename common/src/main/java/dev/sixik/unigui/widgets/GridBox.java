package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.List;

public final class GridBox extends PanelWidget {
    private int columns = 1;
    private float horizontalSpacing;
    private float verticalSpacing;

    public int columns() {
        return columns;
    }

    public GridBox columns(int columns) {
        int normalized = Math.max(1, columns);
        if (this.columns == normalized) return this;
        this.columns = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float horizontalSpacing() {
        return horizontalSpacing;
    }

    public GridBox horizontalSpacing(float horizontalSpacing) {
        if (this.horizontalSpacing == horizontalSpacing) return this;
        this.horizontalSpacing = horizontalSpacing;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float verticalSpacing() {
        return verticalSpacing;
    }

    public GridBox verticalSpacing(float verticalSpacing) {
        if (this.verticalSpacing == verticalSpacing) return this;
        this.verticalSpacing = verticalSpacing;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public GridBox spacing(float spacing) {
        return horizontalSpacing(spacing).verticalSpacing(spacing);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();

        List<Widget> snapshot = visibleLayoutChildren();
        if (snapshot.isEmpty()) {
            setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
            return;
        }

        float maxCellWidth = 0.0f;
        float maxCellHeight = 0.0f;
        for (Widget child : snapshot) {
            child.measure(context);
            LayoutSize childSize = child.desiredSize().withMargin(child.layoutConstraints().margin());
            maxCellWidth = Math.max(maxCellWidth, childSize.width());
            maxCellHeight = Math.max(maxCellHeight, childSize.height());
        }

        int visibleColumns = Math.min(columns, snapshot.size());
        int rows = (int) Math.ceil(snapshot.size() / (double) columns);
        float desiredWidth = maxCellWidth * visibleColumns + horizontalSpacing * Math.max(0, visibleColumns - 1);
        float desiredHeight = maxCellHeight * rows + verticalSpacing * Math.max(0, rows - 1);
        setDesiredSize(resolveDesiredSize(context, desiredWidth, desiredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();

        List<Widget> snapshot = visibleLayoutChildren();
        if (snapshot.isEmpty()) return;

        int rows = (int) Math.ceil(snapshot.size() / (double) columns);
        float totalHorizontalSpacing = horizontalSpacing * Math.max(0, columns - 1);
        float totalVerticalSpacing = verticalSpacing * Math.max(0, rows - 1);
        float cellWidth = Math.max(0.0f, bounds.width() - totalHorizontalSpacing) / columns;
        float cellHeight = Math.max(0.0f, bounds.height() - totalVerticalSpacing) / rows;

        for (int index = 0; index < snapshot.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            float x = bounds.x() + column * (cellWidth + horizontalSpacing);
            float y = bounds.y() + row * (cellHeight + verticalSpacing);
            arrangeChild(snapshot.get(index), x, y, cellWidth, cellHeight);
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

    private static void arrangeChild(Widget child, float cellX, float cellY, float cellWidth, float cellHeight) {
        LayoutConstraints constraints = child.layoutConstraints();
        EdgeInsets margin = constraints.margin();
        float innerX = cellX + margin.left();
        float innerY = cellY + margin.top();
        float innerWidth = Math.max(0.0f, cellWidth - margin.horizontal());
        float innerHeight = Math.max(0.0f, cellHeight - margin.vertical());
        float childWidth = resolveSize(innerWidth, constraints.preferredWidth(), child.desiredSize().width(), constraints.minWidth(), constraints.maxWidth(), constraints.horizontalAlignment());
        float childHeight = resolveSize(innerHeight, constraints.preferredHeight(), child.desiredSize().height(), constraints.minHeight(), constraints.maxHeight(), constraints.verticalAlignment());
        float childX = align(innerX, innerWidth, childWidth, constraints.horizontalAlignment());
        float childY = align(innerY, innerHeight, childHeight, constraints.verticalAlignment());
        child.arrange(new MutableRect(childX, childY, childWidth, childHeight));
    }

    private static float resolveSize(float available, float preferred, float measured, float min, float max, Alignment alignment) {
        if (alignment == Alignment.STRETCH && LayoutConstraints.isAuto(preferred)) {
            return clamp(available, min, max);
        }
        float desired = LayoutConstraints.isAuto(preferred) ? (measured > 0.0f ? measured : available) : preferred;
        return Math.min(available, clamp(desired, min, max));
    }

    private static float align(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.STRETCH : alignment) {
            case START, STRETCH -> start;
            case CENTER -> start + (available - size) * 0.5f;
            case END -> start + available - size;
        };
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }
}
