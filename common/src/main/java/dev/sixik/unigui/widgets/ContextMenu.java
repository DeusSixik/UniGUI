package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

public final class ContextMenu extends Box implements OverlayHostAware {
    private final VBox itemsHost = new VBox();
    private boolean open;
    private boolean closeOnOutsideClick = true;
    private float x;
    private float y;
    private EdgeInsets padding = EdgeInsets.all(3.0f);

    public ContextMenu() {
        backgroundVisible(true);
        borderVisible(true);
        radius(3.0f);
        background().set(0.025f, 0.030f, 0.040f, 0.98f);
        borderColor().set(0.25f, 0.78f, 1.0f, 0.85f);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.HIDDEN));
        visible(false);
        itemsHost.spacing(1.0f);
        addChild(itemsHost);
    }

    public ContextMenu item(String text, Runnable action) {
        return item(RichText.plain(text), action);
    }

    public ContextMenu item(RichText text, Runnable action) {
        Button button = new Button(text == null ? RichText.plain("") : text);
        button.layout(style -> style.size(132.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        button.onClick(event -> {
            if (action != null) action.run();
            close();
        });
        itemsHost.addChild(button);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ContextMenu separator() {
        Separator separator = new Separator();
        separator.layout(style -> style.size(132.0f, 1.0f).margin(2.0f, 3.0f).flexGrow(0).flexShrink(0.0f));
        itemsHost.addChild(separator);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean opened() {
        return open;
    }

    public ContextMenu openAt(float x, float y) {
        this.x = Float.isFinite(x) ? x : 0.0f;
        this.y = Float.isFinite(y) ? y : 0.0f;
        return open(true);
    }

    public ContextMenu open() {
        return open(true);
    }

    public ContextMenu close() {
        return open(false);
    }

    public ContextMenu toggle(float x, float y) {
        return open ? close() : openAt(x, y);
    }

    public ContextMenu open(boolean open) {
        if (this.open == open) return this;
        this.open = open;
        visible(open);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean closeOnOutsideClick() {
        return closeOnOutsideClick;
    }

    public ContextMenu closeOnOutsideClick(boolean closeOnOutsideClick) {
        this.closeOnOutsideClick = closeOnOutsideClick;
        return this;
    }

    public ContextMenu padding(EdgeInsets padding) {
        this.padding = padding == null ? EdgeInsets.all(0.0f) : padding;
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
        itemsHost.measure(context);
        setDesiredSize(resolveDesiredSize(context,
                itemsHost.desiredSize().width() + padding.horizontal(),
                itemsHost.desiredSize().height() + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView hostBounds) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(hostBounds.x(), hostBounds.y(), 0.0f, 0.0f);
            return;
        }
        float width = desiredSize().width();
        float height = desiredSize().height();
        float placedX = Math.max(hostBounds.x(), Math.min(x, hostBounds.x() + Math.max(0.0f, hostBounds.width() - width)));
        float placedY = Math.max(hostBounds.y(), Math.min(y, hostBounds.y() + Math.max(0.0f, hostBounds.height() - height)));
        mutableLayoutBounds().set(placedX, placedY, width, height);
        itemsHost.arrange(new MutableRect(
                placedX + padding.left(),
                placedY + padding.top(),
                Math.max(0.0f, width - padding.horizontal()),
                Math.max(0.0f, height - padding.vertical())));
    }
}