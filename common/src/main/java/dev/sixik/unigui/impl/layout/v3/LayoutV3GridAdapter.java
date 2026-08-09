package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** V3 migration adapter for GridBox's current equal-cell grid semantics. */
public final class LayoutV3GridAdapter {
    private static final LayoutNodeId ROOT_ID = LayoutNodeId.of("root");

    private LayoutV3GridAdapter() {
    }

    public static LayoutSize measure(List<Widget> children,
                                     int columns,
                                     float horizontalSpacing,
                                     float verticalSpacing,
                                     LayoutContext context,
                                     LayoutStyle containerStyle) {
        LayoutStyle style = containerStyle == null ? new LayoutStyle() : containerStyle;
        EdgeInsets padding = style.padding();
        LayoutContext childContext = context == null
                ? null
                : new LayoutContext(
                subtractAvailable(context.availableWidth(), padding.horizontal()),
                subtractAvailable(context.availableHeight(), padding.vertical()));
        List<Widget> snapshot = visibleLayoutChildren(children);
        measureAbsoluteChildren(children, childContext);
        if (snapshot.isEmpty()) {
            return LayoutSize.of(padding.horizontal(), padding.vertical());
        }

        float maxCellWidth = 0.0f;
        float maxCellHeight = 0.0f;
        for (Widget child : snapshot) {
            child.measure(childContext);
            LayoutSize childSize = child.desiredSize().withMargin(child.layoutConstraints().margin());
            maxCellWidth = Math.max(maxCellWidth, childSize.width());
            maxCellHeight = Math.max(maxCellHeight, childSize.height());
        }

        int normalizedColumns = normalizeColumns(columns);
        int visibleColumns = Math.min(normalizedColumns, snapshot.size());
        int rows = rows(snapshot.size(), normalizedColumns);
        float desiredWidth = maxCellWidth * visibleColumns + horizontalSpacing * Math.max(0, visibleColumns - 1);
        float desiredHeight = maxCellHeight * rows + verticalSpacing * Math.max(0, rows - 1);
        return LayoutSize.of(desiredWidth + padding.horizontal(), desiredHeight + padding.vertical());
    }

    public static void arrange(List<Widget> children,
                               int columns,
                               float horizontalSpacing,
                               float verticalSpacing,
                               RectView bounds,
                               LayoutStyle containerStyle) {
        if (bounds == null) {
            return;
        }
        LayoutStyle style = containerStyle == null ? new LayoutStyle() : containerStyle;
        EdgeInsets padding = style.padding();
        MutableRect contentBounds = new MutableRect(
                bounds.x() + padding.left(),
                bounds.y() + padding.top(),
                Math.max(0.0f, bounds.width() - padding.horizontal()),
                Math.max(0.0f, bounds.height() - padding.vertical()));

        LayoutOutput.Builder output = LayoutOutput.builder(ROOT_ID)
                .add(new LayoutResult(ROOT_ID, 0.0f, 0.0f, bounds.width(), bounds.height(),
                        contentBounds.width(), contentBounds.height()));
        Map<LayoutNodeId, Widget> widgets = new LinkedHashMap<>();
        List<Widget> snapshot = visibleLayoutChildren(children);
        int normalizedColumns = normalizeColumns(columns);
        if (!snapshot.isEmpty()) {
            int rows = rows(snapshot.size(), normalizedColumns);
            float totalHorizontalSpacing = horizontalSpacing * Math.max(0, normalizedColumns - 1);
            float totalVerticalSpacing = verticalSpacing * Math.max(0, rows - 1);
            float cellWidth = Math.max(0.0f, contentBounds.width() - totalHorizontalSpacing) / normalizedColumns;
            float cellHeight = Math.max(0.0f, contentBounds.height() - totalVerticalSpacing) / rows;

            for (int index = 0; index < snapshot.size(); index++) {
                Widget child = snapshot.get(index);
                int row = index / normalizedColumns;
                int column = index % normalizedColumns;
                float x = contentBounds.x() + column * (cellWidth + horizontalSpacing);
                float y = contentBounds.y() + row * (cellHeight + verticalSpacing);
                addResult(output, widgets, LayoutNodeId.of("cell" + originalIndex(children, child)),
                        child, childRect(child, x, y, cellWidth, cellHeight), bounds);
            }
        }

        addAbsoluteResults(children, output, widgets, contentBounds, bounds);
        LayoutApplier.apply(output.build(), widgets, bounds);
    }

    private static List<Widget> visibleLayoutChildren(List<Widget> children) {
        List<Widget> output = new ArrayList<>();
        if (children == null) {
            return output;
        }
        for (Widget child : children) {
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && !AbsoluteLayoutEngine.isAbsolute(child)) {
                output.add(child);
            }
        }
        return output;
    }

    private static void measureAbsoluteChildren(List<Widget> children, LayoutContext context) {
        if (children == null) {
            return;
        }
        for (Widget child : children) {
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && AbsoluteLayoutEngine.isAbsolute(child)) {
                child.measure(context);
            }
        }
    }

    private static void addAbsoluteResults(List<Widget> children,
                                           LayoutOutput.Builder output,
                                           Map<LayoutNodeId, Widget> widgets,
                                           RectView contentBounds,
                                           RectView hostBounds) {
        if (children == null) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            Widget child = children.get(index);
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && AbsoluteLayoutEngine.isAbsolute(child)) {
                addResult(output, widgets, LayoutNodeId.of("absolute" + index), child,
                        AbsoluteLayoutEngine.resolveRect(child, contentBounds), hostBounds);
            }
        }
    }

    private static void addResult(LayoutOutput.Builder output,
                                  Map<LayoutNodeId, Widget> widgets,
                                  LayoutNodeId id,
                                  Widget widget,
                                  RectView rect,
                                  RectView hostBounds) {
        output.add(LayoutResult.of(id,
                rect.x() - hostBounds.x(),
                rect.y() - hostBounds.y(),
                rect.width(),
                rect.height()));
        widgets.put(id, widget);
    }

    private static MutableRect childRect(Widget child, float cellX, float cellY, float cellWidth, float cellHeight) {
        LayoutConstraints constraints = child.layoutConstraints();
        EdgeInsets margin = constraints.margin();
        float innerX = cellX + margin.left();
        float innerY = cellY + margin.top();
        float innerWidth = Math.max(0.0f, cellWidth - margin.horizontal());
        float innerHeight = Math.max(0.0f, cellHeight - margin.vertical());
        float childWidth = resolveSize(innerWidth, constraints.preferredWidth(), child.desiredSize().width(),
                constraints.minWidth(), constraints.maxWidth(), constraints.horizontalAlignment());
        float childHeight = resolveSize(innerHeight, constraints.preferredHeight(), child.desiredSize().height(),
                constraints.minHeight(), constraints.maxHeight(), constraints.verticalAlignment());
        return new MutableRect(
                align(innerX, innerWidth, childWidth, constraints.horizontalAlignment()),
                align(innerY, innerHeight, childHeight, constraints.verticalAlignment()),
                childWidth,
                childHeight);
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

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available)
                ? Math.max(0.0f, available - Math.max(0.0f, consumed))
                : Float.POSITIVE_INFINITY;
    }

    private static int normalizeColumns(int columns) {
        return Math.max(1, columns);
    }

    private static int rows(int count, int columns) {
        return (int) Math.ceil(count / (double) normalizeColumns(columns));
    }

    private static int originalIndex(List<Widget> children, Widget child) {
        return children == null ? 0 : Math.max(0, children.indexOf(child));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }
}