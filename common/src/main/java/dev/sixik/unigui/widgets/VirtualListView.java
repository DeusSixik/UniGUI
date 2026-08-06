package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Fixed-row-height virtualized list for large scrollable datasets.
 */
public class VirtualListView extends WidgetBase {
    private static final float SCROLLBAR_WIDTH = 6.0f;

    private final ScrollBar verticalScrollBar = new ScrollBar().orientation(Orientation.VERTICAL);
    private final Map<Integer, Widget> realized = new LinkedHashMap<>();
    private IntFunction<? extends Widget> itemFactory = index -> new Label(String.valueOf(index));
    private int itemCount;
    private float itemHeight = 18.0f;
    private int overscan = 1;
    private float scrollY;
    private float scrollStep = 16.0f;
    private int firstVisibleIndex;
    private int lastVisibleIndexExclusive;

    public VirtualListView() {
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.onValueChanged(event -> scrollTo(event.newValue()));
    }

    public int itemCount() {
        return itemCount;
    }

    public VirtualListView itemCount(int itemCount) {
        int normalized = Math.max(0, itemCount);
        if (this.itemCount == normalized) return this;
        this.itemCount = normalized;
        pruneRealized();
        scrollTo(scrollY);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float itemHeight() {
        return itemHeight;
    }

    public VirtualListView itemHeight(float itemHeight) {
        float normalized = Float.isFinite(itemHeight) ? Math.max(1.0f, itemHeight) : 18.0f;
        if (this.itemHeight == normalized) return this;
        this.itemHeight = normalized;
        scrollTo(scrollY);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int overscan() {
        return overscan;
    }

    public VirtualListView overscan(int overscan) {
        int normalized = Math.max(0, overscan);
        if (this.overscan == normalized) return this;
        this.overscan = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView itemFactory(IntFunction<? extends Widget> itemFactory) {
        this.itemFactory = itemFactory == null ? index -> new Label(String.valueOf(index)) : itemFactory;
        clearRealized();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float scrollY() {
        return scrollY;
    }

    public VirtualListView scrollStep(float scrollStep) {
        this.scrollStep = Float.isFinite(scrollStep) ? Math.max(1.0f, scrollStep) : 16.0f;
        syncScrollBar();
        return this;
    }

    public float contentHeight() {
        return itemCount * itemHeight;
    }

    public float maxScrollY() {
        return Math.max(0.0f, contentHeight() - Math.max(0.0f, layoutBounds().height()));
    }

    public int firstVisibleIndex() {
        return firstVisibleIndex;
    }

    public int lastVisibleIndexExclusive() {
        return lastVisibleIndexExclusive;
    }

    public int realizedCount() {
        return realized.size();
    }

    public ScrollBar verticalScrollBar() {
        return verticalScrollBar;
    }

    public VirtualListView scrollTo(float y) {
        float clamped = clamp(y, 0.0f, maxScrollY());
        if (scrollY == clamped) return this;
        scrollY = clamped;
        syncScrollBar();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView scrollBy(float dy) {
        return scrollTo(scrollY + dy);
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.setUiContextInternal(uiContext);
        for (Widget item : realized.values()) {
            attachItem(item);
        }
    }

    @Override
    public List<Widget> children() {
        if (realized.isEmpty() && !hasVerticalScrollBar()) return Collections.emptyList();
        List<Widget> children = new ArrayList<>(realized.size() + 1);
        children.addAll(realized.values());
        if (hasVerticalScrollBar()) {
            children.add(verticalScrollBar);
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public void measure(LayoutContext context) {
        for (Widget item : realized.values()) {
            item.measure(context);
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        scrollTo(scrollY);
        updateRealizedWindow();
        arrangeItems();
        arrangeScrollBar();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        for (Widget item : List.copyOf(realized.values())) {
            if (item.visibility() == Visibility.VISIBLE) {
                item.tick(frame);
            }
        }
        if (hasVerticalScrollBar()) {
            verticalScrollBar.tick(frame);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        context.pushClip(layoutBounds().x(), layoutBounds().y(), viewportWidth(), layoutBounds().height());
        for (Widget item : List.copyOf(realized.values())) {
            if (item.visibility() == Visibility.VISIBLE) {
                item.render(context);
            }
        }
        context.popClip();
        if (hasVerticalScrollBar()) {
            verticalScrollBar.render(context);
        }
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float before = scrollY;
            scrollBy(-scroll.deltaY() * scrollStep);
            if (before != scrollY) {
                event.cancel();
            }
        }
    }

    @Override
    public void dispose() {
        clearRealized();
    }

    private void updateRealizedWindow() {
        int first = itemCount == 0 ? 0 : clampIndex((int) Math.floor(scrollY / itemHeight) - overscan);
        int visibleRows = (int) Math.ceil(Math.max(0.0f, layoutBounds().height()) / itemHeight) + overscan * 2 + 1;
        int last = Math.min(itemCount, first + Math.max(0, visibleRows));
        firstVisibleIndex = first;
        lastVisibleIndexExclusive = last;

        Iterator<Map.Entry<Integer, Widget>> iterator = realized.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Widget> entry = iterator.next();
            int index = entry.getKey();
            if (index < first || index >= last) {
                detachItem(entry.getValue());
                entry.getValue().dispose();
                iterator.remove();
            }
        }

        for (int index = first; index < last; index++) {
            realized.computeIfAbsent(index, this::createItem);
        }
    }

    private Widget createItem(int index) {
        Widget item = itemFactory.apply(index);
        if (item == null) {
            item = new Label(String.valueOf(index));
        }
        attachItem(item);
        return item;
    }

    private void arrangeItems() {
        float x = layoutBounds().x();
        float width = viewportWidth();
        for (Map.Entry<Integer, Widget> entry : realized.entrySet()) {
            int index = entry.getKey();
            Widget item = entry.getValue();
            float y = layoutBounds().y() + index * itemHeight - scrollY;
            item.arrange(new MutableRect(x, y, width, itemHeight));
        }
    }

    private void arrangeScrollBar() {
        if (!hasVerticalScrollBar()) return;
        verticalScrollBar.arrange(new MutableRect(
                layoutBounds().x() + layoutBounds().width() - SCROLLBAR_WIDTH,
                layoutBounds().y(),
                SCROLLBAR_WIDTH,
                layoutBounds().height()));
        syncScrollBar();
    }

    private void syncScrollBar() {
        verticalScrollBar
                .range(0.0f, maxScrollY())
                .pageSize(Math.max(1.0f, layoutBounds().height()))
                .step(scrollStep)
                .silentValue(scrollY);
    }

    private boolean hasVerticalScrollBar() {
        return maxScrollY() > 0.0f;
    }

    private float viewportWidth() {
        return Math.max(0.0f, layoutBounds().width() - (hasVerticalScrollBar() ? SCROLLBAR_WIDTH : 0.0f));
    }

    private void pruneRealized() {
        Iterator<Map.Entry<Integer, Widget>> iterator = realized.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Widget> entry = iterator.next();
            if (entry.getKey() >= itemCount) {
                detachItem(entry.getValue());
                entry.getValue().dispose();
                iterator.remove();
            }
        }
    }

    private void clearRealized() {
        for (Widget item : realized.values()) {
            detachItem(item);
            item.dispose();
        }
        realized.clear();
    }

    private void attachItem(Widget item) {
        if (item instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private static void detachItem(Widget item) {
        if (item instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(itemCount, index));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
