package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;

public final class Popup extends Box implements OverlayHostAware {
    private Widget anchor;
    private Widget content;
    private boolean open;
    private boolean closeOnOutsideClick = true;
    private float offsetX;
    private float offsetY = 4.0f;
    private EdgeInsets padding = EdgeInsets.all(6.0f);

    public Popup() {
        backgroundVisible(true);
        borderVisible(true);
        radius(3.0f);
        background().set(0.025f, 0.030f, 0.040f, 0.97f);
        borderColor().set(0.25f, 0.78f, 1.0f, 0.90f);
        layout(style -> style
                .position(PositionType.ABSOLUTE)
                .maxWidthPercent(100.0f)
                .maxHeightPercent(100.0f)
                .overflow(Overflow.HIDDEN));
        visible(false);
    }

    public Popup(Widget anchor, Widget content) {
        this();
        anchor(anchor);
        content(content);
    }

    public Widget anchor() {
        return anchor;
    }

    public Popup anchor(Widget anchor) {
        if (this.anchor == anchor) return this;
        this.anchor = anchor;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Widget content() {
        return content;
    }

    public Popup content(Widget content) {
        if (this.content == content) return this;
        if (this.content != null) {
            removeChild(this.content);
        }
        this.content = content;
        if (content != null) {
            addChild(content);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean opened() {
        return open;
    }

    public Popup open() {
        return open(true);
    }

    public Popup close() {
        return open(false);
    }

    public Popup toggle() {
        return open(!open);
    }

    public Popup open(boolean open) {
        if (this.open == open) return this;
        this.open = open;
        visible(open);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean closeOnOutsideClick() {
        return closeOnOutsideClick;
    }

    public Popup closeOnOutsideClick(boolean closeOnOutsideClick) {
        this.closeOnOutsideClick = closeOnOutsideClick;
        return this;
    }

    public Popup offset(float x, float y) {
        float normalizedX = Float.isFinite(x) ? x : 0.0f;
        float normalizedY = Float.isFinite(y) ? y : 4.0f;
        if (offsetX == normalizedX && offsetY == normalizedY) return this;
        offsetX = normalizedX;
        offsetY = normalizedY;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Popup padding(EdgeInsets padding) {
        EdgeInsets normalized = padding == null ? EdgeInsets.all(0.0f) : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }

        applyQueuedMutations();
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            content.measure(context);
            setDesiredSize(resolveDesiredSize(context,
                    content.desiredSize().width() + padding.horizontal(),
                    content.desiredSize().height() + padding.vertical()));
        } else {
            setDesiredSize(resolveDesiredSize(context, padding.horizontal(), padding.vertical()));
        }
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView bounds) {
        if (!open || visibility() == Visibility.COLLAPSED || anchor == null) {
            mutableLayoutBounds().set(bounds.x(), bounds.y(), 0.0f, 0.0f);
            return;
        }

        applyQueuedMutations();
        float width = desiredSize().width();
        float height = desiredSize().height();
        MutableRect placed = AbsoluteLayoutEngine.placeBelow(
                bounds, anchor.layoutBounds(), width, height, offsetX, offsetY, true, true);
        float x = placed.x();
        float y = placed.y();
        width = placed.width();
        height = placed.height();
        mutableLayoutBounds().set(placed);

        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            content.arrange(new MutableRect(
                    x + padding.left(),
                    y + padding.top(),
                    Math.max(0.0f, width - padding.horizontal()),
                    Math.max(0.0f, height - padding.vertical())));
        }
    }
}
