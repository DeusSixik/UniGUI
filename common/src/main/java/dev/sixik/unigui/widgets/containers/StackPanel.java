package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.v3.LayoutV3StackAdapter;

public final class StackPanel extends PanelWidget {
    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        LayoutSize measured = LayoutV3StackAdapter.measure(children(), context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3StackAdapter.arrange(children(), bounds, layoutStyle());
    }

    public static void arrangeChild(Widget child, float slotX, float slotY, float slotWidth, float slotHeight) {
        LayoutConstraints constraints = child.layoutConstraints();
        EdgeInsets margin = constraints.margin();
        float innerX = slotX + margin.left();
        float innerY = slotY + margin.top();
        float innerWidth = Math.max(0.0f, slotWidth - margin.horizontal());
        float innerHeight = Math.max(0.0f, slotHeight - margin.vertical());
        float childWidth = resolveSize(innerWidth, constraints.preferredWidth(), child.desiredSize().width(), constraints.minWidth(), constraints.maxWidth(), constraints.horizontalAlignment());
        float childHeight = resolveSize(innerHeight, constraints.preferredHeight(), child.desiredSize().height(), constraints.minHeight(), constraints.maxHeight(), constraints.verticalAlignment());
        float childX = align(innerX, innerWidth, childWidth, constraints.horizontalAlignment());
        float childY = align(innerY, innerHeight, childHeight, constraints.verticalAlignment());
        child.arrange(new MutableRect(childX, childY, childWidth, childHeight));
    }

    public static float preferredWidth(Widget child, float fallback) {
        LayoutConstraints constraints = child.layoutConstraints();
        float preferred = constraints.preferredWidth();
        float content = LayoutConstraints.isAuto(preferred) ? measuredOrFallback(child.desiredSize().width(), fallback) : preferred;
        return constraints.margin().horizontal() + clamp(content, constraints.minWidth(), constraints.maxWidth());
    }

    public static float preferredHeight(Widget child, float fallback) {
        LayoutConstraints constraints = child.layoutConstraints();
        float preferred = constraints.preferredHeight();
        float content = LayoutConstraints.isAuto(preferred) ? measuredOrFallback(child.desiredSize().height(), fallback) : preferred;
        return constraints.margin().vertical() + clamp(content, constraints.minHeight(), constraints.maxHeight());
    }

    static float outerDesiredWidth(Widget child) {
        return child.desiredSize().width() + child.layoutConstraints().margin().horizontal();
    }

    static float outerDesiredHeight(Widget child) {
        return child.desiredSize().height() + child.layoutConstraints().margin().vertical();
    }

    static float resolveSize(float available, float preferred, float measured, float min, float max, Alignment alignment) {
        if (alignment == Alignment.STRETCH && LayoutConstraints.isAuto(preferred)) {
            return clamp(available, min, max);
        }
        float desired = LayoutConstraints.isAuto(preferred) ? measuredOrFallback(measured, available) : preferred;
        return Math.min(available, clamp(desired, min, max));
    }

    private static float measuredOrFallback(float measured, float fallback) {
        return measured > 0.0f ? measured : fallback;
    }

    static float align(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.STRETCH : alignment) {
            case START, STRETCH -> start;
            case CENTER -> start + (available - size) * 0.5f;
            case END -> start + available - size;
        };
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }
}
