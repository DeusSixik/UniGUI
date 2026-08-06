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

public class LinearBox extends PanelWidget {
    private Orientation orientation;
    private float spacing;

    public LinearBox(Orientation orientation) {
        this.orientation = orientation == null ? Orientation.VERTICAL : orientation;
    }

    public Orientation orientation() {
        return orientation;
    }

    public LinearBox orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.VERTICAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float spacing() {
        return spacing;
    }

    public LinearBox spacing(float spacing) {
        if (this.spacing == spacing) return this;
        this.spacing = spacing;
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
        float desiredMain = 0.0f;
        float desiredCross = 0.0f;
        int measuredCount = 0;
        for (Widget child : snapshot) {
            child.measure(context);
            LayoutSize childSize = child.desiredSize().withMargin(child.layoutConstraints().margin());
            desiredMain += main(childSize);
            desiredCross = Math.max(desiredCross, cross(childSize));
            measuredCount++;
        }
        desiredMain += spacing * Math.max(0, measuredCount - 1);
        setDesiredSize(orientation == Orientation.HORIZONTAL
                ? resolveDesiredSize(context, desiredMain, desiredCross)
                : resolveDesiredSize(context, desiredCross, desiredMain));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();

        List<Widget> snapshot = visibleLayoutChildren();
        if (snapshot.isEmpty()) return;

        int count = snapshot.size();
        float totalSpacing = spacing * Math.max(0, count - 1);
        float availableMain = Math.max(0.0f, main(bounds) - totalSpacing);
        float fixedMain = 0.0f;
        float totalGrow = 0.0f;
        for (Widget child : snapshot) {
            LayoutConstraints constraints = child.layoutConstraints();
            fixedMain += mainMargin(constraints);
            fixedMain += preferredMainOrMeasured(child);
            totalGrow += constraints.grow();
        }
        float extraMain = Math.max(0.0f, availableMain - fixedMain);

        if (orientation == Orientation.HORIZONTAL) {
            float x = bounds.x();
            for (Widget child : snapshot) {
                float slotWidth = allocatedMain(child, extraMain, totalGrow);
                arrangeChild(child, x, bounds.y(), slotWidth, bounds.height());
                x += slotWidth + spacing;
            }
        } else {
            float y = bounds.y();
            for (Widget child : snapshot) {
                float slotHeight = allocatedMain(child, extraMain, totalGrow);
                arrangeChild(child, bounds.x(), y, bounds.width(), slotHeight);
                y += slotHeight + spacing;
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

    private void arrangeChild(Widget child, float slotX, float slotY, float slotWidth, float slotHeight) {
        LayoutConstraints constraints = child.layoutConstraints();
        EdgeInsets margin = constraints.margin();
        float innerX = slotX + margin.left();
        float innerY = slotY + margin.top();
        float innerWidth = Math.max(0.0f, slotWidth - margin.horizontal());
        float innerHeight = Math.max(0.0f, slotHeight - margin.vertical());

        float childWidth = resolveCrossSize(innerWidth, constraints.preferredWidth(), child.desiredSize().width(), constraints.minWidth(), constraints.maxWidth(), constraints.horizontalAlignment());
        float childHeight = resolveCrossSize(innerHeight, constraints.preferredHeight(), child.desiredSize().height(), constraints.minHeight(), constraints.maxHeight(), constraints.verticalAlignment());
        if (orientation == Orientation.HORIZONTAL) {
            childWidth = clamp(innerWidth, constraints.minWidth(), constraints.maxWidth());
        } else {
            childHeight = clamp(innerHeight, constraints.minHeight(), constraints.maxHeight());
        }

        float childX = align(innerX, innerWidth, childWidth, constraints.horizontalAlignment());
        float childY = align(innerY, innerHeight, childHeight, constraints.verticalAlignment());
        child.arrange(new MutableRect(childX, childY, childWidth, childHeight));
    }

    private float allocatedMain(Widget child, float extraMain, float totalGrow) {
        LayoutConstraints constraints = child.layoutConstraints();
        float preferred = preferredMainOrMeasured(child);
        float share = totalGrow <= 0.0f ? 0.0f : extraMain * (constraints.grow() / totalGrow);
        float size = preferred + share + mainMargin(constraints);
        return Math.max(0.0f, size);
    }

    private float preferredMainOrMeasured(Widget child) {
        LayoutConstraints constraints = child.layoutConstraints();
        float preferred = orientation == Orientation.HORIZONTAL ? constraints.preferredWidth() : constraints.preferredHeight();
        float min = orientation == Orientation.HORIZONTAL ? constraints.minWidth() : constraints.minHeight();
        float max = orientation == Orientation.HORIZONTAL ? constraints.maxWidth() : constraints.maxHeight();
        if (!LayoutConstraints.isAuto(preferred)) {
            return clamp(preferred, min, max);
        }
        float measured = orientation == Orientation.HORIZONTAL ? child.desiredSize().width() : child.desiredSize().height();
        return clamp(measured, min, max);
    }

    private float mainMargin(LayoutConstraints constraints) {
        EdgeInsets margin = constraints.margin();
        return orientation == Orientation.HORIZONTAL ? margin.horizontal() : margin.vertical();
    }

    private float main(RectView bounds) {
        return orientation == Orientation.HORIZONTAL ? bounds.width() : bounds.height();
    }

    private float main(LayoutSize size) {
        return orientation == Orientation.HORIZONTAL ? size.width() : size.height();
    }

    private float cross(LayoutSize size) {
        return orientation == Orientation.HORIZONTAL ? size.height() : size.width();
    }

    private static float resolveCrossSize(float available, float preferred, float measured, float min, float max, Alignment alignment) {
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
