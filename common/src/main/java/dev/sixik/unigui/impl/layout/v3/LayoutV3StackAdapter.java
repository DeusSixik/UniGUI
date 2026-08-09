package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;

import java.util.List;

/** V3 migration adapter for StackPanel's overlay-stack semantics. */
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
                desiredWidth = Math.max(desiredWidth, child.desiredSize().width() + child.layoutConstraints().margin().horizontal());
                desiredHeight = Math.max(desiredHeight, child.desiredSize().height() + child.layoutConstraints().margin().vertical());
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
        LayoutConstraints constraints = child.layoutConstraints();
        EdgeInsets margin = constraints.margin();
        float innerX = slotX + margin.left();
        float innerY = slotY + margin.top();
        float innerWidth = Math.max(0.0f, slotWidth - margin.horizontal());
        float innerHeight = Math.max(0.0f, slotHeight - margin.vertical());
        float childWidth = resolveSize(innerWidth, constraints.preferredWidth(), child.desiredSize().width(),
                constraints.minWidth(), constraints.maxWidth(), constraints.horizontalAlignment());
        float childHeight = resolveSize(innerHeight, constraints.preferredHeight(), child.desiredSize().height(),
                constraints.minHeight(), constraints.maxHeight(), constraints.verticalAlignment());
        float childX = align(innerX, innerWidth, childWidth, constraints.horizontalAlignment());
        float childY = align(innerY, innerHeight, childHeight, constraints.verticalAlignment());
        child.arrange(new MutableRect(childX, childY, childWidth, childHeight));
    }

    private static float resolveSize(float available, float preferred, float measured, float min, float max, Alignment alignment) {
        if (alignment == Alignment.STRETCH && LayoutConstraints.isAuto(preferred)) {
            return clamp(available, min, max);
        }
        float desired = LayoutConstraints.isAuto(preferred) ? measuredOrFallback(measured, available) : preferred;
        return Math.min(available, clamp(desired, min, max));
    }

    private static float measuredOrFallback(float measured, float fallback) {
        return measured > 0.0f ? measured : fallback;
    }

    private static float align(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.STRETCH : alignment) {
            case START, STRETCH -> start;
            case CENTER -> start + (available - size) * 0.5f;
            case END -> start + available - size;
        };
    }

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available) ? Math.max(0.0f, available - consumed) : Float.POSITIVE_INFINITY;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }
}