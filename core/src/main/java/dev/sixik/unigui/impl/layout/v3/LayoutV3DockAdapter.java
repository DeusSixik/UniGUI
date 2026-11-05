package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.EdgeInsets;
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
import dev.sixik.unigui.impl.layout.SlotLayout;
import dev.sixik.unigui.widgets.containers.DockSide;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import dev.sixik.unigui.widgets.containers.DockPanel;

/** V3-адаптер миграции для последовательного алгоритма стыковки по краям DockPanel. */
public final class LayoutV3DockAdapter {
    private static final LayoutNodeId ROOT_ID = LayoutNodeId.of("root");

    private LayoutV3DockAdapter() {
    }

    public static LayoutSize measure(List<Widget> children,
                                     Function<Widget, DockSide> dockSide,
                                     boolean lastChildFill,
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
        float dockedWidth = 0.0f;
        float dockedHeight = 0.0f;
        float desiredWidth = 0.0f;
        float desiredHeight = 0.0f;

        for (int index = 0; index < snapshot.size(); index++) {
            Widget child = snapshot.get(index);
            child.measure(childContext);
            float childWidth = outerDesiredWidth(child);
            float childHeight = outerDesiredHeight(child);
            boolean fill = lastChildFill && index == snapshot.size() - 1;
            if (fill) {
                desiredWidth = Math.max(desiredWidth, dockedWidth + childWidth);
                desiredHeight = Math.max(desiredHeight, dockedHeight + childHeight);
                continue;
            }

            switch (sideOf(dockSide, child)) {
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
        return LayoutSize.of(desiredWidth + padding.horizontal(), desiredHeight + padding.vertical());
    }

    public static void arrange(List<Widget> children,
                               Function<Widget, DockSide> dockSide,
                               boolean lastChildFill,
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

        float left = contentBounds.x();
        float top = contentBounds.y();
        float right = contentBounds.x() + contentBounds.width();
        float bottom = contentBounds.y() + contentBounds.height();

        for (int index = 0; index < snapshot.size(); index++) {
            Widget child = snapshot.get(index);
            boolean fill = lastChildFill && index == snapshot.size() - 1;
            LayoutNodeId id = LayoutNodeId.of("child" + originalIndex(children, child));
            MutableRect rect;
            if (fill) {
                rect = childRect(child, left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
            } else {
                switch (sideOf(dockSide, child)) {
                    case LEFT -> {
                        float width = Math.min(Math.max(0.0f, right - left),
                                preferredWidth(child, Math.max(0.0f, right - left)));
                        rect = childRect(child, left, top, width, Math.max(0.0f, bottom - top));
                        left += width;
                    }
                    case RIGHT -> {
                        float width = Math.min(Math.max(0.0f, right - left),
                                preferredWidth(child, Math.max(0.0f, right - left)));
                        rect = childRect(child, right - width, top, width, Math.max(0.0f, bottom - top));
                        right -= width;
                    }
                    case TOP -> {
                        float height = Math.min(Math.max(0.0f, bottom - top),
                                preferredHeight(child, Math.max(0.0f, bottom - top)));
                        rect = childRect(child, left, top, Math.max(0.0f, right - left), height);
                        top += height;
                    }
                    case BOTTOM -> {
                        float height = Math.min(Math.max(0.0f, bottom - top),
                                preferredHeight(child, Math.max(0.0f, bottom - top)));
                        rect = childRect(child, left, bottom - height, Math.max(0.0f, right - left), height);
                        bottom -= height;
                    }
                    default -> rect = childRect(child, left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
                }
            }
            addResult(output, widgets, id, child, rect, bounds);
        }

        addAbsoluteResults(children, output, widgets, contentBounds, bounds);
        LayoutApplier.apply(output.build(), widgets, bounds);
    }

    private static List<Widget> visibleLayoutChildren(List<Widget> children) {
        List<Widget> output = new ObjectArrayList<>();
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

    private static MutableRect childRect(Widget child, float slotX, float slotY, float slotWidth, float slotHeight) {
        return SlotLayout.placeChild(child, slotX, slotY, slotWidth, slotHeight);
    }

    private static float preferredWidth(Widget child, float fallback) {
        return SlotLayout.preferredWidth(child, fallback);
    }

    private static float preferredHeight(Widget child, float fallback) {
        return SlotLayout.preferredHeight(child, fallback);
    }

    private static float outerDesiredWidth(Widget child) {
        return SlotLayout.outerDesiredWidth(child);
    }

    private static float outerDesiredHeight(Widget child) {
        return SlotLayout.outerDesiredHeight(child);
    }

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available)
                ? Math.max(0.0f, available - Math.max(0.0f, consumed))
                : Float.POSITIVE_INFINITY;
    }

    private static DockSide sideOf(Function<Widget, DockSide> dockSide, Widget child) {
        if (dockSide == null) {
            return DockSide.LEFT;
        }
        DockSide side = dockSide.apply(child);
        return side == null ? DockSide.LEFT : side;
    }

    private static int originalIndex(List<Widget> children, Widget child) {
        return children == null ? 0 : Math.max(0, children.indexOf(child));
    }
}