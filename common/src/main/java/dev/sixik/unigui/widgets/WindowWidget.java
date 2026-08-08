package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
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

    private final Button closeButton = new Button("x");
    private final MutableColor headerColor = new MutableColor(0.075f, 0.090f, 0.125f, 0.98f);
    private final MutableColor headerSeparatorColor = new MutableColor(0.22f, 0.24f, 0.30f, 0.95f);
    private final MutableColor titleColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private WindowRenderer renderer;
    private String title = "";
    private RichText richTitle = RichText.plain("");
    private Widget content;
    private boolean open;
    private boolean draggable = true;
    private boolean dragging;
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
    private final MutableRect hostBounds = new MutableRect();

    public WindowWidget() {
        backgroundVisible(true);
        borderVisible(true);
        radius(4.0f);
        background().set(0.030f, 0.035f, 0.050f, 0.98f);
        borderColor().set(0.25f, 0.78f, 1.0f, 0.85f);
        closeButton.preferredSize(18.0f, 18.0f).grow(0.0f);
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
        visible(open);
        if (!open) {
            stopDragging();
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
        if (!draggable) {
            stopDragging();
        }
        return this;
    }

    public boolean dragging() {
        return dragging;
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
                && pointer.phase() == EventPhase.TARGET
                && pointer.button() == PointerButton.PRIMARY
                && draggable
                && isInHeader(pointer.rootX(), pointer.rootY())) {
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
    protected void renderContent(RenderContext context) {
        effectiveRenderer().render(new DrawScope(context, transform()), snapshot(context));
        super.renderContent(context);
    }

    private WindowRenderer effectiveRenderer() {
        return renderer == null ? WidgetsRender.window() : renderer;
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
                titleColor.copy());
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
        position(nextX, nextY);
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
    }

    private boolean isInHeader(float rootX, float rootY) {
        RectView bounds = layoutBounds();
        return rootX >= bounds.x()
                && rootX <= bounds.x() + bounds.width()
                && rootY >= bounds.y()
                && rootY <= bounds.y() + Math.min(headerHeight, bounds.height());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
