package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.ModalClosedEvent;
import dev.sixik.unigui.api.event.ModalOpenedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.WindowActivatedEvent;
import dev.sixik.unigui.api.event.WindowClosedEvent;
import dev.sixik.unigui.api.event.WindowDeactivatedEvent;
import dev.sixik.unigui.api.event.WindowMoveEndedEvent;
import dev.sixik.unigui.api.event.WindowMovedEvent;
import dev.sixik.unigui.api.event.WindowMoveStartedEvent;
import dev.sixik.unigui.api.event.WindowOpenedEvent;
import dev.sixik.unigui.api.event.WindowResizeEndedEvent;
import dev.sixik.unigui.api.event.WindowResizedEvent;
import dev.sixik.unigui.api.event.WindowResizeStartedEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.widgets.render.WindowRenderer;
import dev.sixik.unigui.widgets.render.WindowState;

import java.util.Objects;

/**
 * Retained overlay window/dialog shell hosted by {@link OverlayLayer}.
 */
public final class WindowWidget extends Box implements OverlayHostAware {
    private static final float DEFAULT_WIDTH = 220.0f;
    private static final float DEFAULT_HEIGHT = 120.0f;
    private static final float DEFAULT_HEADER_HEIGHT = 22.0f;
    private static final float DEFAULT_MIN_WIDTH = 120.0f;
    private static final float DEFAULT_MIN_HEIGHT = 64.0f;
    private static final float RESIZE_HANDLE_SIZE = 6.0f;

    private final Button closeButton = new Button("x");
    private final MutableColor headerColor = new MutableColor(0.075f, 0.090f, 0.125f, 0.98f);
    private final MutableColor headerSeparatorColor = new MutableColor(0.22f, 0.24f, 0.30f, 0.95f);
    private final MutableColor titleColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private WindowRenderer renderer;
    private String title = "";
    private RichText richTitle = RichText.plain("");
    private Widget content;
    private WindowManager windowManager;
    private boolean open;
    private boolean active;
    private boolean modal;
    private boolean fixedModal;
    private boolean dockRedockLocked;
    private boolean draggable = true;
    private boolean dragging;
    private boolean resizable = true;
    private boolean resizing;
    private ResizeHandle resizeHandle = ResizeHandle.NONE;
    private boolean closeButtonVisible = true;
    private boolean closeOnOutsideClick;
    private boolean constrainToHost = true;
    private float x = 32.0f;
    private float y = 28.0f;
    private float headerHeight = DEFAULT_HEADER_HEIGHT;
    private float minWindowWidth = DEFAULT_MIN_WIDTH;
    private float minWindowHeight = DEFAULT_MIN_HEIGHT;
    private EdgeInsets padding = EdgeInsets.all(8.0f);
    private float dragOffsetX;
    private float dragOffsetY;
    private int dragPointerId = -1;
    private int resizePointerId = -1;
    private float resizeStartRootX;
    private float resizeStartRootY;
    private float resizeStartX;
    private float resizeStartY;
    private float resizeStartWidth;
    private float resizeStartHeight;
    private final MutableRect hostBounds = new MutableRect();

    public WindowWidget() {
        backgroundVisible(true);
        borderVisible(true);
        focusable(true);
        radius(4.0f);
        background().set(0.030f, 0.035f, 0.050f, 0.98f);
        borderColor().set(0.25f, 0.78f, 1.0f, 0.85f);
        closeButton.textPaddingX(0.0f);
        closeButton.layout(style -> style.size(18.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
        closeButton.onClick(event -> close());
        addChild(closeButton);
        headerColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        headerSeparatorColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        titleColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(x)
                .top(y)
                .maxWidthPercent(100.0f)
                .maxHeightPercent(100.0f)
                .overflow(Overflow.HIDDEN));
        visible(false);
    }

    public WindowWidget(String title, Widget content) {
        this();
        title(title);
        content(content);
    }

    public WindowWidget(RichText title, Widget content) {
        this();
        richTitle(title);
        content(content);
    }

    public String title() {
        return title;
    }

    public WindowWidget title(String title) {
        String normalized = title == null ? "" : title;
        if (Objects.equals(this.title, normalized)) return this;
        this.title = normalized;
        this.richTitle = RichText.plain(normalized);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richTitle() {
        return richTitle;
    }

    public WindowWidget richTitle(RichText title) {
        RichText normalized = title == null ? RichText.plain("") : title;
        if (Objects.equals(this.richTitle, normalized)) return this;
        this.richTitle = normalized;
        this.title = normalized.plainText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Widget content() {
        return content;
    }

    public WindowWidget content(Widget content) {
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

    public boolean active() {
        return active;
    }

    public boolean modal() {
        return modal;
    }

    public WindowWidget modal(boolean modal) {
        if (this.modal == modal) return this;
        boolean oldModal = this.modal;
        this.modal = modal;
        if (fixedModal && modal) {
            stopDragging();
            stopResizing();
        }
        if (windowManager != null) {
            windowManager.onModalChanged(this, oldModal, modal);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean fixedModal() {
        return fixedModal;
    }

    public WindowWidget fixedModal(boolean fixedModal) {
        if (this.fixedModal == fixedModal) return this;
        this.fixedModal = fixedModal;
        if (fixedModal && modal) {
            stopDragging();
            stopResizing();
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean dockRedockLocked() {
        return dockRedockLocked;
    }

    public WindowWidget dockRedockLocked(boolean dockRedockLocked) {
        if (this.dockRedockLocked == dockRedockLocked) return this;
        this.dockRedockLocked = dockRedockLocked;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WindowWidget openModal() {
        modal(true);
        return open();
    }

    public WindowWidget open() {
        return open(true);
    }

    public WindowWidget close() {
        return open(false);
    }

    public WindowWidget toggle() {
        return open(!open);
    }

    public WindowWidget open(boolean open) {
        if (this.open == open) return this;
        this.open = open;
        if (!open) {
            stopDragging();
            stopResizing();
            if (windowManager != null) {
                windowManager.onWindowClosed(this);
            } else {
                setActiveInternal(false);
            }
            dispatchWindowClosed();
            visible(false);
        } else {
            visible(true);
            dispatchWindowOpened();
            if (windowManager != null) {
                windowManager.onWindowOpened(this);
            }
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean draggable() {
        return draggable;
    }

    public WindowWidget draggable(boolean draggable) {
        if (this.draggable == draggable) return this;
        this.draggable = draggable;
        if (!effectiveDraggable()) {
            stopDragging();
        }
        return this;
    }

    public boolean dragging() {
        return dragging;
    }

    public boolean resizable() {
        return resizable;
    }

    public WindowWidget resizable(boolean resizable) {
        if (this.resizable == resizable) return this;
        this.resizable = resizable;
        if (!effectiveResizable()) {
            stopResizing();
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean resizing() {
        return resizing;
    }

    public String resizeHandle() {
        return resizeHandle.publicName();
    }

    public boolean closeButtonVisible() {
        return closeButtonVisible;
    }

    public WindowWidget closeButtonVisible(boolean closeButtonVisible) {
        if (this.closeButtonVisible == closeButtonVisible) return this;
        this.closeButtonVisible = closeButtonVisible;
        closeButton.visible(closeButtonVisible);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Button closeButton() {
        return closeButton;
    }

    public boolean closeOnOutsideClick() {
        return closeOnOutsideClick;
    }

    public WindowWidget closeOnOutsideClick(boolean closeOnOutsideClick) {
        this.closeOnOutsideClick = closeOnOutsideClick;
        return this;
    }

    public boolean constrainToHost() {
        return constrainToHost;
    }

    public WindowWidget constrainToHost(boolean constrainToHost) {
        if (this.constrainToHost == constrainToHost) return this;
        this.constrainToHost = constrainToHost;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float windowX() {
        return x;
    }

    public float windowY() {
        return y;
    }

    public WindowWidget position(float x, float y) {
        float normalizedX = Float.isFinite(x) ? x : 0.0f;
        float normalizedY = Float.isFinite(y) ? y : 0.0f;
        if (this.x == normalizedX && this.y == normalizedY) return this;
        this.x = normalizedX;
        this.y = normalizedY;
        layout(style -> style.left(normalizedX).top(normalizedY));
        return this;
    }

    public float headerHeight() {
        return headerHeight;
    }

    public WindowWidget headerHeight(float headerHeight) {
        float normalized = Float.isFinite(headerHeight) ? Math.max(12.0f, headerHeight) : DEFAULT_HEADER_HEIGHT;
        if (this.headerHeight == normalized) return this;
        this.headerHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public WindowWidget minWindowSize(float width, float height) {
        float normalizedWidth = Float.isFinite(width) ? Math.max(0.0f, width) : DEFAULT_MIN_WIDTH;
        float normalizedHeight = Float.isFinite(height) ? Math.max(0.0f, height) : DEFAULT_MIN_HEIGHT;
        if (minWindowWidth == normalizedWidth && minWindowHeight == normalizedHeight) return this;
        minWindowWidth = normalizedWidth;
        minWindowHeight = normalizedHeight;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public EdgeInsets padding() {
        return padding;
    }

    public WindowWidget padding(EdgeInsets padding) {
        EdgeInsets normalized = padding == null ? EdgeInsets.ZERO : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor headerColor() {
        return headerColor;
    }

    public MutableColor headerSeparatorColor() {
        return headerSeparatorColor;
    }

    public MutableColor titleColor() {
        return titleColor;
    }

    public WindowRenderer renderer() {
        return renderer;
    }

    public WindowWidget renderer(WindowRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WindowWidget useDefaultRenderer() {
        return renderer(null);
    }

    public EventSubscription onOpened(EventListener<? super WindowOpenedEvent> listener) {
        return on(WindowOpenedEvent.TYPE, listener);
    }

    public EventSubscription onClosed(EventListener<? super WindowClosedEvent> listener) {
        return on(WindowClosedEvent.TYPE, listener);
    }

    public EventSubscription onActivated(EventListener<? super WindowActivatedEvent> listener) {
        return on(WindowActivatedEvent.TYPE, listener);
    }

    public EventSubscription onDeactivated(EventListener<? super WindowDeactivatedEvent> listener) {
        return on(WindowDeactivatedEvent.TYPE, listener);
    }

    public EventSubscription onMoveStarted(EventListener<? super WindowMoveStartedEvent> listener) {
        return on(WindowMoveStartedEvent.TYPE, listener);
    }

    public EventSubscription onMoved(EventListener<? super WindowMovedEvent> listener) {
        return on(WindowMovedEvent.TYPE, listener);
    }

    public EventSubscription onMoveEnded(EventListener<? super WindowMoveEndedEvent> listener) {
        return on(WindowMoveEndedEvent.TYPE, listener);
    }

    public EventSubscription onResizeStarted(EventListener<? super WindowResizeStartedEvent> listener) {
        return on(WindowResizeStartedEvent.TYPE, listener);
    }

    public EventSubscription onResized(EventListener<? super WindowResizedEvent> listener) {
        return on(WindowResizedEvent.TYPE, listener);
    }

    public EventSubscription onResizeEnded(EventListener<? super WindowResizeEndedEvent> listener) {
        return on(WindowResizeEndedEvent.TYPE, listener);
    }

    public EventSubscription onModalOpened(EventListener<? super ModalOpenedEvent> listener) {
        return on(ModalOpenedEvent.TYPE, listener);
    }

    public EventSubscription onModalClosed(EventListener<? super ModalClosedEvent> listener) {
        return on(ModalClosedEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }

        applyQueuedMutations();
        closeButton.visible(closeButtonVisible);
        if (closeButtonVisible) {
            closeButton.measure(new LayoutContext(18.0f, headerHeight));
        }

        float availableContentWidth = context == null ? DEFAULT_WIDTH : Math.max(0.0f, context.availableWidth() - padding.horizontal());
        float availableContentHeight = context == null ? DEFAULT_HEIGHT : Math.max(0.0f, context.availableHeight() - headerHeight - padding.vertical());
        float contentWidth = 0.0f;
        float contentHeight = 0.0f;
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            content.measure(new LayoutContext(availableContentWidth, availableContentHeight));
            contentWidth = content.desiredSize().width();
            contentHeight = content.desiredSize().height();
        }

        float closeWidth = closeButtonVisible ? closeButton.desiredSize().width() : 0.0f;
        float titleWidth = TextEngine.measureLineWidth(title) + padding.left() + padding.right() + closeWidth + 8.0f;
        float desiredWidth = Math.max(minWindowWidth, Math.max(titleWidth, contentWidth + padding.horizontal()));
        float desiredHeight = Math.max(minWindowHeight, headerHeight + contentHeight + padding.vertical());
        setDesiredSize(resolveDesiredSize(context, desiredWidth, desiredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView bounds) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(bounds.x(), bounds.y(), 0.0f, 0.0f);
            return;
        }

        applyQueuedMutations();
        hostBounds.set(bounds);
        MutableRect requested = AbsoluteLayoutEngine.resolveRect(
                this, bounds,
                desiredSize().width() > 0.0f ? desiredSize().width() : DEFAULT_WIDTH,
                desiredSize().height() > 0.0f ? desiredSize().height() : DEFAULT_HEIGHT);
        MutableRect constrained = constrainToHost
                ? AbsoluteLayoutEngine.constrainToHost(
                bounds, requested.x(), requested.y(), requested.width(), requested.height())
                : requested;
        float width = constrained.width();
        float height = constrained.height();
        float absoluteX = constrained.x();
        float absoluteY = constrained.y();
        float resolvedX = absoluteX - bounds.x();
        float resolvedY = absoluteY - bounds.y();
        if (x != resolvedX || y != resolvedY) {
            x = resolvedX;
            y = resolvedY;
            layout(style -> style.left(resolvedX).top(resolvedY));
        }
        mutableLayoutBounds().set(constrained);

        if (closeButtonVisible) {
            float buttonWidth = Math.min(18.0f, Math.max(0.0f, width - padding.horizontal()));
            float buttonHeight = Math.min(18.0f, Math.max(0.0f, headerHeight - 4.0f));
            closeButton.arrange(new MutableRect(
                    absoluteX + Math.max(0.0f, width - padding.right() - buttonWidth),
                    absoluteY + Math.max(0.0f, (headerHeight - buttonHeight) * 0.5f),
                    buttonWidth,
                    buttonHeight));
        } else {
            closeButton.arrange(new MutableRect(absoluteX, absoluteY, 0.0f, 0.0f));
        }

        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            StackPanel.arrangeChild(content,
                    absoluteX + padding.left(),
                    absoluteY + headerHeight + padding.top(),
                    Math.max(0.0f, width - padding.horizontal()),
                    Math.max(0.0f, height - headerHeight - padding.vertical()));
        }
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && effectiveResizable()
                && resizeHandle(pointer.rootX(), pointer.rootY()) != ResizeHandle.NONE) {
            if (windowManager != null) {
                windowManager.activate(this);
            }
            startResizing(pointer, resizeHandle(pointer.rootX(), pointer.rootY()));
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer
                && resizing
                && pointer.pointerId() == resizePointerId) {
            updateResize(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && resizing
                && pointer.pointerId() == resizePointerId) {
            updateResize(pointer.rootX(), pointer.rootY());
            stopResizing();
            event.cancel();
        } else if (event instanceof PointerPressedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && effectiveDraggable()
                && canStartDraggingFrom(pointer)) {
            if (windowManager != null) {
                windowManager.activate(this);
            }
            startDragging(pointer);
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer
                && dragging
                && pointer.pointerId() == dragPointerId) {
            updateDrag(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && dragging
                && pointer.pointerId() == dragPointerId) {
            updateDrag(pointer.rootX(), pointer.rootY());
            stopDragging();
            event.cancel();
        }
    }

    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        if (!enabled() || !effectiveResizable() || !open) {
            return super.mouseCursorAt(localX, localY);
        }
        ResizeHandle handle = resizeHandle(layoutBounds().x() + localX, layoutBounds().y() + localY);
        return switch (handle) {
            case LEFT, RIGHT -> MouseCursor.RESIZE_HORIZONTAL;
            case TOP, BOTTOM -> MouseCursor.RESIZE_VERTICAL;
            case TOP_LEFT, BOTTOM_RIGHT -> MouseCursor.RESIZE_HORIZONTAL;
            case TOP_RIGHT, BOTTOM_LEFT -> MouseCursor.RESIZE_VERTICAL;
            case NONE -> super.mouseCursorAt(localX, localY);
        };
    }

    @Override
    protected void renderContent(RenderContext context) {
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot(context));
        super.renderContent(context);
    }

    private WindowRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(WindowRenderer.class, WidgetsRender.window()) : renderer;
    }

    private WindowState snapshot(RenderContext context) {
        return new WindowState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                headerHeight,
                padding.left(),
                padding.right(),
                closeButtonVisible,
                closeButton.layoutBounds().width(),
                richTitle,
                TextEngine.measureLineWidth(context, richTitle),
                TextEngine.measureTextHeight(richTitle),
                headerColor.copy(),
                headerSeparatorColor.copy(),
                titleColor.copy(),
                active,
                focused(),
                dragging,
                resizing,
                resizeHandle.publicName(),
                modal,
                effectiveResizable());
    }

    private boolean focused() {
        UIContext context = uiContext();
        return context != null && context.focusManager().isFocused(this);
    }

    private void startDragging(PointerPressedEvent pointer) {
        dragging = true;
        dragPointerId = pointer.pointerId();
        dragOffsetX = pointer.rootX() - layoutBounds().x();
        dragOffsetY = pointer.rootY() - layoutBounds().y();
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowMoveStarted();
    }

    private void updateDrag(float rootX, float rootY) {
        float hostX = hostBounds.width() > 0.0f || hostBounds.height() > 0.0f ? hostBounds.x() : layoutBounds().x() - x;
        float hostY = hostBounds.width() > 0.0f || hostBounds.height() > 0.0f ? hostBounds.y() : layoutBounds().y() - y;
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        float nextX = rootX - hostX - dragOffsetX;
        float nextY = rootY - hostY - dragOffsetY;
        if (constrainToHost) {
            nextX = clamp(nextX, 0.0f, Math.max(0.0f, hostBounds.width() - width));
            nextY = clamp(nextY, 0.0f, Math.max(0.0f, hostBounds.height() - height));
        }
        float oldX = x;
        float oldY = y;
        position(nextX, nextY);
        dispatchWindowMoved(oldX, oldY, x, y);
    }

    private void stopDragging() {
        if (!dragging) return;
        UIContext context = uiContext();
        if (context != null && dragPointerId >= 0) {
            context.releasePointer(dragPointerId, this);
        }
        dragging = false;
        dragPointerId = -1;
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowMoveEnded();
    }

    private void startResizing(PointerPressedEvent pointer, ResizeHandle handle) {
        resizing = true;
        resizeHandle = handle == null ? ResizeHandle.NONE : handle;
        resizePointerId = pointer.pointerId();
        resizeStartRootX = pointer.rootX();
        resizeStartRootY = pointer.rootY();
        resizeStartX = x;
        resizeStartY = y;
        resizeStartWidth = Math.max(minWindowWidth, layoutBounds().width());
        resizeStartHeight = Math.max(minWindowHeight, layoutBounds().height());
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowResizeStarted();
    }

    private void updateResize(float rootX, float rootY) {
        if (!resizing || resizeHandle == ResizeHandle.NONE) return;
        float dx = rootX - resizeStartRootX;
        float dy = rootY - resizeStartRootY;
        float nextX = resizeStartX;
        float nextY = resizeStartY;
        float nextWidth = resizeStartWidth;
        float nextHeight = resizeStartHeight;

        if (resizeHandle.affectsLeft()) {
            nextX = resizeStartX + dx;
            nextWidth = resizeStartWidth - dx;
            if (nextWidth < minWindowWidth) {
                nextX -= minWindowWidth - nextWidth;
                nextWidth = minWindowWidth;
            }
        } else if (resizeHandle.affectsRight()) {
            nextWidth = resizeStartWidth + dx;
        }

        if (resizeHandle.affectsTop()) {
            nextY = resizeStartY + dy;
            nextHeight = resizeStartHeight - dy;
            if (nextHeight < minWindowHeight) {
                nextY -= minWindowHeight - nextHeight;
                nextHeight = minWindowHeight;
            }
        } else if (resizeHandle.affectsBottom()) {
            nextHeight = resizeStartHeight + dy;
        }

        nextWidth = Math.max(minWindowWidth, nextWidth);
        nextHeight = Math.max(minWindowHeight, nextHeight);

        if (constrainToHost && (hostBounds.width() > 0.0f || hostBounds.height() > 0.0f)) {
            if (resizeHandle.affectsLeft()) {
                float minX = 0.0f;
                if (nextX < minX) {
                    nextWidth += nextX - minX;
                    nextX = minX;
                }
            }
            if (resizeHandle.affectsTop()) {
                float minY = 0.0f;
                if (nextY < minY) {
                    nextHeight += nextY - minY;
                    nextY = minY;
                }
            }
            float maxWidth = Math.max(minWindowWidth, hostBounds.width() - nextX);
            float maxHeight = Math.max(minWindowHeight, hostBounds.height() - nextY);
            nextWidth = Math.min(nextWidth, maxWidth);
            nextHeight = Math.min(nextHeight, maxHeight);
        }

        float oldX = x;
        float oldY = y;
        float oldWidth = layoutBounds().width();
        float oldHeight = layoutBounds().height();
        setWindowRect(nextX, nextY, nextWidth, nextHeight);
        dispatchWindowResized(oldX, oldY, oldWidth, oldHeight, x, y, nextWidth, nextHeight);
    }

    private void stopResizing() {
        if (!resizing) return;
        UIContext context = uiContext();
        if (context != null && resizePointerId >= 0) {
            context.releasePointer(resizePointerId, this);
        }
        resizing = false;
        ResizeHandle completedHandle = resizeHandle;
        resizeHandle = ResizeHandle.NONE;
        resizePointerId = -1;
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowResizeEnded(completedHandle);
    }

    private void setWindowRect(float x, float y, float width, float height) {
        position(x, y);
        layout(style -> style.size(Math.max(minWindowWidth, width), Math.max(minWindowHeight, height)));
    }

    void setWindowManagerInternal(WindowManager windowManager) {
        if (this.windowManager == windowManager) return;
        this.windowManager = windowManager;
    }

    void setActiveInternal(boolean active) {
        if (this.active == active) return;
        this.active = active;
        invalidate(InvalidationFlags.VISUAL);
    }

    WindowActivatedEvent dispatchWindowActivated(WindowWidget previousWindow) {
        WindowActivatedEvent event = new WindowActivatedEvent(this, previousWindow);
        dispatchWindowEvent(event);
        return event;
    }

    WindowDeactivatedEvent dispatchWindowDeactivated(WindowWidget nextWindow) {
        WindowDeactivatedEvent event = new WindowDeactivatedEvent(this, nextWindow);
        dispatchWindowEvent(event);
        return event;
    }

    ModalOpenedEvent dispatchModalOpened(int stackDepth) {
        ModalOpenedEvent event = new ModalOpenedEvent(this, stackDepth);
        dispatchWindowEvent(event);
        return event;
    }

    ModalClosedEvent dispatchModalClosed(int stackDepth) {
        ModalClosedEvent event = new ModalClosedEvent(this, stackDepth);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowOpenedEvent dispatchWindowOpened() {
        WindowOpenedEvent event = new WindowOpenedEvent(this);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowClosedEvent dispatchWindowClosed() {
        WindowClosedEvent event = new WindowClosedEvent(this);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowMoveStartedEvent dispatchWindowMoveStarted() {
        WindowMoveStartedEvent event = new WindowMoveStartedEvent(this, x, y);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowMovedEvent dispatchWindowMoved(float oldX, float oldY, float newX, float newY) {
        if (oldX == newX && oldY == newY) return null;
        WindowMovedEvent event = new WindowMovedEvent(this, oldX, oldY, newX, newY);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowMoveEndedEvent dispatchWindowMoveEnded() {
        WindowMoveEndedEvent event = new WindowMoveEndedEvent(this, x, y);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowResizeStartedEvent dispatchWindowResizeStarted() {
        WindowResizeStartedEvent event = new WindowResizeStartedEvent(this,
                x, y, layoutBounds().width(), layoutBounds().height(), resizeHandle.publicName());
        dispatchWindowEvent(event);
        return event;
    }

    private WindowResizedEvent dispatchWindowResized(float oldX, float oldY, float oldWidth, float oldHeight,
                                                     float newX, float newY, float newWidth, float newHeight) {
        if (oldX == newX && oldY == newY && oldWidth == newWidth && oldHeight == newHeight) return null;
        WindowResizedEvent event = new WindowResizedEvent(this,
                oldX, oldY, oldWidth, oldHeight,
                newX, newY, newWidth, newHeight,
                resizeHandle.publicName());
        dispatchWindowEvent(event);
        return event;
    }

    private WindowResizeEndedEvent dispatchWindowResizeEnded(ResizeHandle completedHandle) {
        WindowResizeEndedEvent event = new WindowResizeEndedEvent(this,
                x, y, layoutBounds().width(), layoutBounds().height(),
                completedHandle == null ? "" : completedHandle.publicName());
        dispatchWindowEvent(event);
        return event;
    }

    private void dispatchWindowEvent(dev.sixik.unigui.api.event.WidgetEvent event) {
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
    }

    private boolean isInHeader(float rootX, float rootY) {
        RectView bounds = layoutBounds();
        return rootX >= bounds.x()
                && rootX <= bounds.x() + bounds.width()
                && rootY >= bounds.y()
                && rootY <= bounds.y() + Math.min(headerHeight, bounds.height());
    }

    private boolean canStartDraggingFrom(PointerPressedEvent pointer) {
        if (pointer.phase() != EventPhase.TARGET && pointer.phase() != EventPhase.BUBBLE) return false;
        if (!contains(layoutBounds(), pointer.rootX(), pointer.rootY())) return false;
        if (isInHeader(pointer.rootX(), pointer.rootY())) return true;
        return !routeContainsInputWidget(pointer.target(), pointer.rootX(), pointer.rootY());
    }

    private boolean effectiveDraggable() {
        return draggable && !(modal && fixedModal);
    }

    private boolean effectiveResizable() {
        return resizable && !(modal && fixedModal);
    }

    private boolean routeContainsInputWidget(Widget target, float rootX, float rootY) {
        Widget current = target;
        while (current != null && current != this) {
            if (isInputWidget(current, rootX, rootY)) return true;
            current = current.parent();
        }
        return false;
    }

    private boolean isInputWidget(Widget widget, float rootX, float rootY) {
        if (widget == null || widget == content) return false;
        if (widget instanceof TextWidget && !(widget instanceof TextInput)) return false;
        if (widget.focusable()) return true;
        MouseCursor cursor = widget.mouseCursorAt(rootX - widget.layoutBounds().x(), rootY - widget.layoutBounds().y());
        if (cursor != MouseCursor.DEFAULT) return true;
        return widget instanceof ScrollView
                || widget instanceof ScrollBar
                || widget instanceof Slider
                || widget instanceof ComboBox
                || widget instanceof DatePicker
                || widget instanceof ColorPicker
                || widget instanceof TreeView
                || widget instanceof VirtualTableView
                || widget instanceof ContextMenu
                || widget instanceof Popup
                || widget instanceof WindowWidget;
    }

    private static boolean contains(RectView bounds, float rootX, float rootY) {
        return bounds != null
                && rootX >= bounds.x()
                && rootX <= bounds.x() + bounds.width()
                && rootY >= bounds.y()
                && rootY <= bounds.y() + bounds.height();
    }

    private ResizeHandle resizeHandle(float rootX, float rootY) {
        if (!effectiveResizable() || !open) return ResizeHandle.NONE;
        RectView bounds = layoutBounds();
        if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) return ResizeHandle.NONE;
        boolean left = rootX >= bounds.x() && rootX < bounds.x() + RESIZE_HANDLE_SIZE;
        boolean right = rootX >= bounds.x() + bounds.width() - RESIZE_HANDLE_SIZE && rootX <= bounds.x() + bounds.width();
        boolean top = rootY >= bounds.y() && rootY < bounds.y() + RESIZE_HANDLE_SIZE;
        boolean bottom = rootY >= bounds.y() + bounds.height() - RESIZE_HANDLE_SIZE && rootY <= bounds.y() + bounds.height();
        if (left && top) return ResizeHandle.TOP_LEFT;
        if (right && top) return ResizeHandle.TOP_RIGHT;
        if (left && bottom) return ResizeHandle.BOTTOM_LEFT;
        if (right && bottom) return ResizeHandle.BOTTOM_RIGHT;
        if (left) return ResizeHandle.LEFT;
        if (right) return ResizeHandle.RIGHT;
        if (top) return ResizeHandle.TOP;
        if (bottom) return ResizeHandle.BOTTOM;
        return ResizeHandle.NONE;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum ResizeHandle {
        NONE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        private boolean affectsLeft() {
            return this == LEFT || this == TOP_LEFT || this == BOTTOM_LEFT;
        }

        private boolean affectsRight() {
            return this == RIGHT || this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }

        private boolean affectsTop() {
            return this == TOP || this == TOP_LEFT || this == TOP_RIGHT;
        }

        private boolean affectsBottom() {
            return this == BOTTOM || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }

        private String publicName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
