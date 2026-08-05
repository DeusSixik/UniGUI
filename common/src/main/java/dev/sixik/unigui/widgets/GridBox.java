package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;

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
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        applyQueuedMutations();

        List<Widget> snapshot = List.copyOf(children());
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
            snapshot.get(index).arrange(new MutableRect(x, y, cellWidth, cellHeight));
        }
    }
}
