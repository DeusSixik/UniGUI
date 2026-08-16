package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.WidgetEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.render.ModalScrimRenderer;
import dev.sixik.unigui.widgets.render.ModalScrimState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Root-level overlay host that renders overlay children above a normal content widget.
 */
@XmlWidgetName("OverlayLayer")
public final class OverlayLayer extends PanelWidget {
    private final WindowManager windowManager = new WindowManager(this);
    private final Map<Widget, Integer> overlayZ = new IdentityHashMap<>();
    private final MutableColor modalScrimColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.48f);
    private Widget content;
    private ModalScrimRenderer modalScrimRenderer;
    private int nextOverlayZ = 1;

    public OverlayLayer() {
    }

    public OverlayLayer(Widget content) {
        content(content);
        applyQueuedMutations();
    }

    public Widget content() {
        return content;
    }

    public WindowManager windows() {
        return windowManager;
    }

    public WindowManager windowManager() {
        return windowManager;
    }

    public MutableColor modalScrimColor() {
        return modalScrimColor;
    }

    public ModalScrimRenderer modalScrimRenderer() {
        return modalScrimRenderer;
    }

    public OverlayLayer modalScrimRenderer(ModalScrimRenderer modalScrimRenderer) {
        if (this.modalScrimRenderer == modalScrimRenderer) return this;
        this.modalScrimRenderer = modalScrimRenderer;
        invalidate(dev.sixik.unigui.api.core.InvalidationFlags.VISUAL);
        return this;
    }

    public OverlayLayer useDefaultModalScrimRenderer() {
        return modalScrimRenderer(null);
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
            overlayZ.putIfAbsent(overlay, nextOverlayZ++);
            if (overlay instanceof WindowWidget window) {
                windowManager.register(window);
            }
            addChild(overlay);
        }
        return this;
    }

    public OverlayLayer removeOverlay(Widget overlay) {
        if (overlay != null && overlay != content) {
            if (overlay instanceof WindowWidget window) {
                windowManager.unregister(window);
            }
            overlayZ.remove(overlay);
            removeChild(overlay);
        }
        return this;
    }

    @Override
    public void applyQueuedMutations() {
        super.applyQueuedMutations();
        enforceOverlayOrder();
    }

    @Override
    public void handle(Event event) {
        WindowWidget topModal = windowManager.topModalWindow();
        if (topModal != null
                && event instanceof WidgetEvent widgetEvent
                && widgetEvent.phase() == EventPhase.CAPTURE
                && !isDescendantOrSelf(widgetEvent.target(), topModal)) {
            event.cancel();
            return;
        }
        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() == EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY) {
            WindowWidget window = windowAncestor(pointer.target());
            if (window != null && window.opened()) {
                windowManager.activate(window);
            }
            closeOverlaysOutside(pointer.target());
        }
        super.handle(event);
    }

    @Override
    protected void renderChildren(RenderContext context) {
        applyQueuedMutations();
        WindowWidget topModal = windowManager.topModalWindow();
        boolean scrimRendered = false;
        for (Widget child : children()) {
            if (child.visibility() != Visibility.VISIBLE) continue;
            if (topModal != null && child == topModal && !scrimRendered) {
                renderModalScrim(context);
                scrimRendered = true;
            }
            renderChildWithInheritedTransform(context, child);
        }
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
            } else if (child instanceof ContextMenu contextMenu
                    && contextMenu.opened()
                    && contextMenu.closeOnOutsideClick()
                    && !contextMenu.isMenuInteractionTarget(target)) {
                contextMenu.close();
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

    private static WindowWidget windowAncestor(Widget widget) {
        Widget current = widget;
        while (current != null) {
            if (current instanceof WindowWidget window) return window;
            current = current.parent();
        }
        return null;
    }

    private void enforceOverlayOrder() {
        reorderChildren((left, right) -> {
            int rankCompare = Integer.compare(overlayRank(left), overlayRank(right));
            if (rankCompare != 0) return rankCompare;
            return Integer.compare(overlayZ(left), overlayZ(right));
        });
    }

    private int overlayRank(Widget widget) {
        return widget == content ? 0 : 1;
    }

    private void renderModalScrim(RenderContext context) {
        effectiveModalScrimRenderer().render(new DrawScope(context, transform(), layoutBounds()), modalScrimState());
    }

    private ModalScrimRenderer effectiveModalScrimRenderer() {
        return modalScrimRenderer == null ? styleRenderer(ModalScrimRenderer.class, WidgetsRender.modalScrim()) : modalScrimRenderer;
    }

    private ModalScrimState modalScrimState() {
        return new ModalScrimState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                windowManager.topModalWindow() != null,
                windowManager.modalStackDepth(),
                modalScrimColor.copy());
    }

    void bringOverlayToFront(Widget overlay) {
        if (overlay == null || overlay == content) return;
        overlayZ.put(overlay, nextOverlayZ++);
        enforceOverlayOrder();
        windowManager.invalidateOrder();
    }

    int overlayZ(Widget overlay) {
        return overlayZ.getOrDefault(overlay, 0);
    }
}
