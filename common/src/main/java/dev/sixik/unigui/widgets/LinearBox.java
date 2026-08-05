package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;

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
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        applyQueuedMutations();

        List<Widget> snapshot = List.copyOf(children());
        if (snapshot.isEmpty()) return;

        int count = snapshot.size();
        float totalSpacing = spacing * Math.max(0, count - 1);

        if (orientation == Orientation.HORIZONTAL) {
            float childWidth = Math.max(0.0f, bounds.width() - totalSpacing) / count;
            float x = bounds.x();
            for (Widget child : snapshot) {
                child.arrange(new MutableRect(x, bounds.y(), childWidth, bounds.height()));
                x += childWidth + spacing;
            }
        } else {
            float childHeight = Math.max(0.0f, bounds.height() - totalSpacing) / count;
            float y = bounds.y();
            for (Widget child : snapshot) {
                child.arrange(new MutableRect(bounds.x(), y, bounds.width(), childHeight));
                y += childHeight + spacing;
            }
        }
    }
}
