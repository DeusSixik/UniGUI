package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.v3.LayoutV3Settings;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.layout.FlexLayoutEngine;
import dev.sixik.unigui.impl.layout.v3.LayoutV3FlexAdapter;

public class LinearBox extends PanelWidget {
    private Orientation orientation;
    private float spacing;

    public LinearBox(Orientation orientation) {
        this.orientation = orientation == null ? Orientation.VERTICAL : orientation;
        syncLayoutStyle();
    }

    public Orientation orientation() {
        return orientation;
    }

    public LinearBox orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.VERTICAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float spacing() {
        return spacing;
    }

    public LinearBox spacing(float spacing) {
        if (this.spacing == spacing) return this;
        this.spacing = spacing;
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
        LayoutSize measured = LayoutV3Settings.linearBoxEnabled()
                ? LayoutV3FlexAdapter.measure(
                children(), context, direction, FlexWrap.NOWRAP,
                layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle())
                : FlexLayoutEngine.measure(
                children(), context, direction, FlexWrap.NOWRAP,
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
        if (LayoutV3Settings.linearBoxEnabled()) {
            LayoutV3FlexAdapter.arrange(
                    children(), bounds, direction, FlexWrap.NOWRAP,
                    layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle());
        } else {
            FlexLayoutEngine.arrange(
                    children(), bounds, direction, FlexWrap.NOWRAP,
                    layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle());
        }
    }

    private void syncLayoutStyle() {
        layoutStyle().update(style -> {
            style.flexDirection(orientation == Orientation.HORIZONTAL
                    ? FlexDirection.ROW
                    : FlexDirection.COLUMN);
            style.flexWrap(FlexWrap.NOWRAP);
            style.rowGap(orientation == Orientation.HORIZONTAL ? 0.0f : spacing);
            style.columnGap(orientation == Orientation.HORIZONTAL ? spacing : 0.0f);
        });
    }
}
