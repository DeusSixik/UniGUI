package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.v3.LayoutV3DockAdapter;

import java.util.IdentityHashMap;
import java.util.Map;

public final class DockPanel extends PanelWidget {
    private final Map<Widget, DockSide> docks = new IdentityHashMap<>();
    private boolean lastChildFill = true;

    public void addChild(Widget child, DockSide dockSide) {
        if (child == null) return;
        docks.put(child, dockSide == null ? DockSide.LEFT : dockSide);
        super.addChild(child);
    }

    public DockSide dockSide(Widget child) {
        return docks.getOrDefault(child, DockSide.LEFT);
    }

    public DockPanel dockSide(Widget child, DockSide dockSide) {
        if (child == null) return this;
        DockSide normalized = dockSide == null ? DockSide.LEFT : dockSide;
        if (dockSide(child) == normalized) return this;
        docks.put(child, normalized);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean lastChildFill() {
        return lastChildFill;
    }

    public DockPanel lastChildFill(boolean lastChildFill) {
        if (this.lastChildFill == lastChildFill) return this;
        this.lastChildFill = lastChildFill;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void addChild(Widget child) {
        addChild(child, DockSide.LEFT);
    }

    @Override
    public void removeChild(Widget child) {
        docks.remove(child);
        super.removeChild(child);
    }

    @Override
    public void clearChildren() {
        docks.clear();
        super.clearChildren();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();

        LayoutSize measured = LayoutV3DockAdapter.measure(
                children(), this::dockSide, lastChildFill, context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3DockAdapter.arrange(children(), this::dockSide, lastChildFill, bounds, layoutStyle());
    }
}
