package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.v3.LayoutV3ScrollAdapter;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScrollView extends WidgetBase {
    private static final float SCROLLBAR_SIZE = ScrollBar.DEFAULT_SIZE;

    private final MutableColor scrollbarTrackColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.28f);
    private final MutableColor scrollbarThumbColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.75f);
    private final ScrollBar horizontalScrollBar = new ScrollBar().orientation(Orientation.HORIZONTAL);
    private final ScrollBar verticalScrollBar = new ScrollBar().orientation(Orientation.VERTICAL);
    private Widget content;
    private float contentWidth;
    private float contentHeight;
    private float measuredContentWidth;
    private float measuredContentHeight;
    private float scrollX;
    private float scrollY;
    private float scrollStep = 16.0f;
    private float scrollbarGap = ScrollBar.DEFAULT_GAP;
    private boolean scrollingEnabled = true;
    private boolean consumeWheelAtScrollBounds = true;
    private boolean horizontalScrollBarVisible;
    private boolean verticalScrollBarVisible;

    public ScrollView() {
        layout(style -> style
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.AUTO));
        scrollbarTrackColor.onChanged(() -> {
            horizontalScrollBar.trackColor().set(scrollbarTrackColor);
            verticalScrollBar.trackColor().set(scrollbarTrackColor);
            invalidate(InvalidationFlags.VISUAL);
        });
        scrollbarThumbColor.onChanged(() -> {
            horizontalScrollBar.thumbColor().set(scrollbarThumbColor);
            verticalScrollBar.thumbColor().set(scrollbarThumbColor);
            invalidate(InvalidationFlags.VISUAL);
        });
        horizontalScrollBar.setParentInternal(this);
        verticalScrollBar.setParentInternal(this);
        horizontalScrollBar.trackColor().set(scrollbarTrackColor);
        horizontalScrollBar.thumbColor().set(scrollbarThumbColor);
        verticalScrollBar.trackColor().set(scrollbarTrackColor);
        verticalScrollBar.thumbColor().set(scrollbarThumbColor);
        horizontalScrollBar.onValueChanged(event -> scrollTo(event.newValue(), scrollY));
        verticalScrollBar.onValueChanged(event -> scrollTo(scrollX, event.newValue()));
    }

    public ScrollView(Widget content) {
        this();
        content(content);
    }

    public Widget content() {
        return content;
    }

    public ScrollView content(Widget content) {
        if (this.content == content) return this;
        if (content == this) throw new IllegalArgumentException("ScrollView cannot contain itself");
        detachContent();
        this.content = content;
        attachContent(content);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float contentWidth() {
        return contentWidth;
    }

    public float contentHeight() {
        return contentHeight;
    }

    public ScrollView contentSize(float width, float height) {
        float normalizedWidth = Math.max(0.0f, width);
        float normalizedHeight = Math.max(0.0f, height);
        if (contentWidth == normalizedWidth && contentHeight == normalizedHeight) return this;
        contentWidth = normalizedWidth;
        contentHeight = normalizedHeight;
        scrollTo(scrollX, scrollY);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float scrollX() {
        return scrollX;
    }

    public float scrollY() {
        return scrollY;
    }

    public float scrollStep() {
        return scrollStep;
    }

    public ScrollView scrollStep(float scrollStep) {
        this.scrollStep = Math.max(1.0f, scrollStep);
        return this;
    }

    public boolean scrollingEnabled() {
        return scrollingEnabled;
    }

    public ScrollView scrollingEnabled(boolean scrollingEnabled) {
        if (this.scrollingEnabled == scrollingEnabled) return this;
        this.scrollingEnabled = scrollingEnabled;
        if (!scrollingEnabled) {
            horizontalScrollBarVisible = false;
            verticalScrollBarVisible = false;
            scrollX = 0.0f;
            scrollY = 0.0f;
            syncScrollBars();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ScrollView disableScrolling() {
        return scrollingEnabled(false);
    }

    public ScrollView enableScrolling() {
        return scrollingEnabled(true);
    }

    public boolean consumeWheelAtScrollBounds() {
        return consumeWheelAtScrollBounds;
    }

    public ScrollView consumeWheelAtScrollBounds(boolean consumeWheelAtScrollBounds) {
        this.consumeWheelAtScrollBounds = consumeWheelAtScrollBounds;
        return this;
    }

    public float scrollbarGap() {
        return scrollbarGap;
    }

    public ScrollView scrollbarGap(float scrollbarGap) {
        float normalized = Float.isFinite(scrollbarGap) ? Math.max(0.0f, scrollbarGap) : ScrollBar.DEFAULT_GAP;
        if (this.scrollbarGap == normalized) return this;
        this.scrollbarGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ScrollView scrollTo(float x, float y) {
        float clampedX = clamp(x, 0.0f, maxScrollX());
        float clampedY = clamp(y, 0.0f, maxScrollY());
        if (scrollX == clampedX && scrollY == clampedY) return this;
        scrollX = clampedX;
        scrollY = clampedY;
        syncScrollBars();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ScrollView scrollBy(float dx, float dy) {
        return scrollTo(scrollX + dx, scrollY + dy);
    }

    public float maxScrollX() {
        if (!scrollingEnabled || !horizontalScrollingEnabled()) return 0.0f;
        return Math.max(0.0f, effectiveContentWidth() - viewportWidth());
    }

    public float maxScrollY() {
        if (!scrollingEnabled || !verticalScrollingEnabled()) return 0.0f;
        return Math.max(0.0f, effectiveContentHeight() - viewportHeight());
    }

    public MutableColor scrollbarTrackColor() {
        return scrollbarTrackColor;
    }

    public MutableColor scrollbarThumbColor() {
        return scrollbarThumbColor;
    }

    public ScrollBar verticalScrollBar() {
        return verticalScrollBar;
    }

    public ScrollBar horizontalScrollBar() {
        return horizontalScrollBar;
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        attachContent(content);
        horizontalScrollBar.setParentInternal(this);
        horizontalScrollBar.setUiContextInternal(uiContext);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.setUiContextInternal(uiContext);
    }

    @Override
    public List<Widget> children() {
        boolean horizontalVisible = showsHorizontalScrollBar();
        boolean verticalVisible = showsVerticalScrollBar();
        if (content == null && !horizontalVisible && !verticalVisible) return Collections.emptyList();

        List<Widget> children = new ArrayList<>(3);
        if (content != null) {
            children.add(content);
        }
        if (horizontalVisible) {
            children.add(horizontalScrollBar);
        }
        if (verticalVisible) {
            children.add(verticalScrollBar);
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        if (content != null) {
            LayoutV3ScrollAdapter.Extent extent = LayoutV3ScrollAdapter.measureContent(
                    content,
                    context,
                    horizontalScrollingEnabled(),
                    verticalScrollingEnabled());
            measuredContentWidth = extent.contentWidth();
            measuredContentHeight = extent.contentHeight();
            float desiredWidth = contentWidth > 0.0f ? contentWidth : measuredContentWidth;
            float desiredHeight = contentHeight > 0.0f ? contentHeight : measuredContentHeight;
            setDesiredSize(resolveDesiredSize(context, desiredWidth, desiredHeight));
        } else {
            measuredContentWidth = 0.0f;
            measuredContentHeight = 0.0f;
            setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        updateScrollBarVisibility();
        scrollTo(scrollX, scrollY);
        arrangeContent();
        arrangeScrollBars();
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (content != null) {
            content.tick(frame);
        }
        if (showsHorizontalScrollBar()) {
            horizontalScrollBar.tick(frame);
        }
        if (showsVerticalScrollBar()) {
            verticalScrollBar.tick(frame);
        }
    }

    @Override
    public void render(RenderContext context) {
        pushOpacity(context);
        try {
            if (content != null) {
                boolean clipsContent = clipsContent();
                if (clipsContent) {
                    context.pushClip(layoutBounds().x(), layoutBounds().y(), viewportWidth(), viewportHeight());
                }
                try {
                    content.render(context);
                } finally {
                    if (clipsContent) {
                        context.popClip();
                    }
                }
            }
            if (showsHorizontalScrollBar()) {
                horizontalScrollBar.render(context);
            }
            if (showsVerticalScrollBar()) {
                verticalScrollBar.render(context);
            }
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (scrollingEnabled && event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float beforeX = scrollX;
            float beforeY = scrollY;
            boolean shiftHorizontal = KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT)
                    && maxScrollX() > 0.0f
                    && scroll.deltaY() != 0.0f;
            if (shiftHorizontal) {
                scrollBy(-scroll.deltaY() * scrollStep, 0.0f);
            } else {
                scrollBy(-scroll.deltaX() * scrollStep, -scroll.deltaY() * scrollStep);
            }
            if (beforeX != scrollX || beforeY != scrollY) {
                syncScrollBars();
                event.cancel();
            } else if (consumeWheelAtScrollBounds && canScrollWheel(scroll)) {
                event.cancel();
            }
        }
    }

    @Override
    public void dispose() {
        if (content != null) {
            content.dispose();
        }
        detachContent();
        content = null;
        horizontalScrollBar.setParentInternal(null);
        horizontalScrollBar.setUiContextInternal(null);
        verticalScrollBar.setParentInternal(null);
        verticalScrollBar.setUiContextInternal(null);
    }

    private void arrangeContent() {
        if (content == null) return;
        StackPanel.arrangeChild(content,
                layoutBounds().x() - scrollX,
                layoutBounds().y() - scrollY,
                effectiveContentWidth(),
                effectiveContentHeight());
    }

    private void arrangeScrollBars() {
        if (showsHorizontalScrollBar()) {
            horizontalScrollBar.arrange(new MutableRect(
                    layoutBounds().x(),
                    layoutBounds().y() + viewportHeight() + scrollbarGap,
                    viewportWidth(),
                    SCROLLBAR_SIZE));
        }
        if (showsVerticalScrollBar()) {
            verticalScrollBar.arrange(new MutableRect(
                    layoutBounds().x() + viewportWidth() + scrollbarGap,
                    layoutBounds().y(),
                    SCROLLBAR_SIZE,
                    viewportHeight()));
        }
        syncScrollBars();
    }

    private void syncScrollBars() {
        horizontalScrollBar
                .range(0.0f, maxScrollX())
                .pageSize(Math.max(1.0f, viewportWidth()))
                .step(scrollStep)
                .silentValue(scrollX);
        verticalScrollBar
                .range(0.0f, maxScrollY())
                .pageSize(Math.max(1.0f, viewportHeight()))
                .step(scrollStep)
                .silentValue(scrollY);
    }

    private void updateScrollBarVisibility() {
        if (!scrollingEnabled) {
            horizontalScrollBarVisible = false;
            verticalScrollBarVisible = false;
            return;
        }
        boolean horizontal = layoutStyle().overflowX() == Overflow.SCROLL;
        boolean vertical = layoutStyle().overflowY() == Overflow.SCROLL;
        float width = Math.max(0.0f, layoutBounds().width());
        float height = Math.max(0.0f, layoutBounds().height());

        for (int pass = 0; pass < 4; pass++) {
            float candidateWidth = Math.max(0.0f, width - (vertical ? scrollbarReservation() : 0.0f));
            float candidateHeight = Math.max(0.0f, height - (horizontal ? scrollbarReservation() : 0.0f));
            boolean nextHorizontal = layoutStyle().overflowX() == Overflow.SCROLL
                    || (layoutStyle().overflowX() == Overflow.AUTO
                    && rawContentWidth() > candidateWidth);
            boolean nextVertical = layoutStyle().overflowY() == Overflow.SCROLL
                    || (layoutStyle().overflowY() == Overflow.AUTO
                    && rawContentHeight() > candidateHeight);
            if (horizontal == nextHorizontal && vertical == nextVertical) break;
            horizontal = nextHorizontal;
            vertical = nextVertical;
        }

        horizontalScrollBarVisible = horizontal;
        verticalScrollBarVisible = vertical;
    }

    private float viewportWidth() {
        return Math.max(0.0f, layoutBounds().width()
                - (showsVerticalScrollBar() ? scrollbarReservation() : 0.0f));
    }

    private float viewportHeight() {
        return Math.max(0.0f, layoutBounds().height()
                - (showsHorizontalScrollBar() ? scrollbarReservation() : 0.0f));
    }

    private float scrollbarReservation() {
        return SCROLLBAR_SIZE + scrollbarGap;
    }

    private float effectiveContentWidth() {
        if (!horizontalScrollingEnabled()) {
            return viewportWidth();
        }
        if (contentWidth > 0.0f) {
            return contentWidth;
        }
        return Math.max(measuredContentWidth, viewportWidth());
    }

    private float effectiveContentHeight() {
        if (!verticalScrollingEnabled()) {
            return viewportHeight();
        }
        if (contentHeight > 0.0f) {
            return contentHeight;
        }
        return Math.max(measuredContentHeight, viewportHeight());
    }

    private float rawContentWidth() {
        return contentWidth > 0.0f ? contentWidth : measuredContentWidth;
    }

    private float rawContentHeight() {
        return contentHeight > 0.0f ? contentHeight : measuredContentHeight;
    }

    private boolean horizontalScrollingEnabled() {
        if (!scrollingEnabled) return false;
        Overflow overflow = layoutStyle().overflowX();
        return overflow == Overflow.AUTO || overflow == Overflow.SCROLL;
    }

    private boolean verticalScrollingEnabled() {
        if (!scrollingEnabled) return false;
        Overflow overflow = layoutStyle().overflowY();
        return overflow == Overflow.AUTO || overflow == Overflow.SCROLL;
    }

    private boolean canScrollWheel(ScrollEvent scroll) {
        if (scroll.deltaX() != 0.0f && maxScrollX() > 0.0f) return true;
        if (scroll.deltaY() != 0.0f && maxScrollY() > 0.0f) return true;
        return KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT)
                && scroll.deltaY() != 0.0f
                && maxScrollX() > 0.0f;
    }

    private boolean showsHorizontalScrollBar() {
        return layoutStyle().overflowX() == Overflow.SCROLL
                || (layoutStyle().overflowX() == Overflow.AUTO && horizontalScrollBarVisible);
    }

    private boolean showsVerticalScrollBar() {
        return layoutStyle().overflowY() == Overflow.SCROLL
                || (layoutStyle().overflowY() == Overflow.AUTO && verticalScrollBarVisible);
    }

    private boolean clipsContent() {
        return layoutStyle().overflowX() != Overflow.VISIBLE
                || layoutStyle().overflowY() != Overflow.VISIBLE;
    }

    private void attachContent(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void detachContent() {
        Widget previous = content;
        content = null;
        if (previous instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
