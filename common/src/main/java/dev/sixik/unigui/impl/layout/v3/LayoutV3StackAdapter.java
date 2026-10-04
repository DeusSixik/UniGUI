package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.impl.layout.SlotLayout;

import java.util.List;

/** V3-адаптер миграции для семантики оверлей-стека StackPanel. */
public final class LayoutV3StackAdapter {
    private LayoutV3StackAdapter() {
    }

    public static LayoutSize measure(List<Widget> children, LayoutContext context, LayoutStyle containerStyle) {
        LayoutStyle style = containerStyle == null ? new LayoutStyle() : containerStyle;
        EdgeInsets padding = style.padding();
        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        float availableHeight = context == null ? Float.POSITIVE_INFINITY : context.availableHeight();
        LayoutContext childContext = new LayoutContext(
                subtractAvailable(availableWidth, padding.horizontal()),
                subtractAvailable(availableHeight, padding.vertical()));

        float desiredWidth = 0.0f;
        float desiredHeight = 0.0f;
        if (children != null) {
            for (Widget child : children) {
                if (child == null || child.visibility() == Visibility.COLLAPSED) {
                    continue;
                }
                child.measure(childContext);
                if (AbsoluteLayoutEngine.isAbsolute(child)) {
                    continue;
                }
                desiredWidth = Math.max(desiredWidth, SlotLayout.outerDesiredWidth(child));
                desiredHeight = Math.max(desiredHeight, SlotLayout.outerDesiredHeight(child));
            }
        }
        return LayoutSize.of(desiredWidth + padding.horizontal(), desiredHeight + padding.vertical());
    }

    public static void arrange(List<Widget> children, RectView bounds, LayoutStyle containerStyle) {
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
        if (children == null) {
            return;
        }
        for (Widget child : children) {
            if (child == null || child.visibility() == Visibility.COLLAPSED) {
                continue;
            }
            if (AbsoluteLayoutEngine.isAbsolute(child)) {
                AbsoluteLayoutEngine.arrange(child, contentBounds);
            } else {
                arrangeNormalChild(child,
                        contentBounds.x(), contentBounds.y(),
                        contentBounds.width(), contentBounds.height());
            }
        }
    }

    private static void arrangeNormalChild(Widget child, float slotX, float slotY, float slotWidth, float slotHeight) {
        SlotLayout.arrangeChild(child, slotX, slotY, slotWidth, slotHeight);
    }

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available) ? Math.max(0.0f, available - consumed) : Float.POSITIVE_INFINITY;
    }
}