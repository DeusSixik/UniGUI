package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Collections;
import java.util.List;

public class ScrollView extends WidgetBase {
    private static final float SCROLLBAR_WIDTH = 4.0f;

    private final MutableColor scrollbarTrackColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.28f);
    private final MutableColor scrollbarThumbColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.75f);
    private Widget content;
    private float contentWidth;
    private float contentHeight;
    private float scrollX;
    private float scrollY;
    private float scrollStep = 16.0f;

    public ScrollView() {
        scrollbarTrackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        scrollbarThumbColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
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

    public ScrollView scrollTo(float x, float y) {
        float clampedX = clamp(x, 0.0f, maxScrollX());
        float clampedY = clamp(y, 0.0f, maxScrollY());
        if (scrollX == clampedX && scrollY == clampedY) return this;
        scrollX = clampedX;
        scrollY = clampedY;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ScrollView scrollBy(float dx, float dy) {
        return scrollTo(scrollX + dx, scrollY + dy);
    }

    public float maxScrollX() {
        return Math.max(0.0f, effectiveContentWidth() - Math.max(0.0f, layoutBounds().width()));
    }

    public float maxScrollY() {
        return Math.max(0.0f, effectiveContentHeight() - Math.max(0.0f, layoutBounds().height()));
    }

    public MutableColor scrollbarTrackColor() {
        return scrollbarTrackColor;
    }

    public MutableColor scrollbarThumbColor() {
        return scrollbarThumbColor;
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        attachContent(content);
    }

    @Override
    public List<Widget> children() {
        return content == null ? Collections.emptyList() : List.of(content);
    }

    @Override
    public void measure(LayoutContext context) {
        if (content != null) {
            content.measure(context);
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        scrollTo(scrollX, scrollY);
        arrangeContent();
    }

    @Override
    public void tick(FrameContext frame) {
        if (content != null) {
            content.tick(frame);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (content != null) {
            context.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
            content.render(context);
            context.popClip();
        }
        renderScrollbars(context);
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float beforeX = scrollX;
            float beforeY = scrollY;
            scrollBy(-scroll.deltaX() * scrollStep, -scroll.deltaY() * scrollStep);
            if (beforeX != scrollX || beforeY != scrollY) {
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
    }

    private void arrangeContent() {
        if (content == null) return;
        content.arrange(new MutableRect(
                layoutBounds().x() - scrollX,
                layoutBounds().y() - scrollY,
                effectiveContentWidth(),
                effectiveContentHeight()));
    }

    private void renderScrollbars(RenderContext context) {
        float maxY = maxScrollY();
        if (maxY <= 0.0f) return;

        float x = layoutBounds().x() + layoutBounds().width() - SCROLLBAR_WIDTH;
        float y = layoutBounds().y();
        float height = Math.max(1.0f, layoutBounds().height());
        float contentExtent = Math.max(height, effectiveContentHeight());
        float thumbHeight = Math.max(8.0f, height * (height / contentExtent));
        float thumbY = y + (height - thumbHeight) * (scrollY / maxY);

        context.rect(x, y, SCROLLBAR_WIDTH, height, Paint.fill(scrollbarTrackColor), transform());
        context.roundedRect(x, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH * 0.5f, Paint.fill(scrollbarThumbColor), transform());
    }

    private float effectiveContentWidth() {
        return contentWidth > 0.0f ? contentWidth : Math.max(0.0f, layoutBounds().width());
    }

    private float effectiveContentHeight() {
        return contentHeight > 0.0f ? contentHeight : Math.max(0.0f, layoutBounds().height());
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
