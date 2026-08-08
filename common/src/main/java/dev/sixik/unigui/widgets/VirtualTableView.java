package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.FocusGainedEvent;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.event.TableCellEditCancelledEvent;
import dev.sixik.unigui.api.event.TableCellEditCommittedEvent;
import dev.sixik.unigui.api.event.TableCellEditStartedEvent;
import dev.sixik.unigui.api.event.TableColumnMovedEvent;
import dev.sixik.unigui.api.event.TableColumnResizedEvent;
import dev.sixik.unigui.api.event.TableSortChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.selection.IndexSelectionModel;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.virtualization.FixedRowVirtualizer;
import dev.sixik.unigui.api.virtualization.VirtualRange;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Fixed-row-height virtualized table/data-grid prototype.
 */
public class VirtualTableView extends WidgetBase {
    private static final float SCROLLBAR_WIDTH = ScrollBar.DEFAULT_SIZE;
    private static final float SCROLLBAR_GAP = ScrollBar.DEFAULT_GAP;
    private static final float HEADER_RESIZE_HIT_SLOP = 4.0f;
    private static final float DEFAULT_MIN_COLUMN_WIDTH = 24.0f;
    private static final MutableColor HEADER_BACKGROUND = new MutableColor(0.10f, 0.10f, 0.10f, 1.0f);
    private static final MutableColor ROW_BACKGROUND = new MutableColor(0.04f, 0.04f, 0.04f, 0.35f);
    private static final MutableColor ALTERNATE_ROW_BACKGROUND = new MutableColor(0.08f, 0.08f, 0.08f, 0.35f);
    private static final MutableColor SELECTED_ROW_BACKGROUND = new MutableColor(0.18f, 0.45f, 0.75f, 0.42f);
    private static final MutableColor ACTIVE_CELL_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 0.72f);
    private static final MutableColor GRID_COLOR = new MutableColor(0.25f, 0.25f, 0.25f, 0.65f);
    private static final MutableColor TEXT_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);

    private final ScrollBar verticalScrollBar = new ScrollBar().orientation(Orientation.VERTICAL);
    private final FixedRowVirtualizer virtualizer = new FixedRowVirtualizer();
    private final IndexSelectionModel selection = new IndexSelectionModel();
    private final List<VirtualTableColumn> columns = new ArrayList<>();
    private BiFunction<Integer, Integer, String> cellTextProvider = (row, column) -> "";
    private BiFunction<Integer, Integer, RichText> cellRichTextProvider;
    private BiFunction<Integer, Integer, ? extends Comparable<?>> sortKeyProvider = (row, column) -> cellText(row, column);
    private final Map<Integer, Comparator<Integer>> columnComparators = new HashMap<>();
    private int sortColumnIndex = -1;
    private SortDirection sortDirection = SortDirection.NONE;
    private int[] sortedRows;
    private boolean sortDirty = true;
    private float headerHeight = 18.0f;
    private float scrollStep = 16.0f;
    private int activeRow = -1;
    private int activeColumn = -1;
    private boolean editable;
    private final TextField cellEditor = new TextField();
    private int editingRow = -1;
    private int editingColumn = -1;
    private String editingOriginalText = "";
    private boolean columnResizeEnabled = true;
    private float minColumnWidth = DEFAULT_MIN_COLUMN_WIDTH;
    private int resizingColumn = -1;
    private float resizeStartRootX;
    private float resizeStartWidth;

    public VirtualTableView() {
        focusable(true);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.onValueChanged(event -> scrollTo(event.newValue()));
        cellEditor.themeEnabled(false);
        cellEditor.background().set(0.025f, 0.030f, 0.040f, 0.98f);
        cellEditor.borderColor().set(0.25f, 0.78f, 1.0f, 1.0f);
        cellEditor.textColor().set(1.0f, 1.0f, 1.0f, 1.0f);
        cellEditor.caretColor().set(0.25f, 0.78f, 1.0f, 1.0f);
        cellEditor.visualOnlyTextChanges(true);
    }

    public int rowCount() {
        return virtualizer.itemCount();
    }

    public VirtualTableView rowCount(int rowCount) {
        if (virtualizer.itemCount() == Math.max(0, rowCount)) return this;
        List<Integer> oldSelection = selection.selectedIndices();
        virtualizer.itemCount(rowCount);
        activeRow = clampRowOrNone(activeRow);
        if (editingRow >= virtualizer.itemCount()) {
            cancelEdit();
        }
        sortDirty = true;
        emitSelectionChangeIfChanged(oldSelection, selection.retainWithin(virtualizer.itemCount()));
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView columns(List<VirtualTableColumn> columns) {
        this.columns.clear();
        if (columns != null) {
            for (VirtualTableColumn column : columns) {
                if (column != null) {
                    this.columns.add(column);
                }
            }
        }
        normalizeSortColumn();
        activeColumn = clampColumnOrNone(activeColumn);
        if (editingColumn >= this.columns.size()) {
            cancelEdit();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView addColumn(String header, float width) {
        columns.add(new VirtualTableColumn(header, width));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView addColumn(RichText header, float width) {
        columns.add(new VirtualTableColumn(header, width));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<VirtualTableColumn> columns() {
        return Collections.unmodifiableList(columns);
    }

    public float columnWidth(int columnIndex) {
        int normalized = normalizeColumnIndex(columnIndex);
        return normalized < 0 ? 0.0f : columns.get(normalized).width();
    }

    public VirtualTableView resizeColumn(int columnIndex, float width) {
        setColumnWidth(columnIndex, width, true);
        return this;
    }

    public boolean columnResizeEnabled() {
        return columnResizeEnabled;
    }

    public VirtualTableView columnResizeEnabled(boolean columnResizeEnabled) {
        if (this.columnResizeEnabled == columnResizeEnabled) return this;
        this.columnResizeEnabled = columnResizeEnabled;
        if (!columnResizeEnabled) {
            endColumnResize();
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float minColumnWidth() {
        return minColumnWidth;
    }

    public VirtualTableView minColumnWidth(float minColumnWidth) {
        float normalized = Float.isFinite(minColumnWidth) ? Math.max(1.0f, minColumnWidth) : DEFAULT_MIN_COLUMN_WIDTH;
        if (this.minColumnWidth == normalized) return this;
        this.minColumnWidth = normalized;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            setColumnWidth(columnIndex, columns.get(columnIndex).width(), false);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView moveColumn(int oldIndex, int newIndex) {
        int from = normalizeColumnIndex(oldIndex);
        int to = normalizeColumnIndex(newIndex);
        if (from < 0 || to < 0 || from == to) return this;

        VirtualTableColumn column = columns.remove(from);
        columns.add(to, column);
        activeColumn = remapColumnIndex(activeColumn, from, to);
        editingColumn = remapColumnIndex(editingColumn, from, to);
        sortColumnIndex = remapColumnIndex(sortColumnIndex, from, to);
        remapColumnComparators(from, to);
        arrangeCellEditor();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableColumnMovedEvent(this, from, to));
        return this;
    }

    public boolean resizingColumn() {
        return resizingColumn >= 0;
    }

    public int resizingColumnIndex() {
        return resizingColumn;
    }

    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        if (columnResizeEnabled && enabled() && visibility() == Visibility.VISIBLE
                && (resizingColumn >= 0 || isHeaderLocalY(localY) && resizeColumnAt(localX) >= 0)) {
            return MouseCursor.RESIZE_HORIZONTAL;
        }
        return super.mouseCursorAt(localX, localY);
    }

    public EventSubscription onColumnResized(EventListener<? super TableColumnResizedEvent> listener) {
        return on(TableColumnResizedEvent.TYPE, listener);
    }

    public EventSubscription onColumnMoved(EventListener<? super TableColumnMovedEvent> listener) {
        return on(TableColumnMovedEvent.TYPE, listener);
    }

    public VirtualTableView cellTextProvider(BiFunction<Integer, Integer, String> cellTextProvider) {
        this.cellTextProvider = cellTextProvider == null ? (row, column) -> "" : cellTextProvider;
        sortDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView cellRichTextProvider(BiFunction<Integer, Integer, RichText> cellRichTextProvider) {
        this.cellRichTextProvider = cellRichTextProvider;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean editable() {
        return editable;
    }

    public VirtualTableView editable(boolean editable) {
        if (this.editable == editable) return this;
        this.editable = editable;
        if (!editable) {
            cancelEdit();
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView sortKeyProvider(BiFunction<Integer, Integer, ? extends Comparable<?>> sortKeyProvider) {
        this.sortKeyProvider = sortKeyProvider == null ? (row, column) -> cellText(row, column) : sortKeyProvider;
        sortDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView columnComparator(int columnIndex, Comparator<Integer> comparator) {
        if (columnIndex < 0) return this;
        if (comparator == null) {
            columnComparators.remove(columnIndex);
        } else {
            columnComparators.put(columnIndex, comparator);
        }
        sortDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float rowHeight() {
        return virtualizer.itemExtent();
    }

    public VirtualTableView rowHeight(float rowHeight) {
        float previous = virtualizer.itemExtent();
        virtualizer.itemExtent(rowHeight);
        if (previous == virtualizer.itemExtent()) return this;
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float headerHeight() {
        return headerHeight;
    }

    public VirtualTableView headerHeight(float headerHeight) {
        float normalized = Float.isFinite(headerHeight) ? Math.max(0.0f, headerHeight) : 18.0f;
        if (this.headerHeight == normalized) return this;
        this.headerHeight = normalized;
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int overscan() {
        return virtualizer.overscan();
    }

    public VirtualTableView overscan(int overscan) {
        int previous = virtualizer.overscan();
        virtualizer.overscan(overscan);
        if (previous == virtualizer.overscan()) return this;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView scrollStep(float scrollStep) {
        this.scrollStep = Float.isFinite(scrollStep) ? Math.max(1.0f, scrollStep) : 16.0f;
        syncScrollBar();
        return this;
    }

    public float scrollY() {
        return virtualizer.scrollOffset();
    }

    public float contentWidth() {
        float width = 0.0f;
        for (VirtualTableColumn column : columns) {
            width += column.width();
        }
        return width;
    }

    public float contentHeight() {
        return virtualizer.contentExtent();
    }

    public float maxScrollY() {
        return Math.max(0.0f, contentHeight() - rowViewportHeight());
    }

    public int firstVisibleRow() {
        return realizedRange().firstIndex();
    }

    public int lastVisibleRowExclusive() {
        return realizedRange().lastIndexExclusive();
    }

    public VirtualRange realizedRange() {
        return virtualizer.visibleRange();
    }

    public int realizedRowCount() {
        return realizedRange().count();
    }

    public SelectionMode selectionMode() {
        return selection.mode();
    }

    public VirtualTableView selectionMode(SelectionMode mode) {
        List<Integer> oldSelection = selection.selectedIndices();
        selection.mode(mode);
        emitSelectionChangeIfChanged(oldSelection, !oldSelection.equals(selection.selectedIndices()));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int selectedRow() {
        return selection.selectedIndex();
    }

    public List<Integer> selectedRows() {
        return selection.selectedIndices();
    }

    public boolean isRowSelected(int row) {
        return selection.isSelected(row);
    }

    public int activeRow() {
        return activeRow;
    }

    public int activeColumn() {
        return activeColumn;
    }

    public VirtualTableView activeCell(int row, int column) {
        int normalizedRow = clampRowOrNone(row);
        int normalizedColumn = clampColumnOrNone(column);
        if (activeRow == normalizedRow && activeColumn == normalizedColumn) return this;
        activeRow = normalizedRow;
        activeColumn = normalizedColumn;
        ensureActiveVisible();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView selectRow(int row) {
        List<Integer> oldSelection = selection.selectedIndices();
        activeRow = clampRowOrNone(row);
        if (activeColumn < 0) {
            activeColumn = firstColumnOrNone();
        }
        emitSelectionChangeIfChanged(oldSelection, selection.select(row));
        ensureActiveVisible();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView toggleRow(int row) {
        List<Integer> oldSelection = selection.selectedIndices();
        emitSelectionChangeIfChanged(oldSelection, selection.toggle(row));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView clearSelection() {
        List<Integer> oldSelection = selection.selectedIndices();
        emitSelectionChangeIfChanged(oldSelection, selection.clear());
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    public boolean editing() {
        return editingRow >= 0 && editingColumn >= 0;
    }

    public int editingRow() {
        return editingRow;
    }

    public int editingColumn() {
        return editingColumn;
    }

    public String editingText() {
        return editing() ? cellEditor.text() : "";
    }

    public VirtualTableView editingText(String text) {
        if (editing()) {
            cellEditor.text(text);
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public VirtualTableView beginEdit() {
        return beginEdit(activeRow, activeColumn);
    }

    public VirtualTableView beginEdit(int row, int column) {
        if (!editable) return this;
        int normalizedRow = clampRowOrNone(row);
        int normalizedColumn = clampColumnOrNone(column);
        if (normalizedRow < 0 || normalizedColumn < 0) return this;
        if (editing() && editingRow == normalizedRow && editingColumn == normalizedColumn) return this;
        if (editing()) {
            commitEdit();
        }

        activeRow = normalizedRow;
        activeColumn = normalizedColumn;
        ensureActiveVisible();
        editingRow = normalizedRow;
        editingColumn = normalizedColumn;
        editingOriginalText = cellText(normalizedRow, normalizedColumn);
        cellEditor.setUiContextInternal(uiContext());
        cellEditor.text(editingOriginalText);
        cellEditor.selectAll();
        cellEditor.handle(new FocusGainedEvent(cellEditor, this));
        arrangeCellEditor();
        emit(new TableCellEditStartedEvent(this, editingRow, editingColumn, editingOriginalText));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView commitEdit() {
        if (!editing()) return this;
        int row = editingRow;
        int column = editingColumn;
        String oldText = editingOriginalText;
        String newText = cellEditor.text();
        endEditFocus();
        editingRow = -1;
        editingColumn = -1;
        editingOriginalText = "";
        sortDirty = true;
        emit(new TableCellEditCommittedEvent(this, row, column, oldText, newText));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView cancelEdit() {
        if (!editing()) return this;
        int row = editingRow;
        int column = editingColumn;
        String text = cellEditor.text();
        endEditFocus();
        editingRow = -1;
        editingColumn = -1;
        editingOriginalText = "";
        emit(new TableCellEditCancelledEvent(this, row, column, text));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onCellEditStarted(EventListener<? super TableCellEditStartedEvent> listener) {
        return on(TableCellEditStartedEvent.TYPE, listener);
    }

    public EventSubscription onCellEditCommitted(EventListener<? super TableCellEditCommittedEvent> listener) {
        return on(TableCellEditCommittedEvent.TYPE, listener);
    }

    public EventSubscription onCellEditCancelled(EventListener<? super TableCellEditCancelledEvent> listener) {
        return on(TableCellEditCancelledEvent.TYPE, listener);
    }

    public int sortColumnIndex() {
        return sortColumnIndex;
    }

    public SortDirection sortDirection() {
        return sortDirection;
    }

    public VirtualTableView sortBy(int columnIndex, SortDirection direction) {
        SortDirection normalizedDirection = direction == null ? SortDirection.NONE : direction;
        int normalizedColumn = normalizedDirection == SortDirection.NONE ? -1 : normalizeColumnIndex(columnIndex);
        if (normalizedColumn < 0) {
            normalizedDirection = SortDirection.NONE;
        }
        if (sortColumnIndex == normalizedColumn && sortDirection == normalizedDirection) return this;

        int oldColumn = sortColumnIndex;
        SortDirection oldDirection = sortDirection;
        sortColumnIndex = normalizedColumn;
        sortDirection = normalizedDirection;
        sortDirty = true;
        scrollTo(0.0f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableSortChangedEvent(this, oldColumn, oldDirection, sortColumnIndex, sortDirection));
        return this;
    }

    public VirtualTableView clearSort() {
        return sortBy(-1, SortDirection.NONE);
    }

    public VirtualTableView cycleSort(int columnIndex) {
        int normalizedColumn = normalizeColumnIndex(columnIndex);
        if (normalizedColumn < 0) return this;
        if (sortColumnIndex != normalizedColumn || sortDirection == SortDirection.NONE) {
            return sortBy(normalizedColumn, SortDirection.ASCENDING);
        }
        if (sortDirection == SortDirection.ASCENDING) {
            return sortBy(normalizedColumn, SortDirection.DESCENDING);
        }
        return clearSort();
    }

    public EventSubscription onSortChanged(EventListener<? super TableSortChangedEvent> listener) {
        return on(TableSortChangedEvent.TYPE, listener);
    }

    public ScrollBar verticalScrollBar() {
        return verticalScrollBar;
    }

    public VirtualTableView scrollTo(float y) {
        updateVirtualizerViewport();
        float before = virtualizer.scrollOffset();
        virtualizer.scrollOffset(y);
        if (before == virtualizer.scrollOffset()) return this;
        syncScrollBar();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView scrollBy(float dy) {
        return scrollTo(virtualizer.scrollOffset() + dy);
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.setUiContextInternal(uiContext);
        cellEditor.setUiContextInternal(uiContext);
    }

    @Override
    public List<Widget> children() {
        if (hasVerticalScrollBar()) {
            return List.of(verticalScrollBar);
        }
        return Collections.emptyList();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, contentWidth(), headerHeight + Math.min(contentHeight(), Math.max(0.0f, rowHeight() * 8.0f))));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        updateVirtualizerViewport();
        virtualizer.scrollOffset(virtualizer.scrollOffset());
        updateVisibleRows();
        arrangeCellEditor();
        arrangeScrollBar();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
        if (hasVerticalScrollBar()) {
            verticalScrollBar.tick(frame);
        }
        if (editing()) {
            cellEditor.tick(frame);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        pushOpacity(context);
        try {
            renderHeader(context);
            context.pushClip(layoutBounds().x(), rowViewportY(), viewportWidth(), rowViewportHeight());
            renderRows(context);
            if (editing()) {
                cellEditor.render(context);
            }
            context.popClip();
            if (hasVerticalScrollBar()) {
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
        if (handleColumnResizeEvent(event)) {
            event.cancel();
            return;
        }
        if (editing() && handleEditingEvent(event)) {
            event.cancel();
            return;
        }
        if (event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float before = virtualizer.scrollOffset();
            scrollBy(-scroll.deltaY() * scrollStep);
            if (before != virtualizer.scrollOffset()) {
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
                && localY(pointer) < Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()))) {
            int column = columnAt(localX(pointer));
            if (column >= 0) {
                requestFocus();
                activeColumn = column;
                if (editing()) {
                    commitEdit();
                }
                cycleSort(column);
                event.cancel();
            }
        } else if (event instanceof PointerPressedEvent pointer
                && pointer.phase() != EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY
                && localX(pointer) >= 0.0f
                && localX(pointer) < viewportWidth()
                && localY(pointer) >= Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()))
                && localY(pointer) < layoutBounds().height()) {
            int row = rowAt(localY(pointer));
            if (row >= 0) {
                requestFocus();
                if (editing()) {
                    commitEdit();
                }
                activeRow = sourceRowAt(row);
                activeColumn = columnAt(localX(pointer));
                if (activeColumn < 0) {
                    activeColumn = firstColumnOrNone();
                }
                selectRow(activeRow);
                event.cancel();
            }
        }
    }

    private void updateVisibleRows() {
        updateVirtualizerViewport();
    }

    private void renderHeader(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float height = Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()));
        context.rect(x, y, viewportWidth(), height, Paint.fill(HEADER_BACKGROUND), transform());
        float columnX = x;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            VirtualTableColumn column = columns.get(columnIndex);
            float width = Math.min(column.width(), Math.max(0.0f, viewportWidth() - (columnX - x)));
            if (width <= 0.0f) break;
            drawCellText(context, headerRichText(columnIndex), column, columnX, y, width, height, 3.0f, false);
            context.line(columnX + width, y, columnX + width, y + height,
                    Paint.stroke(columnIndex == resizingColumn ? ACTIVE_CELL_COLOR : GRID_COLOR, columnIndex == resizingColumn ? 2.0f : 1.0f),
                    transform());
            columnX += column.width();
        }
        context.line(x, y + height, x + viewportWidth(), y + height, Paint.stroke(GRID_COLOR, 1.0f), transform());
    }

    private void renderRows(RenderContext context) {
        float tableX = layoutBounds().x();
        ensureSortIndex();
        for (int visualRow = firstVisibleRow(); visualRow < lastVisibleRowExclusive(); visualRow++) {
            int row = sourceRowAt(visualRow);
            float y = rowViewportY() + virtualizer.itemOffset(visualRow);
            float rowHeight = rowHeight();
            MutableColor rowColor = selection.isSelected(row)
                    ? SELECTED_ROW_BACKGROUND
                    : ((visualRow & 1) == 0 ? ROW_BACKGROUND : ALTERNATE_ROW_BACKGROUND);
            context.rect(tableX, y, viewportWidth(), rowHeight, Paint.fill(rowColor), transform());
            float columnX = tableX;
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                VirtualTableColumn column = columns.get(columnIndex);
                float width = Math.min(column.width(), Math.max(0.0f, viewportWidth() - (columnX - tableX)));
                if (width <= 0.0f) break;
                if (!(editing() && row == editingRow && columnIndex == editingColumn)) {
                    RichText richText = cellRichText(row, columnIndex);
                    if (richText == null) {
                        drawCellText(context, cellText(row, columnIndex), column, columnX, y, width, rowHeight, 3.0f, true);
                    } else {
                        drawCellText(context, richText, column, columnX, y, width, rowHeight, 3.0f, true);
                    }
                }
                context.line(columnX + width, y, columnX + width, y + rowHeight, Paint.stroke(GRID_COLOR, 1.0f), transform());
                if (isFocused() && row == activeRow && columnIndex == activeColumn) {
                    context.rect(columnX, y, width, rowHeight, Paint.stroke(ACTIVE_CELL_COLOR, 1.0f), transform());
                }
                columnX += column.width();
            }
            context.line(tableX, y + rowHeight, tableX + viewportWidth(), y + rowHeight, Paint.stroke(GRID_COLOR, 1.0f), transform());
        }
    }

    private boolean handleEditingEvent(Event event) {
        if (event instanceof TextInputEvent input && input.phase() == EventPhase.TARGET) {
            cellEditor.handle(new TextInputEvent(cellEditor, input.codePoint(), input.modifiers()));
            invalidate(InvalidationFlags.VISUAL);
            return true;
        }
        if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET) {
            if (key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER) {
                commitEdit();
                return true;
            }
            if (key.keyCode() == KeyCodes.ESCAPE) {
                cancelEdit();
                return true;
            }
            cellEditor.handle(new KeyPressedEvent(cellEditor, key.keyCode(), key.scanCode(), key.modifiers()));
            invalidate(InvalidationFlags.VISUAL);
            return true;
        }
        return false;
    }

    private boolean handleColumnResizeEvent(Event event) {
        if (!columnResizeEnabled || visibility() != Visibility.VISIBLE || !enabled()) return false;
        if (event instanceof PointerMovedEvent pointer && pointer.phase() != EventPhase.CAPTURE && resizingColumn >= 0) {
            updateColumnResize(pointer.rootX());
            return true;
        }
        if (event instanceof PointerReleasedEvent pointer
                && pointer.phase() != EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY
                && resizingColumn >= 0) {
            updateColumnResize(pointer.rootX());
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            endColumnResize();
            return true;
        }
        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() != EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY
                && isHeaderLocalY(localY(pointer))) {
            int column = resizeColumnAt(localX(pointer));
            if (column >= 0) {
                startColumnResize(column, pointer.rootX(), pointer.pointerId());
                return true;
            }
        }
        return false;
    }

    private void arrangeCellEditor() {
        if (!editing()) return;
        MutableRect cellBounds = cellBounds(editingRow, editingColumn);
        if (cellBounds == null) {
            cellEditor.arrange(new MutableRect(0.0f, 0.0f, 0.0f, 0.0f));
            return;
        }
        cellEditor.arrange(cellBounds);
    }

    private MutableRect cellBounds(int sourceRow, int columnIndex) {
        int visualRow = visualRowOfSource(sourceRow);
        if (visualRow < 0 || columnIndex < 0 || columnIndex >= columns.size()) return null;
        float y = rowViewportY() + virtualizer.itemOffset(visualRow);
        float height = rowHeight();
        if (y + height <= rowViewportY() || y >= rowViewportY() + rowViewportHeight()) return null;
        float x = layoutBounds().x();
        for (int current = 0; current < columnIndex; current++) {
            x += columns.get(current).width();
        }
        float width = Math.min(columns.get(columnIndex).width(), Math.max(0.0f, viewportWidth() - (x - layoutBounds().x())));
        if (width <= 0.0f) return null;
        return new MutableRect(x, y, width, height);
    }

    private void endEditFocus() {
        cellEditor.handle(new FocusLostEvent(cellEditor, this));
        cellEditor.clearSelection();
    }

    private String cellText(int row, int column) {
        String text = cellTextProvider.apply(row, column);
        return text == null ? "" : text;
    }

    private RichText cellRichText(int row, int column) {
        if (cellRichTextProvider == null) return null;
        RichText text = cellRichTextProvider.apply(row, column);
        return text == null || text.isEmpty() ? null : text;
    }

    private void arrangeScrollBar() {
        if (!hasVerticalScrollBar()) return;
        verticalScrollBar.arrange(new MutableRect(
                layoutBounds().x() + layoutBounds().width() - SCROLLBAR_WIDTH,
                rowViewportY(),
                SCROLLBAR_WIDTH,
                rowViewportHeight()));
        syncScrollBar();
    }

    private void syncScrollBar() {
        verticalScrollBar
                .range(0.0f, maxScrollY())
                .pageSize(Math.max(1.0f, rowViewportHeight()))
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

    private float rowViewportY() {
        return layoutBounds().y() + Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()));
    }

    private float rowViewportHeight() {
        return Math.max(0.0f, layoutBounds().height() - headerHeight);
    }

    private void updateVirtualizerViewport() {
        virtualizer.viewportExtent(rowViewportHeight());
    }

    private int rowAt(float localY) {
        float localRowY = localY - Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()));
        int row = (int) Math.floor((localRowY + virtualizer.scrollOffset()) / virtualizer.itemExtent());
        return row >= 0 && row < virtualizer.itemCount() ? row : -1;
    }

    private int columnAt(float localX) {
        float x = 0.0f;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            float width = columns.get(columnIndex).width();
            if (localX >= x && localX < x + width) {
                return columnIndex;
            }
            x += width;
        }
        return -1;
    }

    private int resizeColumnAt(float localX) {
        if (localX < 0.0f || localX >= viewportWidth()) return -1;
        float x = 0.0f;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            x += columns.get(columnIndex).width();
            if (Math.abs(localX - x) <= HEADER_RESIZE_HIT_SLOP) {
                return columnIndex;
            }
        }
        return -1;
    }

    private boolean isHeaderLocalY(float localY) {
        return localY >= 0.0f && localY < Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()));
    }

    private void startColumnResize(int columnIndex, float rootX, int pointerId) {
        resizingColumn = normalizeColumnIndex(columnIndex);
        if (resizingColumn < 0) return;
        resizeStartRootX = rootX;
        resizeStartWidth = columns.get(resizingColumn).width();
        if (editing()) {
            commitEdit();
        }
        UIContext context = uiContext();
        if (context != null) {
            context.focusManager().requestFocus(this);
            context.capturePointer(pointerId, this);
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    private void updateColumnResize(float rootX) {
        if (resizingColumn < 0) return;
        setColumnWidth(resizingColumn, resizeStartWidth + rootX - resizeStartRootX, true);
    }

    private void endColumnResize() {
        if (resizingColumn < 0) return;
        resizingColumn = -1;
        resizeStartRootX = 0.0f;
        resizeStartWidth = 0.0f;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setColumnWidth(int columnIndex, float width, boolean emitChange) {
        int normalizedColumn = normalizeColumnIndex(columnIndex);
        if (normalizedColumn < 0) return;
        VirtualTableColumn column = columns.get(normalizedColumn);
        float normalizedWidth = Float.isFinite(width) ? Math.max(minColumnWidth, width) : Math.max(minColumnWidth, column.width());
        if (column.width() == normalizedWidth) return;
        columns.set(normalizedColumn, column.width(normalizedWidth));
        arrangeCellEditor();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new TableColumnResizedEvent(this, normalizedColumn, column.width(), normalizedWidth));
        }
    }

    private void remapColumnComparators(int oldIndex, int newIndex) {
        if (columnComparators.isEmpty()) return;
        Map<Integer, Comparator<Integer>> remapped = new HashMap<>();
        for (Map.Entry<Integer, Comparator<Integer>> entry : columnComparators.entrySet()) {
            int key = remapColumnIndex(entry.getKey(), oldIndex, newIndex);
            if (key >= 0) {
                remapped.put(key, entry.getValue());
            }
        }
        columnComparators.clear();
        columnComparators.putAll(remapped);
    }

    private int remapColumnIndex(int columnIndex, int oldIndex, int newIndex) {
        if (columnIndex < 0) return -1;
        if (columnIndex == oldIndex) return newIndex;
        if (oldIndex < newIndex && columnIndex > oldIndex && columnIndex <= newIndex) return columnIndex - 1;
        if (oldIndex > newIndex && columnIndex >= newIndex && columnIndex < oldIndex) return columnIndex + 1;
        return columnIndex;
    }

    private void drawCellText(RenderContext context,
                              String text,
                              VirtualTableColumn column,
                              float x,
                              float y,
                              float width,
                              float height,
                              float padding,
                              boolean allowColumnAlignment) {
        if (text == null || text.isEmpty()) return;
        drawCellText(context, RichText.plain(text), column, x, y, width, height, padding, allowColumnAlignment);
    }

    private void drawCellText(RenderContext context,
                              RichText text,
                              VirtualTableColumn column,
                              float x,
                              float y,
                              float width,
                              float height,
                              float padding,
                              boolean allowColumnAlignment) {
        if (text == null || text.isEmpty()) return;
        drawCellText(context, (Object) text, column, x, y, width, height, padding, allowColumnAlignment);
    }

    private void drawCellText(RenderContext context,
                              Object text,
                              VirtualTableColumn column,
                              float x,
                              float y,
                              float width,
                              float height,
                              float padding,
                              boolean allowColumnAlignment) {
        if (context == null || text == null || width <= 0.0f || height <= 0.0f) return;
        VirtualTableColumn safeColumn = column == null ? new VirtualTableColumn("", width) : column;
        float innerX = x + padding;
        float innerWidth = Math.max(0.0f, width - padding * 2.0f);
        if (innerWidth <= 0.0f) return;

        TextOverflowMode overflow = safeColumn.overflowMode();
        boolean clipped = overflow != TextOverflowMode.VISIBLE;
        if (clipped) {
            context.pushClip(x, y, width, height);
        }
        try {
            Alignment horizontal = allowColumnAlignment ? safeColumn.horizontalAlignment() : Alignment.START;
            Alignment vertical = safeColumn.verticalAlignment();
            RichText richText = text instanceof RichText value ? value : RichText.plain(text.toString());
            drawRichText(context, richText, innerX, y, innerWidth, height, horizontal, vertical, overflow);
        } finally {
            if (clipped) {
                context.popClip();
            }
        }
    }

    private void drawRichText(RenderContext context,
                              RichText text,
                              float x,
                              float y,
                              float width,
                              float height,
                              Alignment horizontal,
                              Alignment vertical,
                              TextOverflowMode overflow) {
        if (text == null || text.isEmpty()) return;
        if (overflow == TextOverflowMode.SHRINK_TO_FIT) {
            drawShrinkToFit(context, text, x, y, width, height, horizontal, vertical);
            return;
        }
        TextEngine.draw(context, text, x, y, width, height,
                Paint.fill(TEXT_COLOR), transform(), horizontal, vertical);
    }

    private void drawShrinkToFit(RenderContext context,
                                 RichText richText,
                                 float x,
                                 float y,
                                 float width,
                                 float height,
                                 Alignment horizontal,
                                 Alignment vertical) {
        float textWidth = TextEngine.measureLineWidth(context, richText);
        if (textWidth <= 0.0f) return;
        float sourceHeight = TextEngine.measureTextHeight(richText);
        float scale = width <= 0.0f ? 1.0f : Math.min(1.0f, width / textWidth);
        float scaledWidth = textWidth * scale;
        float scaledHeight = sourceHeight * scale;
        float drawX = TextEngine.alignedStart(x, width, scaledWidth, horizontal);
        float drawY = TextEngine.alignedStart(y, height, scaledHeight, vertical);
        var scaled = transform().copy();
        scaled.scale().set(transform().scale().x() * scale, transform().scale().y() * scale);
        context.text(richText, drawX, drawY, textWidth, sourceHeight, Paint.fill(TEXT_COLOR), scaled);
    }

    private float localX(PointerEvent pointer) {
        return pointer.rootX() - layoutBounds().x();
    }

    private float localY(PointerEvent pointer) {
        return pointer.rootY() - layoutBounds().y();
    }

    private String headerText(int columnIndex) {
        String header = columns.get(columnIndex).header();
        if (columnIndex != sortColumnIndex) return header;
        return switch (sortDirection) {
            case ASCENDING -> header + " ↑";
            case DESCENDING -> header + " ↓";
            case NONE -> header;
        };
    }

    private RichText headerRichText(int columnIndex) {
        RichText header = columns.get(columnIndex).richHeader();
        if (columnIndex != sortColumnIndex) return header;
        return switch (sortDirection) {
            case ASCENDING -> header.append(RichText.plain(" ↑"));
            case DESCENDING -> header.append(RichText.plain(" ↓"));
            case NONE -> header;
        };
    }

    private int sourceRowAt(int visualRow) {
        ensureSortIndex();
        if (visualRow < 0 || visualRow >= virtualizer.itemCount()) return visualRow;
        return sortedRows == null ? visualRow : sortedRows[visualRow];
    }

    private int visualRowOfSource(int sourceRow) {
        if (sourceRow < 0 || sourceRow >= virtualizer.itemCount()) return -1;
        ensureSortIndex();
        if (sortedRows == null) return sourceRow;
        for (int visualRow = 0; visualRow < sortedRows.length; visualRow++) {
            if (sortedRows[visualRow] == sourceRow) return visualRow;
        }
        return -1;
    }

    private void ensureSortIndex() {
        if (!sortDirty) return;
        sortDirty = false;
        if (sortColumnIndex < 0 || sortDirection == SortDirection.NONE || virtualizer.itemCount() == 0) {
            sortedRows = null;
            return;
        }

        List<Integer> rows = new ArrayList<>(virtualizer.itemCount());
        for (int row = 0; row < virtualizer.itemCount(); row++) {
            rows.add(row);
        }

        Comparator<Integer> comparator = rowComparator();
        if (sortDirection == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }
        rows.sort(comparator.thenComparingInt(Integer::intValue));

        sortedRows = new int[rows.size()];
        for (int index = 0; index < rows.size(); index++) {
            sortedRows[index] = rows.get(index);
        }
    }

    private Comparator<Integer> rowComparator() {
        Comparator<Integer> custom = columnComparators.get(sortColumnIndex);
        if (custom != null) return custom;
        return (left, right) -> compareSortKeys(sortKey(left, sortColumnIndex), sortKey(right, sortColumnIndex));
    }

    private Comparable<?> sortKey(int row, int column) {
        Comparable<?> key = sortKeyProvider.apply(row, column);
        return key == null ? "" : key;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareSortKeys(Comparable left, Comparable right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        return left.compareTo(right);
    }

    private void normalizeSortColumn() {
        if (sortColumnIndex >= columns.size()) {
            sortColumnIndex = -1;
            sortDirection = SortDirection.NONE;
        }
        sortDirty = true;
    }

    private int normalizeColumnIndex(int columnIndex) {
        return columnIndex >= 0 && columnIndex < columns.size() ? columnIndex : -1;
    }

    private boolean handleKey(int keyCode, int modifiers) {
        if (virtualizer.itemCount() <= 0) return false;
        return switch (keyCode) {
            case KeyCodes.UP -> {
                moveActiveRow(navigationBaseVisualRow() - 1, modifiers);
                yield true;
            }
            case KeyCodes.DOWN -> {
                moveActiveRow(navigationBaseVisualRow() + 1, modifiers);
                yield true;
            }
            case KeyCodes.PAGE_UP -> {
                moveActiveRow(navigationBaseVisualRow() - pageRowCount(), modifiers);
                yield true;
            }
            case KeyCodes.PAGE_DOWN -> {
                moveActiveRow(navigationBaseVisualRow() + pageRowCount(), modifiers);
                yield true;
            }
            case KeyCodes.HOME -> {
                moveActiveRow(0, modifiers);
                yield true;
            }
            case KeyCodes.END -> {
                moveActiveRow(virtualizer.itemCount() - 1, modifiers);
                yield true;
            }
            case KeyCodes.LEFT -> {
                moveActiveColumn(activeColumn < 0 ? firstColumnOrNone() : activeColumn - 1);
                yield true;
            }
            case KeyCodes.RIGHT -> {
                moveActiveColumn(activeColumn < 0 ? firstColumnOrNone() : activeColumn + 1);
                yield true;
            }
            case KeyCodes.F2 -> {
                beginEdit();
                yield editable;
            }
            case KeyCodes.SPACE, KeyCodes.ENTER, KeyCodes.KEYPAD_ENTER -> {
                if (editable && (keyCode == KeyCodes.ENTER || keyCode == KeyCodes.KEYPAD_ENTER) && !KeyModifiers.has(modifiers, KeyModifiers.SHIFT) && !KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
                    if (activeRow < 0) {
                        activeRow = sourceRowAt(navigationBaseVisualRow());
                    }
                    if (activeColumn < 0) {
                        activeColumn = firstColumnOrNone();
                    }
                    beginEdit();
                } else {
                    selectActiveRow(KeyModifiers.has(modifiers, KeyModifiers.CONTROL), KeyModifiers.has(modifiers, KeyModifiers.SHIFT));
                }
                yield true;
            }
            default -> false;
        };
    }

    private void moveActiveRow(int visualRow, int modifiers) {
        int targetVisualRow = Math.max(0, Math.min(virtualizer.itemCount() - 1, visualRow));
        int targetSourceRow = sourceRowAt(targetVisualRow);
        activeRow = targetSourceRow;
        if (activeColumn < 0) {
            activeColumn = firstColumnOrNone();
        }
        ensureActiveVisible();
        if (!KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
            List<Integer> oldSelection = selection.selectedIndices();
            boolean changed = KeyModifiers.has(modifiers, KeyModifiers.SHIFT)
                    ? selection.selectRange(targetSourceRow)
                    : selection.select(targetSourceRow);
            emitSelectionChangeIfChanged(oldSelection, changed);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void moveActiveColumn(int column) {
        int normalized = clampColumnOrNone(column);
        if (normalized < 0) return;
        if (activeRow < 0) {
            activeRow = sourceRowAt(navigationBaseVisualRow());
        }
        activeColumn = normalized;
        ensureActiveVisible();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void selectActiveRow(boolean toggle, boolean range) {
        if (activeRow < 0) {
            activeRow = sourceRowAt(navigationBaseVisualRow());
        }
        if (activeColumn < 0) {
            activeColumn = firstColumnOrNone();
        }
        if (activeRow < 0) return;
        List<Integer> oldSelection = selection.selectedIndices();
        boolean changed = range ? selection.selectRange(activeRow) : (toggle ? selection.toggle(activeRow) : selection.select(activeRow));
        emitSelectionChangeIfChanged(oldSelection, changed);
        ensureActiveVisible();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private int navigationBaseVisualRow() {
        int activeVisual = visualRowOfSource(activeRow);
        if (activeVisual >= 0) return activeVisual;
        int selectedVisual = visualRowOfSource(selection.selectedIndex());
        if (selectedVisual >= 0) return selectedVisual;
        return Math.min(Math.max(0, firstVisibleRow()), Math.max(0, virtualizer.itemCount() - 1));
    }

    private int pageRowCount() {
        float extent = Math.max(1.0f, virtualizer.itemExtent());
        return Math.max(1, (int) Math.floor(Math.max(extent, rowViewportHeight()) / extent));
    }

    private void ensureActiveVisible() {
        int visualRow = visualRowOfSource(activeRow);
        if (visualRow < 0) return;
        updateVirtualizerViewport();
        float rowTop = visualRow * virtualizer.itemExtent();
        float rowBottom = rowTop + virtualizer.itemExtent();
        float viewportTop = virtualizer.scrollOffset();
        float viewportBottom = viewportTop + rowViewportHeight();
        if (rowTop < viewportTop) {
            scrollTo(rowTop);
        } else if (rowBottom > viewportBottom) {
            scrollTo(rowBottom - rowViewportHeight());
        }
    }

    private int clampRowOrNone(int row) {
        return row >= 0 && row < virtualizer.itemCount() ? row : -1;
    }

    private int clampColumnOrNone(int column) {
        return column >= 0 && column < columns.size() ? column : -1;
    }

    private int firstColumnOrNone() {
        return columns.isEmpty() ? -1 : 0;
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
