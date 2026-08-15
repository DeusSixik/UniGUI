package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.layout.v3.LayoutV3FlexAdapter;
import dev.sixik.unigui.widgets.core.Orientation;

public final class WrapPanel extends PanelWidget {
    private Orientation orientation = Orientation.HORIZONTAL;
    private float spacing;
    private float lineSpacing;

    public WrapPanel() {
        syncLayoutStyle();
    }

    public Orientation orientation() {
        return orientation;
    }

    public WrapPanel orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        syncLayoutStyle();
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
        syncLayoutStyle();
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
        syncLayoutStyle();
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
        FlexDirection direction = orientation == Orientation.HORIZONTAL
                ? FlexDirection.ROW
                : FlexDirection.COLUMN;
        LayoutSize measured = LayoutV3FlexAdapter.measure(
                children(), context, direction, FlexWrap.WRAP,
                layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        FlexDirection direction = orientation == Orientation.HORIZONTAL
                ? FlexDirection.ROW
                : FlexDirection.COLUMN;
        LayoutV3FlexAdapter.arrange(
                children(), bounds, direction, FlexWrap.WRAP,
                layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle());
    }

    private void syncLayoutStyle() {
        layoutStyle().update(style -> {
            style.flexDirection(orientation == Orientation.HORIZONTAL
                    ? FlexDirection.ROW
                    : FlexDirection.COLUMN);
            style.flexWrap(FlexWrap.WRAP);
            style.rowGap(orientation == Orientation.HORIZONTAL ? lineSpacing : spacing);
            style.columnGap(orientation == Orientation.HORIZONTAL ? spacing : lineSpacing);
        });
    }
}
