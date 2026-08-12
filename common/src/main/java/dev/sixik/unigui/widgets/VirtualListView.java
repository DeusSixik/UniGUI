package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.selection.IndexSelectionModel;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.virtualization.FixedRowVirtualizer;
import dev.sixik.unigui.api.virtualization.VirtualRange;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.VirtualListViewRenderer;
import dev.sixik.unigui.widgets.render.VirtualListViewRenderPhase;
import dev.sixik.unigui.widgets.render.VirtualListViewRowState;
import dev.sixik.unigui.widgets.render.VirtualListViewState;

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
    private static final float SCROLLBAR_WIDTH = ScrollBar.DEFAULT_SIZE;
    private static final float SCROLLBAR_GAP = ScrollBar.DEFAULT_GAP;
    private static final MutableColor SELECTED_ROW_COLOR = new MutableColor(0.18f, 0.45f, 0.75f, 0.35f);
    private static final MutableColor ACTIVE_ROW_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 0.70f);

    private final ScrollBar verticalScrollBar = new ScrollBar().orientation(Orientation.VERTICAL);
    private final FixedRowVirtualizer virtualizer = new FixedRowVirtualizer();
    private final IndexSelectionModel selection = new IndexSelectionModel();
    private final Map<Integer, Widget> realized = new LinkedHashMap<>();
    private final Map<Integer, Widget> recycled = new LinkedHashMap<>();
    private IntFunction<? extends Widget> itemFactory = index -> new Label(String.valueOf(index));
    private VirtualListViewRenderer renderer;
    private float scrollStep = 16.0f;
    private int offscreenCacheSize;
    private boolean consumeWheelAtScrollBounds = true;
    private int activeIndex = -1;

    public VirtualListView() {
        focusable(true);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.onValueChanged(event -> scrollTo(event.newValue()));
    }

    public int itemCount() {
        return virtualizer.itemCount();
    }

    public VirtualListView itemCount(int itemCount) {
        if (virtualizer.itemCount() == Math.max(0, itemCount)) return this;
        List<Integer> oldSelection = selection.selectedIndices();
        virtualizer.itemCount(itemCount);
        activeIndex = clampIndexOrNone(activeIndex);
        pruneRealized();
        pruneRecycled();
        emitSelectionChangeIfChanged(oldSelection, selection.retainWithin(virtualizer.itemCount()));
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float itemHeight() {
        return virtualizer.itemExtent();
    }

    public VirtualListView itemHeight(float itemHeight) {
        float previous = virtualizer.itemExtent();
        virtualizer.itemExtent(itemHeight);
        if (previous == virtualizer.itemExtent()) return this;
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int overscan() {
        return virtualizer.overscan();
    }

    public VirtualListView overscan(int overscan) {
        int previous = virtualizer.overscan();
        virtualizer.overscan(overscan);
        if (previous == virtualizer.overscan()) return this;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView itemFactory(IntFunction<? extends Widget> itemFactory) {
        this.itemFactory = itemFactory == null ? index -> new Label(String.valueOf(index)) : itemFactory;
        clearRealized();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int offscreenCacheSize() {
        return offscreenCacheSize;
    }

    public VirtualListView offscreenCacheSize(int offscreenCacheSize) {
        int normalized = Math.max(0, offscreenCacheSize);
        if (this.offscreenCacheSize == normalized) return this;
        this.offscreenCacheSize = normalized;
        trimRecycled();
        return this;
    }

    public float scrollY() {
        return virtualizer.scrollOffset();
    }

    public VirtualListView scrollStep(float scrollStep) {
        this.scrollStep = Float.isFinite(scrollStep) ? Math.max(1.0f, scrollStep) : 16.0f;
        syncScrollBar();
        return this;
    }

    public boolean consumeWheelAtScrollBounds() {
        return consumeWheelAtScrollBounds;
    }

    public VirtualListView consumeWheelAtScrollBounds(boolean consumeWheelAtScrollBounds) {
        this.consumeWheelAtScrollBounds = consumeWheelAtScrollBounds;
        return this;
    }

    public float contentHeight() {
        return virtualizer.contentExtent();
    }

    public float maxScrollY() {
        return Math.max(0.0f, contentHeight() - Math.max(0.0f, layoutBounds().height()));
    }

    public int firstVisibleIndex() {
        return realizedRange().firstIndex();
    }

    public int lastVisibleIndexExclusive() {
        return realizedRange().lastIndexExclusive();
    }

    public VirtualRange realizedRange() {
        return virtualizer.visibleRange();
    }

    public int realizedCount() {
        return realizedRange().count();
    }

    public SelectionMode selectionMode() {
        return selection.mode();
    }

    public VirtualListView selectionMode(SelectionMode mode) {
        List<Integer> oldSelection = selection.selectedIndices();
        selection.mode(mode);
        emitSelectionChangeIfChanged(oldSelection, !oldSelection.equals(selection.selectedIndices()));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int selectedIndex() {
        return selection.selectedIndex();
    }

    public List<Integer> selectedIndices() {
        return selection.selectedIndices();
    }

    public boolean isSelectedIndex(int index) {
        return selection.isSelected(index);
    }

    public int activeIndex() {
        return activeIndex;
    }

    public VirtualListView activeIndex(int index) {
        int normalized = clampIndexOrNone(index);
        if (activeIndex == normalized) return this;
        activeIndex = normalized;
        ensureActiveVisible();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView selectIndex(int index) {
        List<Integer> oldSelection = selection.selectedIndices();
        activeIndex = clampIndexOrNone(index);
        emitSelectionChangeIfChanged(oldSelection, selection.select(index));
        ensureActiveVisible();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView toggleIndex(int index) {
        List<Integer> oldSelection = selection.selectedIndices();
        emitSelectionChangeIfChanged(oldSelection, selection.toggle(index));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView clearSelection() {
        List<Integer> oldSelection = selection.selectedIndices();
        emitSelectionChangeIfChanged(oldSelection, selection.clear());
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    public ScrollBar verticalScrollBar() {
        return verticalScrollBar;
    }

    public VirtualListViewRenderer renderer() {
        return renderer;
    }

    public VirtualListView renderer(VirtualListViewRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView useDefaultRenderer() {
        return renderer(null);
    }

    public VirtualListView scrollTo(float y) {
        updateVirtualizerViewport();
        float before = virtualizer.scrollOffset();
        virtualizer.scrollOffset(y);
        if (before == virtualizer.scrollOffset()) return this;
        syncScrollBar();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualListView scrollBy(float dy) {
        return scrollTo(virtualizer.scrollOffset() + dy);
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
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        float desiredWidth = 0.0f;
        for (Widget item : realized.values()) {
            item.measure(context);
            desiredWidth = Math.max(desiredWidth, StackPanel.preferredWidth(item, 0.0f));
        }
        float desiredHeight = Math.min(contentHeight(), Math.max(0.0f, virtualizer.itemExtent() * 8.0f));
        setDesiredSize(resolveDesiredSize(context, desiredWidth, desiredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        updateVirtualizerViewport();
        virtualizer.scrollOffset(virtualizer.scrollOffset());
        updateRealizedWindow();
        arrangeItems();
        arrangeScrollBar();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
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
        pushOpacity(context);
        try {
            VirtualListViewRenderer activeRenderer = effectiveRenderer();
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            draw.pushClip(layoutBounds().x(), layoutBounds().y(), viewportWidth(), layoutBounds().height());
            activeRenderer.render(draw, snapshot(VirtualListViewRenderPhase.BACKGROUND));
            for (Map.Entry<Integer, Widget> entry : List.copyOf(realized.entrySet())) {
                Widget item = entry.getValue();
                if (item.visibility() == Visibility.VISIBLE) {
                    item.render(context);
                }
            }
            activeRenderer.render(draw, snapshot(VirtualListViewRenderPhase.FOREGROUND));
            draw.popClip();
            if (hasVerticalScrollBar()) {
                verticalScrollBar.render(context);
            }
        } finally {
            popOpacity(context);
        }
    }

    protected VirtualListViewRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(VirtualListViewRenderer.class, WidgetsRender.virtualListView()) : renderer;
    }

    protected VirtualListViewState snapshot(VirtualListViewRenderPhase phase) {
        List<VirtualListViewRowState> rows = new ArrayList<>(realized.size());
        for (Map.Entry<Integer, Widget> entry : realized.entrySet()) {
            Widget item = entry.getValue();
            if (item.visibility() != Visibility.VISIBLE) continue;
            rows.add(new VirtualListViewRowState(
                    entry.getKey(),
                    item.layoutBounds().x(),
                    item.layoutBounds().y(),
                    item.layoutBounds().width(),
                    item.layoutBounds().height(),
                    selection.isSelected(entry.getKey()),
                    entry.getKey() == activeIndex));
        }
        return new VirtualListViewState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                viewportWidth(),
                layoutBounds().height(),
                isFocused(),
                phase,
                SELECTED_ROW_COLOR.copy(),
                ACTIVE_ROW_COLOR.copy(),
                rows);
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float before = virtualizer.scrollOffset();
            scrollBy(-scroll.deltaY() * scrollStep);
            if (before != virtualizer.scrollOffset()) {
                event.cancel();
            } else if (consumeWheelAtScrollBounds && scroll.deltaY() != 0.0f && maxScrollY() > 0.0f) {
                event.cancel();
            }
        } else if (event instanceof KeyPressedEvent key && key.phase() != EventPhase.CAPTURE) {
            if (handleKey(key.keyCode(), key.modifiers())) {
                event.cancel();
            }
        } else if (event instanceof PointerPressedEvent pointer
                && pointer.phase() != EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY
                && localX(pointer) >= 0.0f
                && localX(pointer) < viewportWidth()
                && localY(pointer) >= 0.0f
                && localY(pointer) < layoutBounds().height()) {
            int index = indexAt(localY(pointer));
            if (index >= 0) {
                requestFocus();
                activeIndex = index;
                selectFromPointer(index, 0);
                event.cancel();
            }
        }
    }

    @Override
    public void dispose() {
        clearRealized();
    }

    private void updateRealizedWindow() {
        updateVirtualizerViewport();
        VirtualRange range = realizedRange();
        int first = range.firstIndex();
        int last = range.lastIndexExclusive();

        Iterator<Map.Entry<Integer, Widget>> iterator = realized.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Widget> entry = iterator.next();
            int index = entry.getKey();
            if (index < first || index >= last) {
                detachItem(entry.getValue());
                recycleItem(index, entry.getValue());
                iterator.remove();
            }
        }

        for (int index = first; index < last; index++) {
            realized.computeIfAbsent(index, this::realizeItem);
        }
    }

    private Widget realizeItem(int index) {
        Widget item = recycled.remove(index);
        if (item != null) {
            attachItem(item);
            return item;
        }
        return createItem(index);
    }

    private Widget createItem(int index) {
        Widget item = itemFactory.apply(index);
        if (item == null) {
            item = new Label(String.valueOf(index));
        }
        attachItem(item);
        return item;
    }

    private void recycleItem(int index, Widget item) {
        if (offscreenCacheSize <= 0 || index < 0 || index >= virtualizer.itemCount()) {
            item.dispose();
            return;
        }
        Widget previous = recycled.put(index, item);
        if (previous != null && previous != item) {
            previous.dispose();
        }
        trimRecycled();
    }

    private void arrangeItems() {
        float x = layoutBounds().x();
        float width = viewportWidth();
        for (Map.Entry<Integer, Widget> entry : realized.entrySet()) {
            int index = entry.getKey();
            Widget item = entry.getValue();
            float y = layoutBounds().y() + virtualizer.itemOffset(index);
            StackPanel.arrangeChild(item, x, y, width, virtualizer.itemExtent());
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
                .silentValue(virtualizer.scrollOffset());
    }

    private boolean hasVerticalScrollBar() {
        return maxScrollY() > 0.0f;
    }

    private float viewportWidth() {
        return Math.max(0.0f, layoutBounds().width()
                - (hasVerticalScrollBar() ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0.0f));
    }

    private void pruneRealized() {
        Iterator<Map.Entry<Integer, Widget>> iterator = realized.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Widget> entry = iterator.next();
            if (entry.getKey() >= virtualizer.itemCount()) {
                detachItem(entry.getValue());
                entry.getValue().dispose();
                iterator.remove();
            }
        }
    }

    private void pruneRecycled() {
        Iterator<Map.Entry<Integer, Widget>> iterator = recycled.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Widget> entry = iterator.next();
            if (entry.getKey() >= virtualizer.itemCount()) {
                entry.getValue().dispose();
                iterator.remove();
            }
        }
        trimRecycled();
    }

    private void trimRecycled() {
        if (offscreenCacheSize <= 0) {
            clearRecycled();
            return;
        }
        Iterator<Map.Entry<Integer, Widget>> iterator = recycled.entrySet().iterator();
        while (recycled.size() > offscreenCacheSize && iterator.hasNext()) {
            Map.Entry<Integer, Widget> entry = iterator.next();
            entry.getValue().dispose();
            iterator.remove();
        }
    }

    private void clearRealized() {
        for (Widget item : realized.values()) {
            detachItem(item);
            item.dispose();
        }
        realized.clear();
        clearRecycled();
    }

    private void clearRecycled() {
        for (Widget item : recycled.values()) {
            item.dispose();
        }
        recycled.clear();
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

    private void updateVirtualizerViewport() {
        virtualizer.viewportExtent(Math.max(0.0f, layoutBounds().height()));
    }

    private int indexAt(float localY) {
        int index = (int) Math.floor((localY + virtualizer.scrollOffset()) / virtualizer.itemExtent());
        return index >= 0 && index < virtualizer.itemCount() ? index : -1;
    }

    private float localX(PointerPressedEvent pointer) {
        return pointer.rootX() - layoutBounds().x();
    }

    private float localY(PointerPressedEvent pointer) {
        return pointer.rootY() - layoutBounds().y();
    }

    private void selectFromPointer(int index, int modifiers) {
        List<Integer> oldSelection = selection.selectedIndices();
        boolean changed;
        if (KeyModifiers.has(modifiers, KeyModifiers.SHIFT)) {
            changed = selection.selectRange(index);
        } else if (KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
            changed = selection.toggle(index);
        } else {
            changed = selection.select(index);
        }
        emitSelectionChangeIfChanged(oldSelection, changed);
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean handleKey(int keyCode, int modifiers) {
        if (virtualizer.itemCount() <= 0) return false;
        int target = switch (keyCode) {
            case KeyCodes.UP -> navigationBaseIndex() - 1;
            case KeyCodes.DOWN -> navigationBaseIndex() + 1;
            case KeyCodes.HOME -> 0;
            case KeyCodes.END -> virtualizer.itemCount() - 1;
            case KeyCodes.PAGE_UP -> navigationBaseIndex() - pageRowCount();
            case KeyCodes.PAGE_DOWN -> navigationBaseIndex() + pageRowCount();
            case KeyCodes.SPACE, KeyCodes.ENTER, KeyCodes.KEYPAD_ENTER -> {
                selectActive(KeyModifiers.has(modifiers, KeyModifiers.CONTROL), KeyModifiers.has(modifiers, KeyModifiers.SHIFT));
                yield activeIndex;
            }
            default -> Integer.MIN_VALUE;
        };
        if (target == Integer.MIN_VALUE) return false;
        if (target != activeIndex) {
            moveActive(target, modifiers);
        }
        return true;
    }

    private void moveActive(int index, int modifiers) {
        int target = clampIndexOrNone(index);
        if (target < 0) return;
        activeIndex = target;
        ensureActiveVisible();
        if (!KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
            List<Integer> oldSelection = selection.selectedIndices();
            boolean changed = KeyModifiers.has(modifiers, KeyModifiers.SHIFT)
                    ? selection.selectRange(target)
                    : selection.select(target);
            emitSelectionChangeIfChanged(oldSelection, changed);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void selectActive(boolean toggle, boolean range) {
        if (activeIndex < 0) {
            activeIndex = navigationBaseIndex();
        }
        if (activeIndex < 0) return;
        List<Integer> oldSelection = selection.selectedIndices();
        boolean changed = range ? selection.selectRange(activeIndex) : (toggle ? selection.toggle(activeIndex) : selection.select(activeIndex));
        emitSelectionChangeIfChanged(oldSelection, changed);
        ensureActiveVisible();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private int navigationBaseIndex() {
        if (activeIndex >= 0 && activeIndex < virtualizer.itemCount()) return activeIndex;
        int selected = selection.selectedIndex();
        if (selected >= 0 && selected < virtualizer.itemCount()) return selected;
        return Math.min(Math.max(0, firstVisibleIndex()), Math.max(0, virtualizer.itemCount() - 1));
    }

    private int pageRowCount() {
        float extent = Math.max(1.0f, virtualizer.itemExtent());
        return Math.max(1, (int) Math.floor(Math.max(extent, layoutBounds().height()) / extent));
    }

    private void ensureActiveVisible() {
        if (activeIndex < 0) return;
        updateVirtualizerViewport();
        float itemTop = activeIndex * virtualizer.itemExtent();
        float itemBottom = itemTop + virtualizer.itemExtent();
        float viewportTop = virtualizer.scrollOffset();
        float viewportBottom = viewportTop + Math.max(0.0f, layoutBounds().height());
        if (itemTop < viewportTop) {
            scrollTo(itemTop);
        } else if (itemBottom > viewportBottom) {
            scrollTo(itemBottom - Math.max(0.0f, layoutBounds().height()));
        }
    }

    private int clampIndexOrNone(int index) {
        return index >= 0 && index < virtualizer.itemCount() ? index : -1;
    }

    private void requestFocus() {
        UIContext context = uiContext();
        if (context != null) {
            context.focusManager().requestFocus(this);
        }
    }

    private boolean isFocused() {
        UIContext context = uiContext();
        return context != null && context.focusManager().isFocused(this);
    }

    private void emitSelectionChangeIfChanged(List<Integer> oldSelection, boolean changed) {
        if (changed) {
            emit(new SelectionChangedEvent(this, oldSelection, selection.selectedIndices()));
        }
    }
}
