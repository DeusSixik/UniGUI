package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Generic searchable modal grid picker.
 *
 * <p>The widget owns the common UI shell: selected preview, open button, modal dialog,
 * search field, virtualized icon grid, tooltip, filtering, selection, and row recycling.
 * Subclasses only provide entry-specific text, selected-preview updates and icon drawing.</p>
 */
public abstract class SearchableGridPickerWidget<T> extends LinearBox {
    protected static final float SUMMARY_HEIGHT = 38.0f;
    protected static final float DEFAULT_TILE_SIZE = 34.0f;
    protected static final float DEFAULT_TILE_GAP = 4.0f;
    protected static final int DEFAULT_GRID_COLUMNS = 10;
    protected static final int DEFAULT_ROW_CACHE_SIZE = 192;
    protected static final float DEFAULT_DIALOG_WIDTH = 430.0f;
    protected static final float DEFAULT_DIALOG_HEIGHT = 420.0f;

    private static final MutableColor TILE_BACKGROUND = new MutableColor(0.055f, 0.064f, 0.085f, 0.88f);
    private static final MutableColor TILE_HOVER_BACKGROUND = new MutableColor(0.095f, 0.120f, 0.160f, 0.96f);
    private static final MutableColor TILE_BORDER = new MutableColor(0.20f, 0.28f, 0.36f, 0.72f);
    private static final MutableColor TILE_HOVER_BORDER = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);

    private final WidgetBase selectedPreview;
    private final Button openButton;
    private final SearchField searchField = new SearchField();
    private final VirtualListView resultGrid = new VirtualListView() {
        @Override
        public void arrange(RectView bounds) {
            syncGridColumnsForWidth(bounds.width());
            super.arrange(bounds);
        }
    };
    private final Label emptyLabel;
    private final Tooltip idTooltip = new Tooltip();
    private final TileTooltipAnchor tileTooltipAnchor = new TileTooltipAnchor();
    private final OverlayLayer dialogOverlay = new OverlayLayer();
    private final VBox dialogPanel = new VBox();
    private final WindowWidget dialog;
    private final List<T> entries = new ObjectArrayList<>();
    private final List<T> filteredEntries = new ObjectArrayList<>();
    private final Map<T, String> searchTextCache = new HashMap<>();
    private final String emptySelectionText;
    private final float tileSize;
    private final float iconSize;
    private final float tileGap;
    private final float rowHeight;
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private T selectedEntry;
    private int gridColumns = DEFAULT_GRID_COLUMNS;

    protected SearchableGridPickerWidget(String dialogTitle,
                                         WidgetBase selectedPreview,
                                         String emptyLabelText,
                                         String emptySelectionText,
                                         float openButtonWidth,
                                         float iconSize) {
        this(dialogTitle,
                selectedPreview,
                emptyLabelText,
                emptySelectionText,
                openButtonWidth,
                DEFAULT_TILE_SIZE,
                iconSize,
                DEFAULT_TILE_GAP,
                DEFAULT_DIALOG_WIDTH,
                DEFAULT_DIALOG_HEIGHT);
    }

    protected SearchableGridPickerWidget(String dialogTitle,
                                         WidgetBase selectedPreview,
                                         String emptyLabelText,
                                         String emptySelectionText,
                                         float openButtonWidth,
                                         float tileSize,
                                         float iconSize,
                                         float tileGap,
                                         float dialogWidth,
                                         float dialogHeight) {
        super(Orientation.HORIZONTAL);
        this.selectedPreview = Objects.requireNonNull(selectedPreview, "selectedPreview");
        this.emptySelectionText = emptySelectionText == null ? "" : emptySelectionText;
        this.tileSize = sanitizePositive(tileSize, DEFAULT_TILE_SIZE);
        this.iconSize = sanitizePositive(iconSize, Math.max(1.0f, this.tileSize - 6.0f));
        this.tileGap = Float.isFinite(tileGap) ? Math.max(0.0f, tileGap) : DEFAULT_TILE_GAP;
        this.rowHeight = this.tileSize + 4.0f;
        this.openButton = new Button(this.emptySelectionText);
        this.emptyLabel = new Label(emptyLabelText == null ? "No entries" : emptyLabelText);
        this.dialog = new WindowWidget(dialogTitle == null ? "Select" : dialogTitle, dialogOverlay);

        spacing(6.0f);
        layout(style -> style.align(Alignment.START, Alignment.CENTER));

        this.selectedPreview.layout(style -> style.size(SUMMARY_HEIGHT, SUMMARY_HEIGHT).flexGrow(0).flexShrink(0.0f));
        openButton.layout(style -> style.size(openButtonWidth, SUMMARY_HEIGHT).flexGrow(0).flexShrink(0.0f));
        openButton.onClick(event -> open());

        addChild(this.selectedPreview);
        addChild(openButton);
        buildDialog(sanitizePositive(dialogWidth, DEFAULT_DIALOG_WIDTH), sanitizePositive(dialogHeight, DEFAULT_DIALOG_HEIGHT));
    }

    public Button openButton() {
        return openButton;
    }

    public SearchField searchField() {
        return searchField;
    }

    public WidgetBase selectedPreview() {
        return selectedPreview;
    }

    public VirtualListView resultGrid() {
        return resultGrid;
    }

    public VirtualListView resultRows() {
        return resultGrid;
    }

    public WindowWidget dialog() {
        return dialog;
    }

    public OverlayLayer dialogOverlay() {
        return dialogOverlay;
    }

    public Tooltip idTooltip() {
        return idTooltip;
    }

    public boolean opened() {
        return dialog.opened();
    }

    public SearchableGridPickerWidget<T> open() {
        syncDialogAttachment();
        if (attachedOverlayLayer != null) {
            dialog.openModal();
        }
        return this;
    }

    public SearchableGridPickerWidget<T> close() {
        dialog.close();
        return this;
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public SearchableGridPickerWidget<T> overlayLayer(OverlayLayer overlayLayer) {
        if (explicitOverlayLayer == overlayLayer) return this;
        detachDialog();
        explicitOverlayLayer = overlayLayer;
        syncDialogAttachment();
        return this;
    }

    public OverlayLayer attachedOverlayLayer() {
        return attachedOverlayLayer;
    }

    public List<T> entries() {
        return List.copyOf(entries);
    }

    public int entryCount() {
        return entries.size();
    }

    public List<T> filteredEntries() {
        return List.copyOf(filteredEntries);
    }

    public int filteredEntryCount() {
        return filteredEntries.size();
    }

    public String query() {
        return searchField.text();
    }

    public SearchableGridPickerWidget<T> query(String query) {
        searchField.text(query);
        return this;
    }

    public SearchableGridPickerWidget<T> clearQuery() {
        return query("");
    }

    public Optional<T> selectedEntry() {
        return Optional.ofNullable(selectedEntry);
    }

    public int selectedIndex() {
        return indexOf(entries, selectedEntry);
    }

    public int selectedFilteredIndex() {
        return indexOf(filteredEntries, selectedEntry);
    }

    public int gridColumns() {
        return gridColumns;
    }

    public SearchableGridPickerWidget<T> entries(Collection<T> entries) {
        T previous = selectedEntry;
        this.entries.clear();
        searchTextCache.clear();
        if (entries != null) {
            for (T entry : entries) {
                if (entry != null) {
                    this.entries.add(entry);
                }
            }
        }
        selectedEntry = containsEntry(this.entries, previous) ? previous : null;
        updateSelectedSummary();
        applyFilter();
        return this;
    }

    public SearchableGridPickerWidget<T> selectIndex(int index) {
        T entry = index >= 0 && index < entries.size() ? entries.get(index) : null;
        selectEntry(entry, true, false);
        return this;
    }

    public SearchableGridPickerWidget<T> selectFilteredIndex(int filteredIndex) {
        T entry = filteredIndex >= 0 && filteredIndex < filteredEntries.size()
                ? filteredEntries.get(filteredIndex)
                : null;
        selectEntry(entry, true, false);
        return this;
    }

    public SearchableGridPickerWidget<T> clearSelection() {
        selectEntry(null, true, false);
        return this;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void setParentInternal(Widget parent) {
        super.setParentInternal(parent);
        syncDialogAttachment();
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        syncDialogAttachment();
    }

    protected T selectedEntryOrNull() {
        return selectedEntry;
    }

    protected List<T> mutableEntries() {
        return entries;
    }

    protected List<T> mutableFilteredEntries() {
        return filteredEntries;
    }

    protected void selectEntry(T entry, boolean emitChange, boolean closeDialog) {
        if (Objects.equals(selectedEntry, entry)) {
            if (closeDialog) close();
            return;
        }
        int oldIndex = selectedIndex();
        selectedEntry = entry;
        updateSelectedSummary();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SelectionChangedEvent(this, indexList(oldIndex), indexList(selectedIndex())));
        }
        if (closeDialog) {
            close();
        }
    }

    protected abstract String entryIdText(T entry);

    protected abstract String searchText(T entry);

    protected abstract void clearSelectedPreview();

    protected abstract void updateSelectedPreview(T entry);

    protected abstract void renderEntryIcon(RenderContext context,
                                            T entry,
                                            float x,
                                            float y,
                                            float size,
                                            float opacity,
                                            Runnable repaint);

    protected String selectedButtonText(T entry) {
        return entryIdText(entry);
    }

    protected RichText tooltipText(T entry) {
        return RichText.plain(entryIdText(entry));
    }

    private void buildDialog(float dialogWidth, float dialogHeight) {
        dialogOverlay.content(dialogPanel);
        dialogOverlay.addOverlay(idTooltip);
        dialogOverlay.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        dialog.padding(EdgeInsets.all(6.0f));
        dialog.position(24.0f, 24.0f);
        dialog.modal(true);
        dialog.closeOnOutsideClick(false);
        dialog.layout(style -> style.size(dialogWidth, dialogHeight).flexGrow(0).flexShrink(0.0f));

        dialogPanel.spacing(6.0f);
        dialogPanel.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        searchField.placeholder("Search...");
        searchField.layout(style -> style.widthPercent(100.0f).height(22.0f).flexShrink(0.0f));
        searchField.onTextChanged(event -> applyFilter());

        resultGrid.itemHeight(rowHeight)
                .overscan(2)
                .offscreenCacheSize(DEFAULT_ROW_CACHE_SIZE)
                .consumeWheelAtScrollBounds(false)
                .itemFactory(GridRow::new);
        resultGrid.layout(style -> style.widthPercent(100.0f).height(326.0f).flexGrow(1.0f).flexShrink(1.0f));

        emptyLabel.layout(style -> style.widthPercent(100.0f).height(20.0f).flexGrow(0).flexShrink(0.0f));
        emptyLabel.visible(false);

        dialogPanel.addChild(searchField);
        dialogPanel.addChild(resultGrid);
        dialogPanel.addChild(emptyLabel);
        dialogPanel.applyQueuedMutations();
        dialogOverlay.applyQueuedMutations();
        dialog.applyQueuedMutations();
    }

    private void applyFilter() {
        String query = normalizeQuery(searchField.text());
        filteredEntries.clear();
        for (T entry : entries) {
            if (matches(entry, query)) {
                filteredEntries.add(entry);
            }
        }
        syncGridRows();
        resultGrid.visible(!filteredEntries.isEmpty());
        emptyLabel.visible(filteredEntries.isEmpty());
        resultGrid.scrollTo(0.0f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void syncGridRows() {
        resultGrid.itemCount(rowCount(filteredEntries.size()));
        resultGrid.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void syncGridColumnsForWidth(float width) {
        int nextColumns = columnsForWidth(width);
        if (nextColumns == gridColumns) return;
        gridColumns = nextColumns;
        syncGridRows();
    }

    private int rowCount(int itemCount) {
        int normalizedColumns = Math.max(1, gridColumns);
        return Math.max(0, (itemCount + normalizedColumns - 1) / normalizedColumns);
    }

    private int columnsForWidth(float width) {
        if (!Float.isFinite(width) || width <= 0.0f) return DEFAULT_GRID_COLUMNS;
        return Math.max(1, (int) Math.floor((width + tileGap) / (tileSize + tileGap)));
    }

    private void updateSelectedSummary() {
        if (selectedEntry == null) {
            clearSelectedPreview();
            openButton.text(emptySelectionText);
        } else {
            updateSelectedPreview(selectedEntry);
            openButton.text(selectedButtonText(selectedEntry));
        }
    }

    private void syncDialogAttachment() {
        OverlayLayer target = overlayLayer();
        if (target == null) {
            detachDialog();
            return;
        }
        if (attachedOverlayLayer == target) return;
        detachDialog();
        attachedOverlayLayer = target;
        attachedOverlayLayer.addOverlay(dialog);
    }

    private void detachDialog() {
        if (attachedOverlayLayer != null) {
            dialog.close();
            attachedOverlayLayer.removeOverlay(dialog);
            attachedOverlayLayer = null;
        }
    }

    private OverlayLayer findTopmostOverlayLayer() {
        OverlayLayer result = null;
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                result = layer;
            }
            current = current.parent();
        }
        return result;
    }

    private boolean matches(T entry, String query) {
        if (query.isEmpty()) return true;
        return cachedSearchText(entry).contains(query);
    }

    private String cachedSearchText(T entry) {
        return searchTextCache.computeIfAbsent(entry, value -> {
            String text = searchText(value);
            return text == null ? "" : text.toLowerCase(Locale.ROOT);
        });
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> boolean containsEntry(List<T> entries, T candidate) {
        return indexOf(entries, candidate) >= 0;
    }

    private static <T> int indexOf(List<T> entries, T candidate) {
        if (candidate == null) return -1;
        for (int i = 0; i < entries.size(); i++) {
            if (Objects.equals(entries.get(i), candidate)) {
                return i;
            }
        }
        return -1;
    }

    private static List<Integer> indexList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }

    private static float sanitizePositive(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private final class TileTooltipAnchor extends WidgetBase {
        private boolean active;

        private void show(float x, float y, float width, float height) {
            active = true;
            mutableLayoutBounds().set(x, y, width, height);
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }

        private void hide() {
            if (!active) return;
            active = false;
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }

        @Override
        public boolean hovered() {
            return active;
        }
    }
    private final class GridRow extends WidgetBase {
        private final int rowIndex;
        private int hoverColumn = -1;

        private GridRow(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        @Override
        public void setParentInternal(Widget parent) {
            super.setParentInternal(parent);
            if (parent == null) {
                clearHover();
            }
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(LayoutSize.ZERO);
                return;
            }
            float width = Math.max(0.0f, gridColumns * tileSize + Math.max(0, gridColumns - 1) * tileGap);
            setDesiredSize(resolveDesiredSize(context, width, rowHeight));
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE) return;
            pushOpacity(context);
            try {
                renderTiles(context);
                renderIcons(context);
            } finally {
                popOpacity(context);
            }
        }

        @Override
        public void handle(Event event) {
            super.handle(event);
            if (event.isCancelled()) return;
            if (event instanceof PointerEnteredEvent pointer && pointer.phase() == EventPhase.TARGET) {
                updateHover(pointer);
            } else if (event instanceof PointerMovedEvent pointer && pointer.phase() == EventPhase.TARGET) {
                updateHover(pointer);
            } else if (event instanceof PointerExitedEvent pointer && pointer.phase() == EventPhase.TARGET) {
                clearHover();
            } else if (event instanceof PointerPressedEvent pointer
                    && pointer.phase() == EventPhase.TARGET
                    && pointer.button() == PointerButton.PRIMARY) {
                T entry = entryAt(columnAt(pointer));
                if (entry != null) {
                    selectEntry(entry, true, true);
                    event.cancel();
                }
            }
        }

        private void renderTiles(RenderContext context) {
            float rowX = layoutBounds().x();
            float tileY = layoutBounds().y() + (rowHeight - tileSize) * 0.5f;
            for (int column = 0; column < gridColumns; column++) {
                T entry = entryAt(column);
                if (entry == null) break;
                float tileX = rowX + column * (tileSize + tileGap);
                boolean hot = hovered() && hoverColumn == column;
                context.roundedRect(tileX, tileY, tileSize, tileSize, 3.0f,
                        Paint.fill(hot ? TILE_HOVER_BACKGROUND : TILE_BACKGROUND));
                context.roundedRect(tileX, tileY, tileSize, tileSize, 3.0f,
                        Paint.stroke(hot ? TILE_HOVER_BORDER : TILE_BORDER, 1.0f));
            }
        }

        private void renderIcons(RenderContext context) {
            float rowX = layoutBounds().x();
            float tileY = layoutBounds().y() + (rowHeight - tileSize) * 0.5f;
            float iconInset = (tileSize - iconSize) * 0.5f;
            float capturedOpacity = context.opacityMultiplier();
            for (int column = 0; column < gridColumns; column++) {
                T entry = entryAt(column);
                if (entry == null) break;
                float tileX = rowX + column * (tileSize + tileGap);
                renderEntryIcon(
                        context,
                        entry,
                        tileX + iconInset,
                        tileY + iconInset,
                        iconSize,
                        capturedOpacity,
                        this::repaint);
            }
        }

        private void repaint() {
            invalidate(InvalidationFlags.VISUAL);
        }

        private void updateHover(PointerEvent pointer) {
            int pointerColumn = columnAt(pointer);
            int nextColumn = entryAt(pointerColumn) == null ? -1 : pointerColumn;
            if (hoverColumn != nextColumn) {
                hoverColumn = nextColumn;
                invalidate(InvalidationFlags.VISUAL);
            }
            T entry = entryAt(hoverColumn);
            if (entry != null) {
                tileTooltipAnchor.show(tileX(hoverColumn), tileY(), tileSize, tileSize);
                idTooltip.anchor(tileTooltipAnchor).richText(tooltipText(entry));
                idTooltip.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            } else {
                tileTooltipAnchor.hide();
                idTooltip.anchor(null).richText(RichText.plain(""));
            }
        }

        private void clearHover() {
            if (hoverColumn != -1) {
                hoverColumn = -1;
                invalidate(InvalidationFlags.VISUAL);
            }
            tileTooltipAnchor.hide();
            idTooltip.anchor(null).richText(RichText.plain(""));
        }

        private float tileX(int column) {
            return layoutBounds().x() + column * (tileSize + tileGap);
        }

        private float tileY() {
            return layoutBounds().y() + (rowHeight - tileSize) * 0.5f;
        }

        private int columnAt(PointerEvent pointer) {
            float localX = pointer.rootX() - layoutBounds().x();
            float localY = pointer.rootY() - layoutBounds().y();
            if (localY < (rowHeight - tileSize) * 0.5f || localY >= (rowHeight + tileSize) * 0.5f) {
                return -1;
            }
            if (localX < 0.0f) return -1;
            int column = (int) Math.floor(localX / (tileSize + tileGap));
            float tileStart = column * (tileSize + tileGap);
            if (column < 0 || column >= gridColumns || localX < tileStart || localX >= tileStart + tileSize) {
                return -1;
            }
            return column;
        }

        private T entryAt(int column) {
            if (column < 0 || column >= gridColumns) return null;
            int index = rowIndex * gridColumns + column;
            return index >= 0 && index < filteredEntries.size() ? filteredEntries.get(index) : null;
        }
    }
}

