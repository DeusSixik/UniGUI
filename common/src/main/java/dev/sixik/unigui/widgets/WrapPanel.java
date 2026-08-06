package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.List;

public final class WrapPanel extends PanelWidget {
    private Orientation orientation = Orientation.HORIZONTAL;
    private float spacing;
    private float lineSpacing;

    public Orientation orientation() {
        return orientation;
    }

    public WrapPanel orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float spacing() {
        return spacing;
    }

    public WrapPanel spacing(float spacing) {
        float normalized = Float.isFinite(spacing) ? Math.max(0.0f, spacing) : 0.0f;
        if (this.spacing == normalized) return this;
        this.spacing = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float lineSpacing() {
        return lineSpacing;
    }

    public WrapPanel lineSpacing(float lineSpacing) {
        float normalized = Float.isFinite(lineSpacing) ? Math.max(0.0f, lineSpacing) : 0.0f;
        if (this.lineSpacing == normalized) return this;
        this.lineSpacing = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        List<Widget> snapshot = visibleLayoutChildren();
        for (Widget child : snapshot) {
            child.measure(context);
        }
        LayoutSize measured = orientation == Orientation.HORIZONTAL
                ? measureHorizontal(context, snapshot)
                : measureVertical(context, snapshot);
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        if (orientation == Orientation.HORIZONTAL) {
            arrangeHorizontal(bounds);
        } else {
            arrangeVertical(bounds);
        }
    }

    private void arrangeHorizontal(RectView bounds) {
        List<Widget> line = new ArrayList<>();
        float lineWidth = 0.0f;
        float lineHeight = 0.0f;
        float y = bounds.y();
        float maxWidth = Math.max(0.0f, bounds.width());
        for (Widget child : visibleLayoutChildren()) {
            float childWidth = StackPanel.preferredWidth(child, maxWidth);
            float childHeight = StackPanel.preferredHeight(child, Math.max(0.0f, bounds.height()));
            float nextWidth = line.isEmpty() ? childWidth : lineWidth + spacing + childWidth;
            if (!line.isEmpty() && nextWidth > maxWidth) {
                arrangeHorizontalLine(line, bounds.x(), y, lineHeight);
                y += lineHeight + lineSpacing;
                line.clear();
                lineWidth = 0.0f;
                lineHeight = 0.0f;
            }
            line.add(child);
            lineWidth = lineWidth == 0.0f ? childWidth : lineWidth + spacing + childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
        }
        if (!line.isEmpty()) {
            arrangeHorizontalLine(line, bounds.x(), y, lineHeight);
        }
    }

    private void arrangeHorizontalLine(List<Widget> line, float x, float y, float lineHeight) {
        float childX = x;
        for (Widget child : line) {
            float width = StackPanel.preferredWidth(child, 0.0f);
            StackPanel.arrangeChild(child, childX, y, width, lineHeight);
            childX += width + spacing;
        }
    }

    private void arrangeVertical(RectView bounds) {
        List<Widget> column = new ArrayList<>();
        float columnHeight = 0.0f;
        float columnWidth = 0.0f;
        float x = bounds.x();
        float maxHeight = Math.max(0.0f, bounds.height());
        for (Widget child : visibleLayoutChildren()) {
            float childWidth = StackPanel.preferredWidth(child, Math.max(0.0f, bounds.width()));
            float childHeight = StackPanel.preferredHeight(child, maxHeight);
            float nextHeight = column.isEmpty() ? childHeight : columnHeight + spacing + childHeight;
            if (!column.isEmpty() && nextHeight > maxHeight) {
                arrangeVerticalColumn(column, x, bounds.y(), columnWidth);
                x += columnWidth + lineSpacing;
                column.clear();
                columnHeight = 0.0f;
                columnWidth = 0.0f;
            }
            column.add(child);
            columnHeight = columnHeight == 0.0f ? childHeight : columnHeight + spacing + childHeight;
            columnWidth = Math.max(columnWidth, childWidth);
        }
        if (!column.isEmpty()) {
            arrangeVerticalColumn(column, x, bounds.y(), columnWidth);
        }
    }

    private void arrangeVerticalColumn(List<Widget> column, float x, float y, float columnWidth) {
        float childY = y;
        for (Widget child : column) {
            float height = StackPanel.preferredHeight(child, 0.0f);
            StackPanel.arrangeChild(child, x, childY, columnWidth, height);
            childY += height + spacing;
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

    private LayoutSize measureHorizontal(LayoutContext context, List<Widget> children) {
        float maxWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        float lineWidth = 0.0f;
        float lineHeight = 0.0f;
        float desiredWidth = 0.0f;
        float desiredHeight = 0.0f;
        int lineCount = 0;

        for (Widget child : children) {
            float childWidth = StackPanel.outerDesiredWidth(child);
            float childHeight = StackPanel.outerDesiredHeight(child);
            float nextWidth = lineCount == 0 ? childWidth : lineWidth + spacing + childWidth;
            if (lineCount > 0 && Float.isFinite(maxWidth) && nextWidth > maxWidth) {
                desiredWidth = Math.max(desiredWidth, lineWidth);
                desiredHeight += lineHeight + (desiredHeight > 0.0f ? lineSpacing : 0.0f);
                lineWidth = 0.0f;
                lineHeight = 0.0f;
                lineCount = 0;
            }
            lineWidth = lineCount == 0 ? childWidth : lineWidth + spacing + childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
            lineCount++;
        }

        if (lineCount > 0) {
            desiredWidth = Math.max(desiredWidth, lineWidth);
            desiredHeight += lineHeight + (desiredHeight > 0.0f ? lineSpacing : 0.0f);
        }
        return LayoutSize.of(desiredWidth, desiredHeight);
    }

    private LayoutSize measureVertical(LayoutContext context, List<Widget> children) {
        float maxHeight = context == null ? Float.POSITIVE_INFINITY : context.availableHeight();
        float columnHeight = 0.0f;
        float columnWidth = 0.0f;
        float desiredWidth = 0.0f;
        float desiredHeight = 0.0f;
        int columnCount = 0;

        for (Widget child : children) {
            float childWidth = StackPanel.outerDesiredWidth(child);
            float childHeight = StackPanel.outerDesiredHeight(child);
            float nextHeight = columnCount == 0 ? childHeight : columnHeight + spacing + childHeight;
            if (columnCount > 0 && Float.isFinite(maxHeight) && nextHeight > maxHeight) {
                desiredWidth += columnWidth + (desiredWidth > 0.0f ? lineSpacing : 0.0f);
                desiredHeight = Math.max(desiredHeight, columnHeight);
                columnHeight = 0.0f;
                columnWidth = 0.0f;
                columnCount = 0;
            }
            columnHeight = columnCount == 0 ? childHeight : columnHeight + spacing + childHeight;
            columnWidth = Math.max(columnWidth, childWidth);
            columnCount++;
        }

        if (columnCount > 0) {
            desiredWidth += columnWidth + (desiredWidth > 0.0f ? lineSpacing : 0.0f);
            desiredHeight = Math.max(desiredHeight, columnHeight);
        }
        return LayoutSize.of(desiredWidth, desiredHeight);
    }
}
