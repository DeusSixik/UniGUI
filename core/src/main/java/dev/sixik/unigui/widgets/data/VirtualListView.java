package dev.sixik.unigui.widgets.data;

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
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.selection.IndexSelectionModel;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.VirtualListViewMode;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.virtualization.FixedRowVirtualizer;
import dev.sixik.unigui.api.virtualization.VirtualRange;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Виртуализированное представление большого набора элементов в виде списка или сетки.
 *
 * <p>Геометрия и поведение могут задаваться общим {@code StylePack} через ключи
 * {@link StyleKeys#VIRTUAL_LIST_VIEW_MODE}, {@link StyleKeys#VIRTUAL_LIST_GRID_CELL_WIDTH},
 * {@link StyleKeys#VIRTUAL_LIST_GRID_CELL_HEIGHT} и остальные
 * {@code VIRTUAL_LIST_*} свойства. Fluent-методы записывают те же ключи в inline-style
 * конкретного экземпляра и поэтому имеют приоритет над темой.</p>
 *
 * <pre>{@code
 * StylePack pack = StylePack.create("files")
 *         .putStyle(VirtualListView.STYLE_TYPE, new MutableStyle()
 *                 .put(StyleKeys.VIRTUAL_LIST_VIEW_MODE, VirtualListViewMode.GRID)
 *                 .put(StyleKeys.VIRTUAL_LIST_GRID_CELL_WIDTH, 120.0f)
 *                 .put(StyleKeys.VIRTUAL_LIST_GRID_CELL_HEIGHT, 140.0f)
 *                 .put(StyleKeys.VIRTUAL_LIST_COLUMN_GAP, 8.0f));
 * }</pre>
 */
@XmlWidgetName("VirtualListView")
public class VirtualListView extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.VIRTUAL_LIST_VIEW;

    public enum ViewMode {
        GRID,
        LIST
    }

    private static final float SCROLLBAR_WIDTH = ScrollBar.DEFAULT_SIZE;
    private static final float SCROLLBAR_GAP = ScrollBar.DEFAULT_GAP;
    private static final MutableColor SELECTED_ROW_COLOR = new MutableColor(0.18f, 0.45f, 0.75f, 0.35f);
    private static final MutableColor ACTIVE_ROW_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 0.70f);

    private final ScrollBar verticalScrollBar = new ScrollBar().orientation(Orientation.VERTICAL);
    private final FixedRowVirtualizer virtualizer = new FixedRowVirtualizer();
    private final IndexSelectionModel selection = new IndexSelectionModel();
    private final Int2ObjectLinkedOpenHashMap<Widget> realized = new Int2ObjectLinkedOpenHashMap<>();
    private final Int2ObjectLinkedOpenHashMap<Widget> recycled = new Int2ObjectLinkedOpenHashMap<>();
    private int[] realizedIndexSnapshot = new int[0];
    private Widget[] realizedWidgetSnapshot = new Widget[0];
    private boolean realizedSnapshotDirty = true;
    private List<Widget> childrenView = Collections.emptyList();
    private boolean childrenViewDirty = true;
    private boolean childrenViewIncludesVerticalScrollBar;
    private IntFunction<? extends Widget> itemFactory = index -> new Label(String.valueOf(index));
    private RenderPhase renderPhase = RenderPhase.BACKGROUND;
    private int gridColumns = 1;
    private int activeIndex = -1;
    private int pendingActiveIndex = -1;

    public VirtualListView() {
        focusable(true);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.onValueChanged(event -> scrollTo(event.newValue()));
    }

    @Override
    protected String styleType() {
        return STYLE_TYPE;
    }

    @Override
    public VirtualListView layout(Consumer<LayoutStyle> update) {
        super.layout(update);
        return this;
    }

    @Override
    public VirtualListView style(String declarations) {
        super.style(declarations);
        syncVirtualizerConfiguration();
        trimRecycled();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public VirtualListView style(Consumer<MutableStyle> update) {
        super.style(update);
        syncVirtualizerConfiguration();
        trimRecycled();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public VirtualListView styleClear() {
        super.styleClear();
        syncVirtualizerConfiguration();
        trimRecycled();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ViewMode viewMode() {
        return effectiveViewMode() == VirtualListViewMode.GRID ? ViewMode.GRID : ViewMode.LIST;
    }

    @XmlAttribute(value = "viewMode", category = "Layout", defaultValue = "list", description = "Virtual item presentation mode: list or grid.")
    public VirtualListView viewMode(ViewMode viewMode) {
        ViewMode normalized = viewMode == null ? ViewMode.LIST : viewMode;
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_VIEW_MODE,
                normalized == ViewMode.GRID ? VirtualListViewMode.GRID : VirtualListViewMode.LIST));
        return this;
    }

    public VirtualListView grid(boolean grid) {
        return viewMode(grid ? ViewMode.GRID : ViewMode.LIST);
    }

    public float gridCellWidth() {
        return sanitizePositive(styleValue(StyleKeys.VIRTUAL_LIST_GRID_CELL_WIDTH, 96.0f), 96.0f);
    }

    @XmlAttribute(value = "gridCellWidth", category = "Layout", defaultValue = "96", description = "Preferred grid cell width used for responsive column auto-fit.")
    public VirtualListView gridCellWidth(float width) {
        float normalized = Float.isFinite(width) ? Math.max(1.0f, width) : 96.0f;
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_GRID_CELL_WIDTH, normalized));
        return this;
    }

    public float gridCellHeight() {
        return sanitizePositive(styleValue(StyleKeys.VIRTUAL_LIST_GRID_CELL_HEIGHT, 96.0f), 96.0f);
    }

    @XmlAttribute(value = "gridCellHeight", category = "Layout", defaultValue = "96", description = "Fixed height of a virtual grid row.")
    public VirtualListView gridCellHeight(float height) {
        float normalized = Float.isFinite(height) ? Math.max(1.0f, height) : 96.0f;
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_GRID_CELL_HEIGHT, normalized));
        return this;
    }

    /**
     * Sets a fixed column count; zero restores width-based auto-fit.
     */
    @XmlAttribute(value = "gridColumns", category = "Layout", defaultValue = "0", description = "Fixed grid column count; zero fits columns to available width.")
    public VirtualListView gridColumns(int columns) {
        int normalized = Math.max(0, columns);
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_GRID_COLUMNS, normalized));
        return this;
    }

    public int gridColumns() {
        return viewMode() == ViewMode.GRID ? gridColumns : 1;
    }

    /**
     * Returns the configured fixed count, or zero while responsive auto-fit is enabled.
     */
    public int configuredGridColumns() {
        return Math.max(0, styleValue(StyleKeys.VIRTUAL_LIST_GRID_COLUMNS, 0));
    }

    public int itemCount() {
        return itemCount;
    }

    @XmlAttribute(value = "itemCount", category = "Data", defaultValue = "0", description = "Number of virtual items exposed by the view.")
    public VirtualListView itemCount(int itemCount) {
        int normalizedCount = Math.max(0, itemCount);
        if (this.itemCount == normalizedCount) return this;
        List<Integer> oldSelection = selection.selectedIndices();
        this.itemCount = normalizedCount;
        syncVirtualizerConfiguration();
        if (pendingActiveIndex >= 0) {
            activeIndex = clampIndexOrNone(pendingActiveIndex);
            if (activeIndex >= 0) {
                pendingActiveIndex = -1;
            }
        } else {
            activeIndex = clampIndexOrNone(activeIndex);
        }
        pruneRealized();
        pruneRecycled();
        emitSelectionChangeIfChanged(oldSelection, selection.retainWithin(this.itemCount));
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    private int itemCount;

    private void syncVirtualizerConfiguration() {
        virtualizer.overscan(overscan());
        if (viewMode() == ViewMode.GRID) {
            int configuredColumns = configuredGridColumns();
            gridColumns = configuredColumns > 0 ? configuredColumns : calculateGridColumns();
            virtualizer.itemExtent(gridCellHeight() + effectiveRowGap());
            virtualizer.itemCount(itemCount == 0 ? 0 : (itemCount - 1) / gridColumns + 1);
        } else {
            gridColumns = 1;
            virtualizer.itemExtent(rowExtent());
            virtualizer.itemCount(itemCount);
        }
    }

    private VirtualListViewMode effectiveViewMode() {
        return styleValue(StyleKeys.VIRTUAL_LIST_VIEW_MODE, VirtualListViewMode.LIST);
    }

    private float effectiveRowGap() {
        return sanitizeNonNegative(styleValue(
                StyleKeys.VIRTUAL_LIST_ROW_GAP, layoutStyle().rowGap()), layoutStyle().rowGap());
    }

    private float effectiveColumnGap() {
        return sanitizeNonNegative(styleValue(
                StyleKeys.VIRTUAL_LIST_COLUMN_GAP, layoutStyle().columnGap()), layoutStyle().columnGap());
    }

    private EdgeInsets effectivePadding() {
        return styleValue(StyleKeys.VIRTUAL_LIST_PADDING, layoutStyle().padding());
    }

    private int rowCount() {
        return virtualizer.itemCount();
    }

    private float rowExtent() {
        return (viewMode() == ViewMode.GRID ? gridCellHeight() : itemHeight()) + effectiveRowGap();
    }

    private int calculateGridColumns() {
        EdgeInsets padding = effectivePadding();
        float width = Math.max(0.0f, layoutBounds().width() - padding.horizontal()
                - (virtualizer.maxScrollOffset() > 0.0f ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0.0f));
        float gap = effectiveColumnGap();
        return Math.max(1, (int) Math.floor((width + gap) / (gridCellWidth() + gap)));
    }

    public float itemHeight() {
        return sanitizePositive(styleValue(StyleKeys.VIRTUAL_LIST_ITEM_HEIGHT, 18.0f), 18.0f);
    }

    @XmlAttribute(value = "itemHeight", category = "Layout", defaultValue = "18", description = "Fixed row height used by virtualization.")
    public VirtualListView itemHeight(float itemHeight) {
        float normalized = Float.isFinite(itemHeight) ? Math.max(1.0f, itemHeight) : 18.0f;
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_ITEM_HEIGHT, normalized));
        return this;
    }

    public int overscan() {
        return Math.max(0, styleValue(StyleKeys.VIRTUAL_LIST_OVERSCAN, 1));
    }

    @XmlAttribute(value = "overscan", category = "Behavior", defaultValue = "1", description = "Extra rows realized above and below the viewport.")
    public VirtualListView overscan(int overscan) {
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_OVERSCAN, Math.max(0, overscan)));
        return this;
    }

    public VirtualListView itemFactory(IntFunction<? extends Widget> itemFactory) {
        this.itemFactory = itemFactory == null ? index -> new Label(String.valueOf(index)) : itemFactory;
        clearRealized();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int offscreenCacheSize() {
        return Math.max(0, styleValue(StyleKeys.VIRTUAL_LIST_OFFSCREEN_CACHE_SIZE, 0));
    }

    @XmlAttribute(value = "offscreenCacheSize", category = "Behavior", defaultValue = "0", description = "Maximum number of recycled offscreen row widgets retained.")
    public VirtualListView offscreenCacheSize(int offscreenCacheSize) {
        int normalized = Math.max(0, offscreenCacheSize);
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_OFFSCREEN_CACHE_SIZE, normalized));
        return this;
    }

    public float scrollY() {
        return virtualizer.scrollOffset();
    }

    @XmlAttribute(value = "scrollStep", category = "Behavior", defaultValue = "16", description = "UI pixels scrolled per wheel/page delta unit.")
    public VirtualListView scrollStep(float scrollStep) {
        float normalized = Float.isFinite(scrollStep) ? Math.max(1.0f, scrollStep) : 16.0f;
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_SCROLL_STEP, normalized));
        return this;
    }

    public float scrollStep() {
        return sanitizePositive(styleValue(StyleKeys.VIRTUAL_LIST_SCROLL_STEP, 16.0f), 16.0f);
    }

    public boolean consumeWheelAtScrollBounds() {
        return styleValue(StyleKeys.VIRTUAL_LIST_CONSUME_WHEEL_AT_BOUNDS, true);
    }

    @XmlAttribute(value = "consumeWheelAtScrollBounds", category = "Behavior", defaultValue = "true", description = "Whether wheel events are consumed at scroll range boundaries.")
    public VirtualListView consumeWheelAtScrollBounds(boolean consumeWheelAtScrollBounds) {
        style(value -> value.put(StyleKeys.VIRTUAL_LIST_CONSUME_WHEEL_AT_BOUNDS, consumeWheelAtScrollBounds));
        return this;
    }

    public float contentHeight() {
        syncVirtualizerConfiguration();
        int rows = rowCount();
        return Math.max(0.0f, rows * rowExtent() - (rows > 0 ? effectiveRowGap() : 0.0f));
    }

    public float maxScrollY() {
        EdgeInsets padding = effectivePadding();
        return Math.max(0.0f, contentHeight() - Math.max(0.0f, layoutBounds().height() - padding.vertical()));
    }

    public int firstVisibleIndex() {
        return realizedRange().firstIndex();
    }

    public int lastVisibleIndexExclusive() {
        return realizedRange().lastIndexExclusive();
    }

    public VirtualRange realizedRange() {
        syncVirtualizerConfiguration();
        VirtualRange rows = virtualizer.visibleRange();
        if (viewMode() != ViewMode.GRID) return rows;
        return new VirtualRange(Math.min(itemCount, rows.firstIndex() * gridColumns),
                Math.min(itemCount, rows.lastIndexExclusive() * gridColumns));
    }

    public int realizedCount() {
        return realizedRange().count();
    }

    public SelectionMode selectionMode() {
        return selection.mode();
    }

    @XmlAttribute(value = "selectionMode", category = "Behavior", defaultValue = "single", description = "Selection mode used by list rows.")
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

    @XmlAttribute(value = "activeIndex", category = "Behavior", defaultValue = "-1", description = "Initial keyboard/navigation active row index.")
    public VirtualListView activeIndex(int index) {
        if (index >= 0 && itemCount <= 0) {
            pendingActiveIndex = index;
        } else {
            pendingActiveIndex = -1;
        }
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

    /**
     * Возвращает фазу текущего render-прохода.
     *
     * <p>Поле обновляется самим виджетом перед каждым обращением к renderer'у и
     * позволяет единому {@code WidgetRender} рисовать фазо-зависимый контент
     * ({@code BACKGROUND} под дочерними виджетами, {@code FOREGROUND} над ними).</p>
     */
    public RenderPhase renderPhase() {
        return renderPhase;
    }

    public VirtualListView scrollTo(float y) {
        syncVirtualizerConfiguration();
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
        syncVirtualizerConfiguration();
        trimRecycled();
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.setUiContextInternal(uiContext);
        for (Widget item : realized.values()) {
            attachItem(item);
        }
    }

    @Override
    public List<Widget> children() {
        boolean includeScrollBar = hasVerticalScrollBar();
        if (childrenViewDirty || childrenViewIncludesVerticalScrollBar != includeScrollBar) {
            Widget[] items = realizedWidgetSnapshot();
            if (items.length == 0 && !includeScrollBar) {
                childrenView = Collections.emptyList();
            } else {
                List<Widget> children = new ObjectArrayList<>(items.length + (includeScrollBar ? 1 : 0));
                Collections.addAll(children, items);
                if (includeScrollBar) {
                    children.add(verticalScrollBar);
                }
                childrenView = Collections.unmodifiableList(children);
            }
            childrenViewIncludesVerticalScrollBar = includeScrollBar;
            childrenViewDirty = false;
        }
        return childrenView;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        syncVirtualizerConfiguration();
        float desiredWidth = 0.0f;
        for (Widget item : realizedWidgetSnapshot()) {
            item.measure(context);
            desiredWidth = Math.max(desiredWidth, StackPanel.preferredWidth(item, 0.0f));
        }
        EdgeInsets padding = effectivePadding();
        float desiredHeight = Math.min(contentHeight(), Math.max(0.0f, rowExtent() * 8.0f));
        setDesiredSize(resolveDesiredSize(context,
                desiredWidth + padding.horizontal(), desiredHeight + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        syncVirtualizerConfiguration();
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
        for (Widget item : realizedWidgetSnapshot()) {
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
            WidgetRender custom = customRender();
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            EdgeInsets padding = effectivePadding();
            draw.pushClip(layoutBounds().x() + padding.left(), layoutBounds().y() + padding.top(),
                    viewportWidth(), Math.max(0.0f, layoutBounds().height() - padding.vertical()));
            renderPhase = RenderPhase.BACKGROUND;
            if (custom != null) {
                custom.render(draw, this);
            } else {
                renderPhaseVisual(draw, RenderPhase.BACKGROUND);
            }
            context.pushTextPixelSnap(false);
            try {
                for (Widget item : realizedWidgetSnapshot()) {
                    renderChildWithInheritedTransform(context, item);
                }
            } finally {
                context.popTextPixelSnap();
            }
            renderPhase = RenderPhase.FOREGROUND;
            if (custom != null) {
                custom.render(draw, this);
            } else {
                renderPhaseVisual(draw, RenderPhase.FOREGROUND);
            }
            draw.popClip();
            if (hasVerticalScrollBar()) {
                renderChildWithInheritedTransform(context, verticalScrollBar);
            }
        } finally {
            popOpacity(context);
        }
    }

    private void renderPhaseVisual(DrawScope draw, RenderPhase phase) {
        ensureRealizedSnapshot();
        boolean focused = isFocused();
        for (int i = 0; i < realizedWidgetSnapshot.length; i++) {
            int index = realizedIndexSnapshot[i];
            Widget item = realizedWidgetSnapshot[i];
            if (item.visibility() != Visibility.VISIBLE) continue;
            boolean selected = selection.isSelected(index);
            boolean active = index == activeIndex;
            if (phase == RenderPhase.BACKGROUND && selected) {
                ColorView selectedColor = styleValue(
                        StyleKeys.ACCENT_COLOR, WidgetState.CHECKED, SELECTED_ROW_COLOR);
                draw.rect(item.layoutBounds().x(), item.layoutBounds().y(),
                        item.layoutBounds().width(), item.layoutBounds().height(),
                        Paint.fill(selectedColor));
            } else if (phase == RenderPhase.FOREGROUND && focused && active) {
                ColorView activeColor = styleValue(
                        StyleKeys.FOCUS_RING_COLOR, WidgetState.FOCUSED, ACTIVE_ROW_COLOR);
                draw.rect(item.layoutBounds().x(), item.layoutBounds().y(),
                        item.layoutBounds().width(), item.layoutBounds().height(),
                        Paint.stroke(activeColor, 1.0f));
            }
        }
    }

    public enum RenderPhase {
        BACKGROUND,
        FOREGROUND
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float before = virtualizer.scrollOffset();
            scrollBy(-scroll.deltaY() * scrollStep());
            if (before != virtualizer.scrollOffset()) {
                event.cancel();
            } else if (consumeWheelAtScrollBounds() && scroll.deltaY() != 0.0f && maxScrollY() > 0.0f) {
                event.cancel();
            }
        } else if (event instanceof KeyPressedEvent key && key.phase() != EventPhase.CAPTURE) {
            if (handleKey(key.keyCode(), key.modifiers())) {
                event.cancel();
            }
        } else if (event instanceof PointerPressedEvent pointer
                && pointer.phase() != EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY
                && localX(pointer) >= effectivePadding().left()
                && localX(pointer) < effectivePadding().left() + viewportWidth()
                && localY(pointer) >= effectivePadding().top()
                && localY(pointer) < layoutBounds().height() - effectivePadding().bottom()) {
            int index = indexAt(localX(pointer), localY(pointer));
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
        super.dispose();
    }

    private void updateRealizedWindow() {
        updateVirtualizerViewport();
        VirtualRange range = virtualizer.visibleRange();
        int first = viewMode() == ViewMode.GRID ? range.firstIndex() * gridColumns : range.firstIndex();
        int last = viewMode() == ViewMode.GRID
                ? Math.min(itemCount, range.lastIndexExclusive() * gridColumns)
                : range.lastIndexExclusive();
        boolean changed = false;

        ObjectIterator<Int2ObjectMap.Entry<Widget>> iterator = realized.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<Widget> entry = iterator.next();
            int index = entry.getIntKey();
            if (index < first || index >= last) {
                detachItem(entry.getValue());
                recycleItem(index, entry.getValue());
                iterator.remove();
                changed = true;
            }
        }

        for (int index = first; index < last; index++) {
            if (!realized.containsKey(index)) {
                realized.put(index, realizeItem(index));
                changed = true;
            }
        }

        if (changed) {
            markRealizedSnapshotDirty();
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
        if (offscreenCacheSize() <= 0 || index < 0 || index >= itemCount) {
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
        EdgeInsets padding = effectivePadding();
        float x = layoutBounds().x() + padding.left();
        float width = viewportWidth();
        ensureRealizedSnapshot();
        for (int i = 0; i < realizedWidgetSnapshot.length; i++) {
            int index = realizedIndexSnapshot[i];
            Widget item = realizedWidgetSnapshot[i];
            int row = viewMode() == ViewMode.GRID ? index / gridColumns : index;
            int column = viewMode() == ViewMode.GRID ? index % gridColumns : 0;
            float y = layoutBounds().y() + padding.top() + virtualizer.itemOffset(row);
            if (viewMode() == ViewMode.GRID) {
                float gap = effectiveColumnGap();
                float cellWidth = Math.max(0.0f, (width - Math.max(0, gridColumns - 1) * gap) / gridColumns);
                StackPanel.arrangeChild(item, x + column * (cellWidth + gap), y, cellWidth, gridCellHeight());
            } else {
                StackPanel.arrangeChild(item, x, y, width, itemHeight());
            }
        }
    }

    private void arrangeScrollBar() {
        if (!hasVerticalScrollBar()) return;
        EdgeInsets padding = effectivePadding();
        verticalScrollBar.arrange(new MutableRect(
                layoutBounds().x() + layoutBounds().width() - padding.right() - SCROLLBAR_WIDTH,
                layoutBounds().y() + padding.top(),
                SCROLLBAR_WIDTH,
                Math.max(0.0f, layoutBounds().height() - padding.vertical())));
        syncScrollBar();
    }

    private void syncScrollBar() {
        EdgeInsets padding = effectivePadding();
        verticalScrollBar
                .range(0.0f, maxScrollY())
                .pageSize(Math.max(1.0f, layoutBounds().height() - padding.vertical()))
                .step(scrollStep())
                .silentValue(virtualizer.scrollOffset());
    }

    private boolean hasVerticalScrollBar() {
        return maxScrollY() > 0.0f;
    }

    private float viewportWidth() {
        EdgeInsets padding = effectivePadding();
        return Math.max(0.0f, layoutBounds().width() - padding.horizontal()
                - (hasVerticalScrollBar() ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0.0f));
    }

    private void pruneRealized() {
        boolean changed = false;
        ObjectIterator<Int2ObjectMap.Entry<Widget>> iterator = realized.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<Widget> entry = iterator.next();
            if (entry.getIntKey() >= itemCount) {
                detachItem(entry.getValue());
                entry.getValue().dispose();
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            markRealizedSnapshotDirty();
        }
    }

    private void pruneRecycled() {
        ObjectIterator<Int2ObjectMap.Entry<Widget>> iterator = recycled.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<Widget> entry = iterator.next();
            if (entry.getIntKey() >= itemCount) {
                entry.getValue().dispose();
                iterator.remove();
            }
        }
        trimRecycled();
    }

    private void trimRecycled() {
        int cacheSize = offscreenCacheSize();
        if (cacheSize <= 0) {
            clearRecycled();
            return;
        }
        ObjectIterator<Int2ObjectMap.Entry<Widget>> iterator = recycled.int2ObjectEntrySet().iterator();
        while (recycled.size() > cacheSize && iterator.hasNext()) {
            Int2ObjectMap.Entry<Widget> entry = iterator.next();
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
        markRealizedSnapshotDirty();
        clearRecycled();
    }

    private void clearRecycled() {
        for (Widget item : recycled.values()) {
            item.dispose();
        }
        recycled.clear();
    }

    private Widget[] realizedWidgetSnapshot() {
        ensureRealizedSnapshot();
        return realizedWidgetSnapshot;
    }

    private void ensureRealizedSnapshot() {
        if (!realizedSnapshotDirty) return;
        int[] sortedIndices = realized.keySet().toIntArray();
        Arrays.sort(sortedIndices);
        int size = sortedIndices.length;
        if (realizedIndexSnapshot.length != size) {
            realizedIndexSnapshot = new int[size];
        }
        if (realizedWidgetSnapshot.length != size) {
            realizedWidgetSnapshot = new Widget[size];
        }
        int i = 0;
        for (int index : sortedIndices) {
            realizedIndexSnapshot[i] = index;
            realizedWidgetSnapshot[i] = realized.get(index);
            i++;
        }
        realizedSnapshotDirty = false;
    }

    private void markRealizedSnapshotDirty() {
        realizedSnapshotDirty = true;
        childrenViewDirty = true;
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
        virtualizer.itemExtent(rowExtent());
        virtualizer.viewportExtent(Math.max(0.0f, layoutBounds().height() - effectivePadding().vertical()));
    }

    private int indexAt(float localX, float localY) {
        EdgeInsets padding = effectivePadding();
        float contentY = localY - padding.top();
        if (contentY < 0.0f) return -1;
        int row = (int) Math.floor((contentY + virtualizer.scrollOffset()) / rowExtent());
        float rowPosition = (contentY + virtualizer.scrollOffset()) - row * rowExtent();
        float itemExtent = viewMode() == ViewMode.GRID ? gridCellHeight() : itemHeight();
        if (rowPosition < 0.0f || rowPosition >= itemExtent) return -1;
        if (viewMode() != ViewMode.GRID) return row >= 0 && row < itemCount ? row : -1;
        float contentX = localX - padding.left();
        float gap = effectiveColumnGap();
        float cellWidth = Math.max(1.0f, (viewportWidth() - Math.max(0, gridColumns - 1) * gap) / gridColumns);
        int column = (int) Math.floor(contentX / (cellWidth + gap));
        if (contentX < 0.0f || column < 0 || column >= gridColumns
                || contentX - column * (cellWidth + gap) >= cellWidth) return -1;
        int index = row * gridColumns + column;
        return index >= 0 && index < itemCount ? index : -1;
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
        if (itemCount <= 0) return false;
        int target = switch (keyCode) {
            case KeyCodes.UP -> navigationBaseIndex() - (viewMode() == ViewMode.GRID ? gridColumns : 1);
            case KeyCodes.DOWN -> navigationBaseIndex() + (viewMode() == ViewMode.GRID ? gridColumns : 1);
            case KeyCodes.LEFT -> navigationBaseIndex() - 1;
            case KeyCodes.RIGHT -> navigationBaseIndex() + 1;
            case KeyCodes.HOME -> 0;
            case KeyCodes.END -> itemCount - 1;
            case KeyCodes.PAGE_UP -> navigationBaseIndex() - pageItemCount();
            case KeyCodes.PAGE_DOWN -> navigationBaseIndex() + pageItemCount();
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
        if (activeIndex >= 0 && activeIndex < itemCount) return activeIndex;
        int selected = selection.selectedIndex();
        if (selected >= 0 && selected < itemCount) return selected;
        return Math.min(Math.max(0, firstVisibleIndex()), Math.max(0, itemCount - 1));
    }

    private int pageItemCount() {
        float extent = Math.max(1.0f, rowExtent());
        int rows = Math.max(1, (int) Math.floor(Math.max(extent,
                layoutBounds().height() - effectivePadding().vertical()) / extent));
        return rows * (viewMode() == ViewMode.GRID ? gridColumns : 1);
    }

    private void ensureActiveVisible() {
        if (activeIndex < 0) return;
        updateVirtualizerViewport();
        int row = viewMode() == ViewMode.GRID ? activeIndex / gridColumns : activeIndex;
        float itemTop = row * rowExtent();
        float itemBottom = itemTop + rowExtent() - effectiveRowGap();
        float viewportTop = virtualizer.scrollOffset();
        float viewportBottom = viewportTop + Math.max(0.0f,
                layoutBounds().height() - effectivePadding().vertical());
        if (itemTop < viewportTop) {
            scrollTo(itemTop);
        } else if (itemBottom > viewportBottom) {
            scrollTo(itemBottom - Math.max(0.0f, layoutBounds().height() - effectivePadding().vertical()));
        }
    }

    private int clampIndexOrNone(int index) {
        return index >= 0 && index < itemCount ? index : -1;
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

    private static float sanitizePositive(Float value, float fallback) {
        return value != null && Float.isFinite(value) ? Math.max(1.0f, value) : fallback;
    }

    private static float sanitizeNonNegative(Float value, float fallback) {
        return value != null && Float.isFinite(value) ? Math.max(0.0f, value) : fallback;
    }
}
