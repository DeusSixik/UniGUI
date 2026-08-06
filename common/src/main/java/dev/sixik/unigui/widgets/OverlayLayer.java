package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;

/**
 * Root-level overlay host that renders overlay children above a normal content widget.
 */
public final class OverlayLayer extends PanelWidget {
    private Widget content;

    public OverlayLayer() {
    }

    public OverlayLayer(Widget content) {
        content(content);
    }

    public Widget content() {
        return content;
    }

    public OverlayLayer content(Widget content) {
        if (this.content == content) return this;
        if (this.content != null) {
            removeChild(this.content);
        }
        this.content = content;
        if (content != null) {
            addChild(content);
        }
        return this;
    }

    public OverlayLayer addOverlay(Widget overlay) {
        if (overlay != null && overlay != content) {
            if (overlay instanceof WidgetBase base) {
                base.layout(style -> style.position(PositionType.ABSOLUTE));
            }
            addChild(overlay);
        }
        return this;
    }

    public OverlayLayer removeOverlay(Widget overlay) {
        if (overlay != null && overlay != content) {
            removeChild(overlay);
        }
        return this;
    }

    @Override
    public void handle(Event event) {
        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() == EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY) {
            closeOverlaysOutside(pointer.target());
        }
        super.handle(event);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }

        applyQueuedMutations();
        LayoutContext childContext = AbsoluteLayoutEngine.contentContext(this, context);
        float desiredWidth = 0.0f;
        float desiredHeight = 0.0f;
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            content.measure(childContext);
            desiredWidth = content.desiredSize().width();
            desiredHeight = content.desiredSize().height();
        }
        for (Widget child : children()) {
            if (child != content && child.visibility() != Visibility.COLLAPSED) {
                child.measure(childContext);
            }
        }
        var padding = layoutStyle().padding();
        setDesiredSize(resolveDesiredSize(context,
                desiredWidth + padding.horizontal(),
                desiredHeight + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        MutableRect hostBounds = AbsoluteLayoutEngine.contentBounds(this, bounds);
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            StackPanel.arrangeChild(content,
                    hostBounds.x(), hostBounds.y(), hostBounds.width(), hostBounds.height());
        }
        for (Widget child : children()) {
            if (child == content || child.visibility() == Visibility.COLLAPSED) continue;
            if (child instanceof OverlayHostAware hostAware) {
                hostAware.arrangeInHost(hostBounds);
            } else {
                AbsoluteLayoutEngine.arrange(child, hostBounds);
            }
        }
    }

    private void closeOverlaysOutside(Widget target) {
        for (Widget child : children()) {
            if (child instanceof Popup popup
                    && popup.opened()
                    && popup.closeOnOutsideClick()
                    && !isPopupInteractionTarget(target, popup)) {
                popup.close();
            } else if (child instanceof WindowWidget window
                    && window.opened()
                    && window.closeOnOutsideClick()
                    && !isDescendantOrSelf(target, window)) {
                window.close();
            }
        }
    }

    private static boolean isDescendantOrSelf(Widget widget, Widget ancestor) {
        Widget current = widget;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.parent();
        }
        return false;
    }

    private static boolean isPopupInteractionTarget(Widget target, Popup popup) {
        return isDescendantOrSelf(target, popup)
                || isDescendantOrSelf(target, popup.anchor());
    }
}
