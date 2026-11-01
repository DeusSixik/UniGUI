package dev.sixik.unigui.widgets.data;

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
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
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
import dev.sixik.unigui.api.event.TableColumnsChangedEvent;
import dev.sixik.unigui.api.event.TableRowsChangedEvent;
import dev.sixik.unigui.api.event.TableSortChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.selection.IndexSelectionModel;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.TableGridLines;
import dev.sixik.unigui.api.style.TableSortIndicatorStyle;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.virtualization.FixedRowVirtualizer;
import dev.sixik.unigui.api.virtualization.VirtualRange;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Fixed-row-height virtualized table/data-grid widget.
 *
 * <p>Поддерживает виртуализацию строк, изменение ширины и порядка колонок,
 * сортировку по клику на заголовок, встроенное редактирование ячеек,
 * горизонтальную прокрутку, добавление и удаление строк и колонок,
 * а также визуальные стили через {@link StyleKeys}.</p>
 */
@XmlWidgetName("VirtualTableView")
@AiDoc(
        summary = "High-performance virtualized grid/table view widget supporting column resizing, reordering, sorting, cell editing and dynamic row/column insertion.",
        purpose = "Displays high-density tabular data with virtualized row recycling, customizable columns, and cell interactions.",
        useWhen = {
                "Presenting multi-column data sheets, inventory manifests, or log tables.",
                "Interactive tables requiring column sorting, moving, resizing or dynamic row/column management."
        },
        criticalRules = {
                "Only rows within visible scroll viewport are measured and rendered.",
                "Columns list determines horizontal cell layout and header order.",
                "Cell providers receive the column modelIndex for data access, which stays stable across reorders."
        },
        related = {"VirtualTableColumn", "VirtualListView", "WidgetBase"})
@WikiPage(
        title = "<key:virtual.table.view.title>",
        slug = "virtual-table-view",
        category = "<key:core.widgets.data.category>",
        summary = "<key:virtual.table.view.summary>",
        order = 30)
@WikiSection(
        title = "<key:virtual.table.view.section.overview.title>",
        markdown = "<key:virtual.table.view.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:virtual.table.view.example.basic.title>",
        code = "<key:virtual.table.view.example.basic.code>",
        order = 10)
public class VirtualTableView extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.VIRTUAL_TABLE_VIEW;

    // ── scrollbar geometry ────────────────────────────────────────────────
    private static final float SCROLLBAR_WIDTH = ScrollBar.DEFAULT_SIZE;
    private static final float SCROLLBAR_GAP   = ScrollBar.DEFAULT_GAP;

    // ── interaction constants ─────────────────────────────────────────────
    private static final float HEADER_RESIZE_HIT_SLOP    = 4.0f;
    private static final float DEFAULT_MIN_COLUMN_WIDTH  = 24.0f;
    private static final float COLUMN_DRAG_THRESHOLD     = 6.0f;
    /** Ширина треугольного индикатора сортировки в UI-пикселях. */
    private static final float SORT_INDICATOR_SLOT       = 14.0f;
    /** Наполовину-высота шеврона (полная высота = 2 × CHEVRON_RISE). */
    private static final float CHEVRON_RISE  = 2.5f;
    private static final float CHEVRON_HALF  = 4.0f;
    private static final float CHEVRON_STROKE = 1.2f;
    private static final long  DOUBLE_CLICK_NS = 400_000_000L; // 400 ms

    // ── child widgets ─────────────────────────────────────────────────────
    private final ScrollBar verticalScrollBar   = new ScrollBar().orientation(Orientation.VERTICAL);
    private final ScrollBar horizontalScrollBar = new ScrollBar().orientation(Orientation.HORIZONTAL);

    // ── virtualization / data ─────────────────────────────────────────────
    private final FixedRowVirtualizer       virtualizer = new FixedRowVirtualizer();
    private final IndexSelectionModel       selection   = new IndexSelectionModel();
    private final ObjectArrayList<VirtualTableColumn> columns   = new ObjectArrayList<>();
    private final List<VirtualTableColumn>  columnsView = Collections.unmodifiableList(columns);
    private int nextModelIndex = 0;

    // ── providers / comparators ───────────────────────────────────────────
    private BiFunction<Integer, Integer, String>              cellTextProvider    = (row, col) -> "";
    private BiFunction<Integer, Integer, RichText>            cellRichTextProvider;
    private BiFunction<Integer, Integer, ? extends Comparable<?>> sortKeyProvider = (row, col) -> cellText(row, col);
    private final Int2ObjectOpenHashMap<Comparator<Integer>>  columnComparators  = new Int2ObjectOpenHashMap<>();

    // ── sort state ────────────────────────────────────────────────────────
    private int           sortColumnIndex = -1;
    private SortDirection sortDirection   = SortDirection.NONE;
    private int[]         sortedRows;
    private boolean       sortDirty = true;

    // ── layout / display ─────────────────────────────────────────────────
    private float headerHeight = 18.0f;
    private float scrollStep   = 16.0f;
    private float scrollX      = 0.0f;
    private boolean consumeWheelAtScrollBounds = true;
    private boolean sortingEnabled       = true;
    private boolean columnReorderEnabled = true;
    private RenderPhase renderPhase = RenderPhase.HEADER;

    // ── hover / selection state ───────────────────────────────────────────
    private int activeRow    = -1;
    private int activeColumn = -1;
    private int hoverRow     = -1;     // visual (display) row under pointer, -1 = none
    private int hoverHeaderCol = -1;   // column header under pointer, -1 = none
    private float lastPointerRootX = Float.NaN;
    private float lastPointerRootY = Float.NaN;

    // ── inline editing ────────────────────────────────────────────────────
    private boolean    editable;
    private final TextField cellEditor = new TextField();
    private int    editingRow    = -1;
    private int    editingColumn = -1;
    private String editingOriginalText = "";

    // ── double-click detection ────────────────────────────────────────────
    private long lastClickNs    = 0L;
    private int  lastClickRow   = -1;
    private int  lastClickCol   = -1;

    // ── column resize ─────────────────────────────────────────────────────
    private boolean columnResizeEnabled = true;
    private float   minColumnWidth      = DEFAULT_MIN_COLUMN_WIDTH;
    private int     resizingColumn      = -1;
    private float   resizeStartRootX;
    private float   resizeStartWidth;

    // ── column drag-reorder ───────────────────────────────────────────────
    private int   draggingHeaderCol    = -1;
    private int   dragHeaderPointerId  = -1;
    private float dragHeaderStartRootX;
    private int   dragHeaderDropIndex  = -1;
    private boolean headerDragActive   = false;

    // ── scrollbar visibility flags (settled in arrangeScrollBars) ─────────
    private boolean verticalBarVisible   = false;
    private boolean horizontalBarVisible = false;

    // ── children list caches ─────────────────────────────────────────────
    private List<Widget> childrenBothBars;
    private List<Widget> childrenVertOnly;
    private List<Widget> childrenHorizOnly;

    // ──────────────────────────────────────────────────────────────────────
    public VirtualTableView() {
        focusable(true);

        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.onValueChanged(event -> scrollTo(event.newValue()));

        horizontalScrollBar.setParentInternal(this);
        horizontalScrollBar.onValueChanged(event -> scrollXTo(event.newValue()));

        cellEditor.themeEnabled(false);
        cellEditor.background().set(0.025f, 0.030f, 0.040f, 0.98f);
        cellEditor.borderColor().set(0.25f, 0.78f, 1.0f, 1.0f);
        cellEditor.textColor().set(1.0f, 1.0f, 1.0f, 1.0f);
        cellEditor.caretColor().set(0.25f, 0.78f, 1.0f, 1.0f);
        cellEditor.visualOnlyTextChanges(true);

        ObjectArrayList<Widget> both  = new ObjectArrayList<>();
        both.add(verticalScrollBar);
        both.add(horizontalScrollBar);
        childrenBothBars  = Collections.unmodifiableList(both);
        childrenVertOnly  = Collections.singletonList(verticalScrollBar);
        childrenHorizOnly = Collections.singletonList(horizontalScrollBar);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Style
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected String styleType() {
        return STYLE_TYPE;
    }

    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Data.VIRTUAL_TABLE_VIEW;
    }

    @Override
    public VirtualTableView style(String declarations) {
        super.style(declarations);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public VirtualTableView style(Consumer<dev.sixik.unigui.api.style.MutableStyle> update) {
        super.style(update);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public VirtualTableView styleClear() {
        super.styleClear();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Row count
    // ══════════════════════════════════════════════════════════════════════

    @WikiMethod(
            title = "<key:virtual.table.view.method.row_count.title>",
            summary = "<key:virtual.table.view.method.row_count.summary>")
    public int rowCount() {
        return virtualizer.itemCount();
    }

    @XmlAttribute(value = "rowCount", category = "Data", defaultValue = "0",
            description = "Number of virtual rows exposed by the table.")
    @WikiMethod(
            title = "<key:virtual.table.view.method.row_count.title>",
            summary = "<key:virtual.table.view.method.row_count.summary>")
    public VirtualTableView rowCount(int rowCount) {
        if (virtualizer.itemCount() == Math.max(0, rowCount)) return this;
        List<Integer> oldSelection = selection.selectedIndices();
        virtualizer.itemCount(rowCount);
        activeRow = clampRowOrNone(activeRow);
        if (editingRow >= virtualizer.itemCount()) cancelEdit();
        sortDirty = true;
        emitSelectionChangeIfChanged(oldSelection, selection.retainWithin(virtualizer.itemCount()));
        scrollTo(virtualizer.scrollOffset());
        hoverRow = -1;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    // ── dynamic row operations ────────────────────────────────────────────

    /**
     * Вставляет {@code count} строк перед позицией {@code index} в модели данных.
     *
     * <p>Индекс выделения, активная и редактируемая строки сдвигаются. Порядок сортировки сбрасывается.</p>
     *
     * @param index позиция вставки (clamp в [0, rowCount])
     * @param count число вставляемых строк; неположительное значение игнорируется
     * @return this
     */
    public VirtualTableView insertRows(int index, int count) {
        if (count <= 0) return this;
        int clampedIndex = Math.max(0, Math.min(index, virtualizer.itemCount()));
        List<Integer> oldSelection = selection.selectedIndices();

        virtualizer.itemCount(virtualizer.itemCount() + count);
        boolean selChanged = selection.shiftForInsert(clampedIndex, count);

        if (activeRow >= clampedIndex) activeRow += count;
        if (editingRow >= clampedIndex) {
            cancelEdit();
        }
        hoverRow = -1;
        sortDirty = true;
        scrollTo(virtualizer.scrollOffset());
        emitSelectionChangeIfChanged(oldSelection, selChanged);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableRowsChangedEvent(this, clampedIndex, count, true));
        return this;
    }

    /**
     * Добавляет одну строку в конец таблицы.
     *
     * @return this
     */
    public VirtualTableView addRow() {
        return insertRows(virtualizer.itemCount(), 1);
    }

    /**
     * Удаляет {@code count} строк начиная с позиции {@code index}.
     *
     * @param index начало удаляемого диапазона
     * @param count число строк для удаления
     * @return this
     */
    public VirtualTableView removeRows(int index, int count) {
        if (count <= 0 || virtualizer.itemCount() == 0) return this;
        int clampedIndex = Math.max(0, Math.min(index, virtualizer.itemCount() - 1));
        int clampedCount = Math.min(count, virtualizer.itemCount() - clampedIndex);
        if (clampedCount <= 0) return this;

        List<Integer> oldSelection = selection.selectedIndices();

        // cancel edit if editing row falls in removed range
        if (editingRow >= clampedIndex && editingRow < clampedIndex + clampedCount) {
            cancelEdit();
        } else if (editingRow >= clampedIndex + clampedCount) {
            editingRow -= clampedCount;
        }

        // update active row
        if (activeRow >= clampedIndex && activeRow < clampedIndex + clampedCount) {
            activeRow = clampRowOrNone(clampedIndex);
        } else if (activeRow >= clampedIndex + clampedCount) {
            activeRow -= clampedCount;
        }

        boolean selChanged = selection.shiftForRemove(clampedIndex, clampedCount);
        virtualizer.itemCount(virtualizer.itemCount() - clampedCount);
        activeRow = clampRowOrNone(activeRow);
        hoverRow = -1;
        sortDirty = true;
        scrollTo(virtualizer.scrollOffset());
        emitSelectionChangeIfChanged(oldSelection, selChanged);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableRowsChangedEvent(this, clampedIndex, clampedCount, false));
        return this;
    }

    /**
     * Удаляет строку по индексу модели.
     *
     * @param index индекс строки
     * @return this
     */
    public VirtualTableView removeRow(int index) {
        return removeRows(index, 1);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Columns
    // ══════════════════════════════════════════════════════════════════════

    @WikiMethod(
            title = "<key:virtual.table.view.method.columns.title>",
            summary = "<key:virtual.table.view.method.columns.summary>")
    public VirtualTableView columns(List<VirtualTableColumn> columns) {
        this.columns.clear();
        nextModelIndex = 0;
        if (columns != null) {
            for (VirtualTableColumn col : columns) {
                if (col != null) this.columns.add(adoptColumn(col));
            }
        }
        normalizeSortColumn();
        activeColumn = clampColumnOrNone(activeColumn);
        if (editingColumn >= this.columns.size()) cancelEdit();
        scrollXTo(scrollX);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:virtual.table.view.method.add_column.title>",
            summary = "<key:virtual.table.view.method.add_column.summary>")
    public VirtualTableView addColumn(String header, float width) {
        return insertColumn(columns.size(), new VirtualTableColumn(header, width));
    }

    public VirtualTableView addColumn(RichText header, float width) {
        return insertColumn(columns.size(), new VirtualTableColumn(header, width));
    }

    /**
     * Вставляет колонку перед позицией {@code index} в порядке отображения.
     *
     * @param index позиция вставки (clamp в [0, columnCount])
     * @param col   описание колонки
     * @return this
     */
    public VirtualTableView insertColumn(int index, VirtualTableColumn col) {
        if (col == null) return this;
        int clampedIndex = Math.max(0, Math.min(index, columns.size()));
        VirtualTableColumn adopted = adoptColumn(col);
        columns.add(clampedIndex, adopted);

        // shift state indices that sit at or past insertion point
        if (activeColumn >= clampedIndex)  activeColumn++;
        if (editingColumn >= clampedIndex) editingColumn++;
        if (sortColumnIndex >= clampedIndex) sortColumnIndex++;

        remapColumnComparatorsForInsert(clampedIndex);
        arrangeCellEditor();
        scrollXTo(scrollX);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableColumnsChangedEvent(this, clampedIndex, adopted.modelIndex(), true));
        return this;
    }

    /**
     * Удаляет колонку по позиции отображения.
     *
     * @param index позиция удаляемой колонки
     * @return this
     */
    public VirtualTableView removeColumn(int index) {
        int normalized = normalizeColumnIndex(index);
        if (normalized < 0) return this;
        VirtualTableColumn removed = columns.get(normalized);
        int removedModelIndex = removed.modelIndex();

        if (editing() && editingColumn == normalized) cancelEdit();
        columns.remove(normalized);
        columnComparators.remove(removedModelIndex);

        // update sort
        if (sortColumnIndex == normalized) {
            int oldSortCol = sortColumnIndex;
            SortDirection oldDir = sortDirection;
            sortColumnIndex = -1;
            sortDirection   = SortDirection.NONE;
            sortDirty = true;
            emit(new TableSortChangedEvent(this, oldSortCol, oldDir, -1, SortDirection.NONE));
        } else if (sortColumnIndex > normalized) {
            sortColumnIndex--;
        }

        if (activeColumn >= columns.size()) activeColumn = columns.isEmpty() ? -1 : columns.size() - 1;
        if (editingColumn > normalized) editingColumn--;

        remapColumnComparatorsForRemove(normalized);
        arrangeCellEditor();
        scrollXTo(scrollX);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableColumnsChangedEvent(this, normalized, removedModelIndex, false));
        return this;
    }

    public List<VirtualTableColumn> columns() {
        return columnsView;
    }

    @WikiMethod(
            title = "<key:virtual.table.view.method.column_width.title>",
            summary = "<key:virtual.table.view.method.column_width.summary>")
    public float columnWidth(int columnIndex) {
        int normalized = normalizeColumnIndex(columnIndex);
        return normalized < 0 ? 0.0f : columns.get(normalized).width();
    }

    @WikiMethod(
            title = "<key:virtual.table.view.method.resize_column.title>",
            summary = "<key:virtual.table.view.method.resize_column.summary>")
    public VirtualTableView resizeColumn(int columnIndex, float width) {
        setColumnWidth(columnIndex, width, true);
        return this;
    }

    public boolean columnResizeEnabled() { return columnResizeEnabled; }

    @XmlAttribute(value = "columnResizeEnabled", category = "Behavior", defaultValue = "true",
            description = "Whether table columns can be resized by pointer input.")
    @WikiMethod(
            title = "<key:virtual.table.view.method.column_resize_enabled.title>",
            summary = "<key:virtual.table.view.method.column_resize_enabled.summary>")
    public VirtualTableView columnResizeEnabled(boolean columnResizeEnabled) {
        if (this.columnResizeEnabled == columnResizeEnabled) return this;
        this.columnResizeEnabled = columnResizeEnabled;
        if (!columnResizeEnabled) endColumnResize();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float minColumnWidth() { return minColumnWidth; }

    @XmlAttribute(value = "minColumnWidth", category = "Layout", defaultValue = "24",
            description = "Minimum width applied while resizing columns.")
    @WikiMethod(
            title = "<key:virtual.table.view.method.min_column_width.title>",
            summary = "<key:virtual.table.view.method.min_column_width.summary>")
    public VirtualTableView minColumnWidth(float minColumnWidth) {
        float normalized = Float.isFinite(minColumnWidth) ? Math.max(1.0f, minColumnWidth) : DEFAULT_MIN_COLUMN_WIDTH;
        if (this.minColumnWidth == normalized) return this;
        this.minColumnWidth = normalized;
        Object[] raw = columns.elements();
        for (int i = 0, size = columns.size(); i < size; i++) {
            setColumnWidth(i, ((VirtualTableColumn) raw[i]).width(), false);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:virtual.table.view.method.move_column.title>",
            summary = "<key:virtual.table.view.method.move_column.summary>")
    public VirtualTableView moveColumn(int oldIndex, int newIndex) {
        int from = normalizeColumnIndex(oldIndex);
        int to   = normalizeColumnIndex(newIndex);
        if (from < 0 || to < 0 || from == to) return this;

        VirtualTableColumn col = columns.remove(from);
        columns.add(to, col);
        activeColumn    = remapColumnIndex(activeColumn, from, to);
        editingColumn   = remapColumnIndex(editingColumn, from, to);
        sortColumnIndex = remapColumnIndex(sortColumnIndex, from, to);
        remapColumnComparators(from, to);
        arrangeCellEditor();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableColumnMovedEvent(this, from, to));
        return this;
    }

    public boolean resizingColumn()      { return resizingColumn >= 0; }
    public int     resizingColumnIndex() { return resizingColumn; }

    public EventSubscription onColumnResized(EventListener<? super TableColumnResizedEvent> listener) {
        return on(TableColumnResizedEvent.TYPE, listener);
    }
    public EventSubscription onColumnMoved(EventListener<? super TableColumnMovedEvent> listener) {
        return on(TableColumnMovedEvent.TYPE, listener);
    }
    public EventSubscription onColumnsChanged(EventListener<? super TableColumnsChangedEvent> listener) {
        return on(TableColumnsChangedEvent.TYPE, listener);
    }
    public EventSubscription onRowsChanged(EventListener<? super TableRowsChangedEvent> listener) {
        return on(TableRowsChangedEvent.TYPE, listener);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Cell providers
    // ══════════════════════════════════════════════════════════════════════

    public VirtualTableView cellTextProvider(BiFunction<Integer, Integer, String> cellTextProvider) {
        this.cellTextProvider = cellTextProvider == null ? (row, col) -> "" : cellTextProvider;
        sortDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView cellRichTextProvider(BiFunction<Integer, Integer, RichText> cellRichTextProvider) {
        this.cellRichTextProvider = cellRichTextProvider;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Sort
    // ══════════════════════════════════════════════════════════════════════

    public boolean sortingEnabled() { return sortingEnabled; }

    @XmlAttribute(value = "sortingEnabled", category = "Behavior", defaultValue = "true",
            description = "Whether clicking column headers cycles through sort states.")
    public VirtualTableView sortingEnabled(boolean sortingEnabled) {
        this.sortingEnabled = sortingEnabled;
        return this;
    }

    public VirtualTableView sortKeyProvider(BiFunction<Integer, Integer, ? extends Comparable<?>> sortKeyProvider) {
        this.sortKeyProvider = sortKeyProvider == null ? (row, col) -> cellText(row, col) : sortKeyProvider;
        sortDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView columnComparator(int columnIndex, Comparator<Integer> comparator) {
        if (columnIndex < 0) return this;
        int modelIdx = modelIndexOf(columnIndex);
        if (comparator == null) columnComparators.remove(modelIdx);
        else columnComparators.put(modelIdx, comparator);
        sortDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int           sortColumnIndex() { return sortColumnIndex; }
    public SortDirection sortDirection()   { return sortDirection; }

    public VirtualTableView sortBy(int columnIndex, SortDirection direction) {
        SortDirection normalizedDirection = direction == null ? SortDirection.NONE : direction;
        int normalizedColumn = normalizedDirection == SortDirection.NONE ? -1 : normalizeColumnIndex(columnIndex);
        if (normalizedColumn < 0) normalizedDirection = SortDirection.NONE;
        if (sortColumnIndex == normalizedColumn && sortDirection == normalizedDirection) return this;

        int oldColumn         = sortColumnIndex;
        SortDirection oldDir  = sortDirection;
        sortColumnIndex       = normalizedColumn;
        sortDirection         = normalizedDirection;
        sortDirty             = true;
        scrollTo(0.0f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emit(new TableSortChangedEvent(this, oldColumn, oldDir, sortColumnIndex, sortDirection));
        return this;
    }

    public VirtualTableView clearSort() { return sortBy(-1, SortDirection.NONE); }

    public VirtualTableView cycleSort(int columnIndex) {
        int normalized = normalizeColumnIndex(columnIndex);
        if (normalized < 0) return this;
        if (sortColumnIndex != normalized || sortDirection == SortDirection.NONE)
            return sortBy(normalized, SortDirection.ASCENDING);
        if (sortDirection == SortDirection.ASCENDING)
            return sortBy(normalized, SortDirection.DESCENDING);
        return clearSort();
    }

    public EventSubscription onSortChanged(EventListener<? super TableSortChangedEvent> listener) {
        return on(TableSortChangedEvent.TYPE, listener);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Column reorder
    // ══════════════════════════════════════════════════════════════════════

    public boolean columnReorderEnabled() { return columnReorderEnabled; }

    @XmlAttribute(value = "columnReorderEnabled", category = "Behavior", defaultValue = "true",
            description = "Whether columns can be reordered by dragging their headers.")
    public VirtualTableView columnReorderEnabled(boolean enabled) {
        columnReorderEnabled = enabled;
        if (!enabled) cancelHeaderDrag();
        return this;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Editing
    // ══════════════════════════════════════════════════════════════════════

    public boolean editable()    { return editable; }
    public int     editingRow()  { return editingRow; }
    public int     editingColumn() { return editingColumn; }
    public boolean editing()     { return editingRow >= 0 && editingColumn >= 0; }
    public String  editingText() { return editing() ? cellEditor.text() : ""; }

    @XmlAttribute(value = "editable", category = "Behavior", defaultValue = "false",
            description = "Whether cells can enter inline edit mode.")
    public VirtualTableView editable(boolean editable) {
        if (this.editable == editable) return this;
        this.editable = editable;
        if (!editable) cancelEdit();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView editingText(String text) {
        if (editing()) { cellEditor.text(text); invalidate(InvalidationFlags.VISUAL); }
        return this;
    }

    public VirtualTableView beginEdit() { return beginEdit(activeRow, activeColumn); }

    public VirtualTableView beginEdit(int row, int column) {
        if (!editable) return this;
        int normRow = clampRowOrNone(row);
        int normCol = clampColumnOrNone(column);
        if (normRow < 0 || normCol < 0) return this;
        if (editing() && editingRow == normRow && editingColumn == normCol) return this;
        if (editing()) commitEdit();

        activeRow    = normRow;
        activeColumn = normCol;
        ensureActiveVisible();
        editingRow    = normRow;
        editingColumn = normCol;
        int modelCol = modelIndexOf(normCol);
        editingOriginalText = cellText(normRow, modelCol);
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
        int row = editingRow, col = editingColumn;
        String oldText = editingOriginalText, newText = cellEditor.text();
        endEditFocus();
        editingRow = -1; editingColumn = -1; editingOriginalText = "";
        sortDirty = true;
        emit(new TableCellEditCommittedEvent(this, row, col, oldText, newText));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView cancelEdit() {
        if (!editing()) return this;
        int row = editingRow, col = editingColumn;
        String text = cellEditor.text();
        endEditFocus();
        editingRow = -1; editingColumn = -1; editingOriginalText = "";
        emit(new TableCellEditCancelledEvent(this, row, col, text));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onCellEditStarted(EventListener<? super TableCellEditStartedEvent> l)    { return on(TableCellEditStartedEvent.TYPE, l); }
    public EventSubscription onCellEditCommitted(EventListener<? super TableCellEditCommittedEvent> l) { return on(TableCellEditCommittedEvent.TYPE, l); }
    public EventSubscription onCellEditCancelled(EventListener<? super TableCellEditCancelledEvent> l) { return on(TableCellEditCancelledEvent.TYPE, l); }

    // ══════════════════════════════════════════════════════════════════════
    // Layout / display settings
    // ══════════════════════════════════════════════════════════════════════

    public float rowHeight() { return virtualizer.itemExtent(); }

    @XmlAttribute(value = "rowHeight", category = "Layout", defaultValue = "18",
            description = "Fixed body row height used by virtualization.")
    public VirtualTableView rowHeight(float rowHeight) {
        float prev = virtualizer.itemExtent();
        virtualizer.itemExtent(rowHeight);
        if (prev == virtualizer.itemExtent()) return this;
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float headerHeight() { return headerHeight; }

    @XmlAttribute(value = "headerHeight", category = "Layout", defaultValue = "18",
            description = "Header row height in UI pixels.")
    public VirtualTableView headerHeight(float headerHeight) {
        float normalized = Float.isFinite(headerHeight) ? Math.max(0.0f, headerHeight) : 18.0f;
        if (this.headerHeight == normalized) return this;
        this.headerHeight = normalized;
        scrollTo(virtualizer.scrollOffset());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int overscan() { return virtualizer.overscan(); }

    @XmlAttribute(value = "overscan", category = "Behavior", defaultValue = "1",
            description = "Extra rows realized above and below the viewport.")
    public VirtualTableView overscan(int overscan) {
        int prev = virtualizer.overscan();
        virtualizer.overscan(overscan);
        if (prev == virtualizer.overscan()) return this;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "scrollStep", category = "Behavior", defaultValue = "16",
            description = "UI pixels scrolled per wheel/page delta unit.")
    public VirtualTableView scrollStep(float scrollStep) {
        this.scrollStep = Float.isFinite(scrollStep) ? Math.max(1.0f, scrollStep) : 16.0f;
        syncScrollBars();
        return this;
    }

    public boolean consumeWheelAtScrollBounds() { return consumeWheelAtScrollBounds; }

    @XmlAttribute(value = "consumeWheelAtScrollBounds", category = "Behavior", defaultValue = "true",
            description = "Whether wheel events are consumed at scroll range boundaries.")
    public VirtualTableView consumeWheelAtScrollBounds(boolean consumeWheelAtScrollBounds) {
        this.consumeWheelAtScrollBounds = consumeWheelAtScrollBounds;
        return this;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Scroll
    // ══════════════════════════════════════════════════════════════════════

    public float scrollY() { return virtualizer.scrollOffset(); }
    public float scrollX() { return scrollX; }

    public float contentWidth() {
        float w = 0.0f;
        Object[] raw = columns.elements();
        for (int i = 0, size = columns.size(); i < size; i++) w += ((VirtualTableColumn) raw[i]).width();
        return w;
    }
    public float contentHeight() { return virtualizer.contentExtent(); }
    public float maxScrollY()    { return Math.max(0.0f, contentHeight() - rowViewportHeight()); }
    public float maxScrollX()    { return Math.max(0.0f, contentWidth()  - viewportWidth()); }

    public VirtualTableView scrollTo(float y) {
        updateVirtualizerViewport();
        float before = virtualizer.scrollOffset();
        virtualizer.scrollOffset(y);
        if (before == virtualizer.scrollOffset()) return this;
        syncScrollBars();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView scrollBy(float dy) { return scrollTo(virtualizer.scrollOffset() + dy); }

    /**
     * Прокручивает таблицу так, чтобы строка с индексом {@code row} стала видимой.
     *
     * @param row индекс строки модели
     * @return this
     */
    public VirtualTableView scrollToRow(int row) {
        if (row < 0 || row >= virtualizer.itemCount()) return this;
        int vr = visualRowOfSource(row);
        if (vr < 0) return this;
        float top = vr * virtualizer.itemExtent();
        float bottom = top + virtualizer.itemExtent();
        float vtop = virtualizer.scrollOffset();
        float vbot = vtop + rowViewportHeight();
        if (top < vtop) scrollTo(top);
        else if (bottom > vbot) scrollTo(bottom - rowViewportHeight());
        return this;
    }

    public VirtualTableView scrollXTo(float x) {
        float clamped = Math.max(0.0f, Math.min(x, maxScrollX()));
        if (scrollX == clamped) return this;
        scrollX = clamped;
        syncScrollBars();
        arrangeCellEditor();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView scrollXBy(float dx) { return scrollXTo(scrollX + dx); }

    // ══════════════════════════════════════════════════════════════════════
    // Realized range
    // ══════════════════════════════════════════════════════════════════════

    public int         firstVisibleRow()            { return realizedRange().firstIndex(); }
    public int         lastVisibleRowExclusive()     { return realizedRange().lastIndexExclusive(); }
    public VirtualRange realizedRange()              { return virtualizer.visibleRange(); }
    public int         realizedRowCount()            { return realizedRange().count(); }

    // ══════════════════════════════════════════════════════════════════════
    // Selection
    // ══════════════════════════════════════════════════════════════════════

    public SelectionMode selectionMode()  { return selection.mode(); }

    @XmlAttribute(value = "selectionMode", category = "Behavior", defaultValue = "single",
            description = "Selection mode used by table rows.")
    public VirtualTableView selectionMode(SelectionMode mode) {
        List<Integer> old = selection.selectedIndices();
        selection.mode(mode);
        emitSelectionChangeIfChanged(old, !old.equals(selection.selectedIndices()));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int          selectedRow()                { return selection.selectedIndex(); }
    public List<Integer> selectedRows()              { return selection.selectedIndices(); }
    public boolean      isRowSelected(int row)       { return selection.isSelected(row); }
    public int          activeRow()                  { return activeRow; }
    public int          activeColumn()               { return activeColumn; }

    public VirtualTableView activeCell(int row, int column) {
        int normRow = clampRowOrNone(row);
        int normCol = clampColumnOrNone(column);
        if (activeRow == normRow && activeColumn == normCol) return this;
        activeRow = normRow; activeColumn = normCol;
        ensureActiveVisible();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView selectRow(int row) {
        List<Integer> old = selection.selectedIndices();
        activeRow = clampRowOrNone(row);
        if (activeColumn < 0) activeColumn = firstColumnOrNone();
        emitSelectionChangeIfChanged(old, selection.select(row));
        ensureActiveVisible();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView toggleRow(int row) {
        List<Integer> old = selection.selectedIndices();
        emitSelectionChangeIfChanged(old, selection.toggle(row));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView clearSelection() {
        List<Integer> old = selection.selectedIndices();
        emitSelectionChangeIfChanged(old, selection.clear());
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> l) {
        return on(SelectionChangedEvent.TYPE, l);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Render phase
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Возвращает фазу текущего render-прохода.
     *
     * <p>Поле обновляется самим виджетом перед каждым обращением к renderer'у и позволяет
     * единому {@code WidgetRender} рисовать фазо-зависимый контент.</p>
     */
    public RenderPhase renderPhase() { return renderPhase; }

    public ScrollBar verticalScrollBar()   { return verticalScrollBar; }
    public ScrollBar horizontalScrollBar() { return horizontalScrollBar; }

    // ══════════════════════════════════════════════════════════════════════
    // Widget lifecycle overrides
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.setUiContextInternal(uiContext);
        horizontalScrollBar.setParentInternal(this);
        horizontalScrollBar.setUiContextInternal(uiContext);
        cellEditor.setUiContextInternal(uiContext);
    }

    @Override
    public List<Widget> children() {
        boolean v = hasVerticalScrollBar();
        boolean h = hasHorizontalScrollBar();
        if (v && h)  return childrenBothBars;
        if (v)       return childrenVertOnly;
        if (h)       return childrenHorizOnly;
        return Collections.emptyList();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) { setDesiredSize(0.0f, 0.0f); return; }
        setDesiredSize(resolveDesiredSize(context,
                contentWidth(),
                headerHeight + Math.min(contentHeight(), Math.max(0.0f, rowHeight() * 8.0f))));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        updateVirtualizerViewport();
        virtualizer.scrollOffset(virtualizer.scrollOffset());
        scrollXTo(scrollX);
        arrangeCellEditor();
        arrangeScrollBars();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
        if (hasVerticalScrollBar())   verticalScrollBar.tick(frame);
        if (hasHorizontalScrollBar()) horizontalScrollBar.tick(frame);
        if (editing()) cellEditor.tick(frame);
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        pushOpacity(context);
        try {
            WidgetRender custom = customRender();
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());

            // Phase: HEADER (custom renderer gets full pass)
            renderHeader(context, custom);

            // Phase: ROWS (clip to row viewport)
            draw.pushClip(layoutBounds().x(), rowViewportY(), viewportWidth(), rowViewportHeight());
            context.pushTextPixelSnap(false);
            try {
                renderRows(context, custom);
                if (editing()) renderChildWithInheritedTransform(context, cellEditor);
            } finally {
                context.popTextPixelSnap();
            }
            draw.popClip();

            // drag-reorder drop marker
            if (headerDragActive && dragHeaderDropIndex >= 0) {
                renderDropMarker(draw);
            }

            if (hasVerticalScrollBar())   renderChildWithInheritedTransform(context, verticalScrollBar);
            if (hasHorizontalScrollBar()) renderChildWithInheritedTransform(context, horizontalScrollBar);
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        if (columnResizeEnabled && enabled() && visibility() == Visibility.VISIBLE
                && (resizingColumn >= 0 || isHeaderLocalY(localY) && resizeColumnAt(localX) >= 0)) {
            return MouseCursor.RESIZE_HORIZONTAL;
        }
        return super.mouseCursorAt(localX, localY);
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;

        // hover tracking
        if (event instanceof PointerMovedEvent moved && moved.phase() != EventPhase.CAPTURE) {
            updateHoverFromRoot(moved.rootX(), moved.rootY());
        } else if (event instanceof PointerEnteredEvent entered && entered.phase() != EventPhase.CAPTURE) {
            updateHoverFromRoot(entered.rootX(), entered.rootY());
        } else if (event instanceof PointerExitedEvent exited && exited.phase() == EventPhase.TARGET) {
            clearHover();
        }

        if (handleColumnResizeEvent(event)) { event.cancel(); return; }
        if (handleHeaderDragEvent(event))   { event.cancel(); return; }
        if (editing() && handleEditingEvent(event)) { event.cancel(); return; }

        if (event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float dX = scroll.deltaX();
            float dY = scroll.deltaY();
            // shift-wheel → horizontal scroll
            if (KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT)) {
                dX = dY; dY = 0.0f;
            }
            float beforeY = virtualizer.scrollOffset();
            float beforeX = scrollX;
            if (dY != 0.0f) scrollBy(-dY * scrollStep);
            if (dX != 0.0f) scrollXBy(-dX * scrollStep);
            boolean moved = virtualizer.scrollOffset() != beforeY || scrollX != beforeX;
            if (moved) { event.cancel(); return; }
            if (consumeWheelAtScrollBounds) {
                if (dY != 0.0f && maxScrollY() > 0.0f) { event.cancel(); return; }
                if (dX != 0.0f && maxScrollX() > 0.0f) { event.cancel(); return; }
            }
        } else if (event instanceof KeyPressedEvent key && key.phase() != EventPhase.CAPTURE) {
            if (handleKey(key.keyCode(), key.modifiers())) { event.cancel(); return; }
        } else if (event instanceof PointerPressedEvent pointer
                && pointer.phase() != EventPhase.CAPTURE
                && pointer.button() == PointerButton.PRIMARY) {
            float lx = localX(pointer);
            float ly = localY(pointer);
            float vw = viewportWidth();
            if (lx >= 0.0f && lx < vw && ly >= 0.0f && ly < effectiveHeaderHeight()) {
                // header click: resize handled earlier; this is sort / start-drag
                int col = columnAt(lx);
                if (col >= 0 && resizeColumnAt(lx) < 0) {
                    requestFocus();
                    activeColumn = col;
                    if (editing()) commitEdit();
                    startHeaderPress(col, pointer.rootX(), pointer.pointerId());
                    event.cancel();
                }
            } else if (lx >= 0.0f && lx < vw
                    && ly >= effectiveHeaderHeight()
                    && ly < rowViewportHeight() + effectiveHeaderHeight()) {
                int row = rowAt(ly);
                if (row >= 0) {
                    requestFocus();
                    if (editing()) commitEdit();
                    int srcRow = sourceRowAt(row);
                    handleRowClick(srcRow, columnAt(lx), pointer.rootX(), pointer.rootY());
                    event.cancel();
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Internal – header rendering
    // ══════════════════════════════════════════════════════════════════════

    private void renderHeader(RenderContext context, WidgetRender custom) {
        renderPhase = RenderPhase.HEADER;
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (custom != null) { custom.render(draw, this); return; }
        renderHeaderVisual(draw);
    }

    private void renderHeaderVisual(DrawScope draw) {
        RenderContext ctx = draw.context();
        float x   = layoutBounds().x();
        float y   = layoutBounds().y();
        float vw  = viewportWidth();
        float hh  = effectiveHeaderHeight();
        if (hh <= 0.0f) return;

        // header background
        ColorView headerBg = styleValue(StyleKeys.TABLE_HEADER_COLOR,
                new MutableColor(0.10f, 0.10f, 0.10f, 1.0f));
        draw.rect(x, y, vw, hh, Paint.fill(headerBg));

        ColorView textColor = resolveHeaderTextColor();
        ColorView gridColor = resolveGridColor();
        TableGridLines gridLines = styleValue(StyleKeys.TABLE_GRID_LINES, TableGridLines.BOTH);
        float padding = Math.max(0.0f, styleValue(StyleKeys.TABLE_CELL_PADDING, 3.0f));

        float columnX = x - scrollX;
        Object[] raw = columns.elements();
        for (int ci = 0, size = columns.size(); ci < size; ci++) {
            VirtualTableColumn col = (VirtualTableColumn) raw[ci];
            float colLeft  = columnX;
            float colRight = columnX + col.width();
            if (colRight <= x) { columnX += col.width(); continue; }
            if (colLeft  >= x + vw) break;

            // per-column visible width
            float drawLeft  = Math.max(colLeft, x);
            float drawWidth = Math.min(colRight, x + vw) - drawLeft;

            // hover highlight
            boolean colHovered = (ci == hoverHeaderCol && !headerDragActive) ||
                    (headerDragActive && ci == draggingHeaderCol);
            if (colHovered) {
                ColorView hoverBg = styleValue(StyleKeys.TABLE_HEADER_HOVER_COLOR,
                        new MutableColor(0.17f, 0.17f, 0.18f, 1.0f));
                draw.rect(drawLeft, y, drawWidth, hh, Paint.fill(hoverBg));
            }

            // sort indicator slot
            boolean sorted   = ci == sortColumnIndex;
            float textWidth  = col.width() - padding * 2.0f;
            if (sorted) textWidth -= SORT_INDICATOR_SLOT;
            textWidth = Math.max(0.0f, textWidth);

            // header text (clipped)
            RichText headerTxt = col.richHeader();
            if (!headerTxt.isEmpty() && textWidth > 0.0f) {
                List<TextSegment> segs = textSegments(ctx, headerTxt, col,
                        colLeft + padding, y, textWidth, hh, 0.0f, false);
                draw.pushTextClip(drawLeft, y, drawWidth, hh);
                try { drawTextSegments(draw, segs, textColor); }
                finally { draw.popClip(); }
            }

            // sort indicator
            if (sorted) {
                float indicatorX = colLeft + col.width() - SORT_INDICATOR_SLOT;
                float indicatorW = SORT_INDICATOR_SLOT;
                float clipLeft   = Math.max(indicatorX, x);
                float clipW      = Math.min(indicatorX + indicatorW, x + vw) - clipLeft;
                if (clipW > 0.0f) {
                    draw.pushClip(clipLeft, y, clipW, hh);
                    try { renderSortIndicator(draw, indicatorX, y, indicatorW, hh, textColor); }
                    finally { draw.popClip(); }
                }
            }

            // vertical grid line
            if (gridLines.vertical()) {
                float lineX = Math.min(colRight, x + vw);
                if (lineX > x && lineX <= x + vw) {
                    boolean isResizing = ci == resizingColumn;
                    float thickness = isResizing ? 2.0f : 1.0f;
                    ColorView lineColor = isResizing ?
                            styleValue(StyleKeys.TABLE_ACTIVE_CELL_COLOR, new MutableColor(1.0f, 1.0f, 1.0f, 0.72f)) :
                            gridColor;
                    draw.line(lineX, y, lineX, y + hh, Paint.stroke(lineColor, thickness));
                }
            }

            columnX += col.width();
        }

        // bottom border of header
        draw.line(x, y + hh, x + vw, y + hh, Paint.stroke(gridColor, 1.0f));
    }

    private void renderSortIndicator(DrawScope draw, float x, float y, float w, float h,
                                     ColorView fallback) {
        ColorView indicatorColor = styleValue(StyleKeys.TABLE_SORT_INDICATOR_COLOR, fallback);
        if (indicatorColor == null) indicatorColor = fallback;

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        boolean ascending = sortDirection == SortDirection.ASCENDING;

        TableSortIndicatorStyle style = styleValue(StyleKeys.TABLE_SORT_INDICATOR_STYLE,
                TableSortIndicatorStyle.CHEVRON);

        if (style == TableSortIndicatorStyle.TRIANGLE) {
            float half = 3.5f;
            draw.pathClear();
            if (ascending) {
                draw.pathLineTo(cx - half, cy + 2.0f);
                draw.pathLineTo(cx + half, cy + 2.0f);
                draw.pathLineTo(cx,        cy - 2.5f);
            } else {
                draw.pathLineTo(cx - half, cy - 2.0f);
                draw.pathLineTo(cx + half, cy - 2.0f);
                draw.pathLineTo(cx,        cy + 2.5f);
            }
            draw.pathFillConvex(indicatorColor);
        } else {
            // CHEVRON
            Paint p = Paint.stroke(indicatorColor, CHEVRON_STROKE);
            if (ascending) {
                draw.line(cx - CHEVRON_HALF, cy + CHEVRON_RISE, cx, cy - CHEVRON_RISE, p);
                draw.line(cx, cy - CHEVRON_RISE, cx + CHEVRON_HALF, cy + CHEVRON_RISE, p);
            } else {
                draw.line(cx - CHEVRON_HALF, cy - CHEVRON_RISE, cx, cy + CHEVRON_RISE, p);
                draw.line(cx, cy + CHEVRON_RISE, cx + CHEVRON_HALF, cy - CHEVRON_RISE, p);
            }
        }
    }

    private void renderDropMarker(DrawScope draw) {
        float x   = layoutBounds().x();
        float y   = layoutBounds().y();
        float hh  = effectiveHeaderHeight();
        float vw  = viewportWidth();

        // compute drop marker x
        float markerX = x - scrollX;
        Object[] raw = columns.elements();
        int drop = Math.max(0, Math.min(dragHeaderDropIndex, columns.size()));
        for (int ci = 0; ci < drop && ci < columns.size(); ci++) {
            markerX += ((VirtualTableColumn) raw[ci]).width();
        }
        markerX = Math.max(x, Math.min(markerX, x + vw));

        ColorView activeColor = styleValue(StyleKeys.TABLE_ACTIVE_CELL_COLOR,
                new MutableColor(1.0f, 1.0f, 1.0f, 0.72f));
        draw.line(markerX, y, markerX, y + hh, Paint.stroke(activeColor, 2.0f));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Internal – rows rendering
    // ══════════════════════════════════════════════════════════════════════

    private void renderRows(RenderContext context, WidgetRender custom) {
        renderPhase = RenderPhase.ROWS;
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (custom != null) { custom.render(draw, this); return; }
        renderRowsVisual(draw);
    }

    private void renderRowsVisual(DrawScope draw) {
        RenderContext ctx = draw.context();
        ensureSortIndex();
        boolean focused = isFocused();

        float tableX  = layoutBounds().x();
        float tableW  = viewportWidth();
        float rvY     = rowViewportY();
        float rvH     = rowViewportHeight();

        ColorView textColor   = resolveTextColor();
        ColorView gridColor   = resolveGridColor();
        ColorView selectedBg  = styleValue(StyleKeys.TABLE_SELECTED_COLOR,
                new MutableColor(0.18f, 0.45f, 0.75f, 0.42f));
        ColorView rowBg       = styleValue(StyleKeys.TABLE_ROW_COLOR,
                new MutableColor(0.04f, 0.04f, 0.04f, 0.35f));
        ColorView altRowBg    = styleValue(StyleKeys.TABLE_ALTERNATE_ROW_COLOR,
                new MutableColor(0.08f, 0.08f, 0.08f, 0.35f));
        ColorView hoverBg     = styleValue(StyleKeys.TABLE_HOVER_COLOR,
                new MutableColor(1.0f, 1.0f, 1.0f, 0.07f));
        ColorView activeColor = styleValue(StyleKeys.TABLE_ACTIVE_CELL_COLOR,
                new MutableColor(1.0f, 1.0f, 1.0f, 0.72f));
        TableGridLines gridLines = styleValue(StyleKeys.TABLE_GRID_LINES, TableGridLines.BOTH);
        float padding = Math.max(0.0f, styleValue(StyleKeys.TABLE_CELL_PADDING, 3.0f));

        for (int vr = firstVisibleRow(); vr < lastVisibleRowExclusive(); vr++) {
            int srcRow = sourceRowAt(vr);
            float rowY = rvY + virtualizer.itemOffset(vr);
            float rowH = rowHeight();

            // row background
            ColorView bg;
            if (selection.isSelected(srcRow)) {
                bg = selectedBg;
            } else {
                bg = ((vr & 1) == 0) ? rowBg : altRowBg;
            }
            draw.rect(tableX, rowY, tableW, rowH, Paint.fill(bg));

            // hover overlay
            if (vr == hoverRow && !selection.isSelected(srcRow)) {
                draw.rect(tableX, rowY, tableW, rowH, Paint.fill(hoverBg));
            }

            // cells
            float columnX = tableX - scrollX;
            Object[] raw = columns.elements();
            for (int ci = 0, size = columns.size(); ci < size; ci++) {
                VirtualTableColumn col = (VirtualTableColumn) raw[ci];
                float colRight = columnX + col.width();
                if (colRight <= tableX) { columnX += col.width(); continue; }
                if (columnX  >= tableX + tableW) break;

                float drawLeft  = Math.max(columnX, tableX);
                float drawWidth = Math.min(colRight, tableX + tableW) - drawLeft;

                boolean editingCell = editing() && srcRow == editingRow && ci == editingColumn;

                if (!editingCell) {
                    int modelCol = col.modelIndex();
                    RichText txt = cellDisplayText(srcRow, modelCol);
                    if (!txt.isEmpty() && drawWidth > 0.0f) {
                        float innerWidth = Math.max(0.0f, col.width() - padding * 2.0f);
                        List<TextSegment> segs = textSegments(ctx, txt, col,
                                columnX + padding, rowY, innerWidth, rowH, 0.0f, true);
                        draw.pushTextClip(drawLeft, rowY, drawWidth, rowH);
                        try { drawTextSegments(draw, segs, textColor); }
                        finally { draw.popClip(); }
                    }
                }

                // vertical grid
                if (gridLines.vertical()) {
                    float lineX = Math.min(colRight, tableX + tableW);
                    if (lineX > tableX && lineX <= tableX + tableW) {
                        draw.line(lineX, rowY, lineX, rowY + rowH, Paint.stroke(gridColor, 1.0f));
                    }
                }

                // active cell outline
                boolean active = srcRow == activeRow && ci == activeColumn;
                if (focused && active && !editingCell) {
                    draw.rect(drawLeft, rowY, drawWidth, rowH, Paint.stroke(activeColor, 1.0f));
                }

                columnX += col.width();
            }

            // horizontal grid
            if (gridLines.horizontal()) {
                draw.line(tableX, rowY + rowH, tableX + tableW, rowY + rowH,
                        Paint.stroke(gridColor, 1.0f));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Text drawing helpers
    // ══════════════════════════════════════════════════════════════════════

    private void drawTextSegments(DrawScope draw, List<TextSegment> segments, ColorView color) {
        if (color == null) color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
        for (TextSegment seg : segments) {
            if (seg.text() == null || seg.text().isEmpty()) continue;
            DrawScope sd = seg.transform() == null ? draw : draw.withTransform(seg.transform());
            TextEngine.drawInline(sd, seg.text(), seg.x(), seg.y(), seg.width(), seg.height(),
                    Paint.fill(color));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Event sub-handlers
    // ══════════════════════════════════════════════════════════════════════

    private boolean handleEditingEvent(Event event) {
        if (event instanceof TextInputEvent input && input.phase() == EventPhase.TARGET) {
            cellEditor.handle(new TextInputEvent(cellEditor, input.codePoint(), input.modifiers()));
            invalidate(InvalidationFlags.VISUAL);
            return true;
        }
        if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET) {
            if (key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER) {
                commitEdit(); return true;
            }
            if (key.keyCode() == KeyCodes.ESCAPE) {
                cancelEdit(); return true;
            }
            cellEditor.handle(new KeyPressedEvent(cellEditor, key.keyCode(), key.scanCode(), key.modifiers()));
            invalidate(InvalidationFlags.VISUAL);
            return true;
        }
        return false;
    }

    private boolean handleColumnResizeEvent(Event event) {
        if (!columnResizeEnabled || visibility() != Visibility.VISIBLE || !enabled()) return false;
        if (event instanceof PointerMovedEvent moved && moved.phase() != EventPhase.CAPTURE && resizingColumn >= 0) {
            updateColumnResize(moved.rootX()); return true;
        }
        if (event instanceof PointerReleasedEvent released
                && released.phase() != EventPhase.CAPTURE
                && released.button() == PointerButton.PRIMARY
                && resizingColumn >= 0) {
            updateColumnResize(released.rootX());
            UIContext ctx = uiContext();
            if (ctx != null) ctx.releasePointer(released.pointerId(), this);
            endColumnResize();
            return true;
        }
        if (event instanceof PointerPressedEvent pressed
                && pressed.phase() != EventPhase.CAPTURE
                && pressed.button() == PointerButton.PRIMARY
                && isHeaderLocalY(localY(pressed))) {
            int col = resizeColumnAt(localX(pressed));
            if (col >= 0) { startColumnResize(col, pressed.rootX(), pressed.pointerId()); return true; }
        }
        return false;
    }

    private void startHeaderPress(int col, float rootX, int pointerId) {
        draggingHeaderCol   = col;
        dragHeaderPointerId = pointerId;
        dragHeaderStartRootX = rootX;
        dragHeaderDropIndex  = col;
        headerDragActive     = false;
        UIContext ctx = uiContext();
        if (ctx != null) ctx.capturePointer(pointerId, this);
    }

    private boolean handleHeaderDragEvent(Event event) {
        if (draggingHeaderCol < 0) return false;
        if (event instanceof PointerMovedEvent moved && moved.phase() != EventPhase.CAPTURE) {
            float delta = moved.rootX() - dragHeaderStartRootX;
            if (!headerDragActive && Math.abs(delta) > COLUMN_DRAG_THRESHOLD && columnReorderEnabled) {
                headerDragActive = true;
            }
            if (headerDragActive) {
                float lx = localX(moved) + scrollX;
                dragHeaderDropIndex = columnDropIndexAt(lx);
                invalidate(InvalidationFlags.VISUAL);
            }
            return headerDragActive;
        }
        if (event instanceof PointerReleasedEvent released
                && released.phase() != EventPhase.CAPTURE
                && released.button() == PointerButton.PRIMARY) {
            UIContext ctx = uiContext();
            if (ctx != null) ctx.releasePointer(released.pointerId(), this);
            if (headerDragActive) {
                int from = draggingHeaderCol;
                int to   = dragHeaderDropIndex;
                // adjust 'to': when moving right, subtract 1 because insertion is after removal
                if (to > from) to--;
                moveColumn(from, Math.max(0, Math.min(to, columns.size() - 1)));
            } else {
                // just a click → sort
                if (sortingEnabled) cycleSort(draggingHeaderCol);
            }
            cancelHeaderDrag();
            return true;
        }
        return false;
    }

    private void cancelHeaderDrag() {
        draggingHeaderCol   = -1;
        dragHeaderPointerId = -1;
        headerDragActive    = false;
        dragHeaderDropIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void handleRowClick(int srcRow, int col, float rootX, float rootY) {
        activeRow    = clampRowOrNone(srcRow);
        activeColumn = col >= 0 ? col : firstColumnOrNone();

        // double-click → begin edit
        long now = System.nanoTime();
        if (editable && srcRow == lastClickRow && col == lastClickCol
                && (now - lastClickNs) < DOUBLE_CLICK_NS) {
            lastClickNs = 0; lastClickRow = -1; lastClickCol = -1;
            beginEdit(srcRow, activeColumn);
        } else {
            lastClickNs  = now;
            lastClickRow = srcRow;
            lastClickCol = col;
        }
        selectRow(srcRow);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Hover
    // ══════════════════════════════════════════════════════════════════════

    private void updateHoverFromRoot(float rootX, float rootY) {
        lastPointerRootX = rootX;
        lastPointerRootY = rootY;
        refreshHover();
    }

    private void clearHover() {
        lastPointerRootX = Float.NaN;
        lastPointerRootY = Float.NaN;
        boolean changed = hoverRow >= 0 || hoverHeaderCol >= 0;
        hoverRow = -1;
        hoverHeaderCol = -1;
        if (changed) invalidate(InvalidationFlags.VISUAL);
    }

    private void refreshHover() {
        if (!enabled() || Float.isNaN(lastPointerRootX)) { clearHover(); return; }
        float lx = lastPointerRootX - layoutBounds().x();
        float ly = lastPointerRootY - layoutBounds().y();
        int newHoverRow = -1, newHoverHeaderCol = -1;
        if (lx >= 0 && lx < viewportWidth() && ly >= 0 && ly < layoutBounds().height()) {
            if (isHeaderLocalY(ly)) {
                newHoverHeaderCol = columnAt(lx);
            } else {
                newHoverRow = rowAt(ly);
            }
        }
        if (newHoverRow != hoverRow || newHoverHeaderCol != hoverHeaderCol) {
            hoverRow = newHoverRow;
            hoverHeaderCol = newHoverHeaderCol;
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Keyboard navigation
    // ══════════════════════════════════════════════════════════════════════

    private boolean handleKey(int keyCode, int modifiers) {
        if (virtualizer.itemCount() <= 0) return false;
        return switch (keyCode) {
            case KeyCodes.UP    -> { moveActiveRow(navigationBaseVisualRow() - 1, modifiers); yield true; }
            case KeyCodes.DOWN  -> { moveActiveRow(navigationBaseVisualRow() + 1, modifiers); yield true; }
            case KeyCodes.PAGE_UP   -> { moveActiveRow(navigationBaseVisualRow() - pageRowCount(), modifiers); yield true; }
            case KeyCodes.PAGE_DOWN -> { moveActiveRow(navigationBaseVisualRow() + pageRowCount(), modifiers); yield true; }
            case KeyCodes.HOME  -> { moveActiveRow(0, modifiers); yield true; }
            case KeyCodes.END   -> { moveActiveRow(virtualizer.itemCount() - 1, modifiers); yield true; }
            case KeyCodes.LEFT  -> { moveActiveColumn(activeColumn < 0 ? firstColumnOrNone() : activeColumn - 1); yield true; }
            case KeyCodes.RIGHT -> { moveActiveColumn(activeColumn < 0 ? firstColumnOrNone() : activeColumn + 1); yield true; }
            case KeyCodes.F2    -> { beginEdit(); yield editable; }
            case KeyCodes.SPACE, KeyCodes.ENTER, KeyCodes.KEYPAD_ENTER -> {
                if (editable && (keyCode == KeyCodes.ENTER || keyCode == KeyCodes.KEYPAD_ENTER)
                        && !KeyModifiers.has(modifiers, KeyModifiers.SHIFT)
                        && !KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
                    if (activeRow < 0) activeRow = sourceRowAt(navigationBaseVisualRow());
                    if (activeColumn < 0) activeColumn = firstColumnOrNone();
                    beginEdit();
                } else {
                    selectActiveRow(KeyModifiers.has(modifiers, KeyModifiers.CONTROL),
                            KeyModifiers.has(modifiers, KeyModifiers.SHIFT));
                }
                yield true;
            }
            default -> false;
        };
    }

    private void moveActiveRow(int visualRow, int modifiers) {
        int target = Math.max(0, Math.min(virtualizer.itemCount() - 1, visualRow));
        int src    = sourceRowAt(target);
        activeRow  = src;
        if (activeColumn < 0) activeColumn = firstColumnOrNone();
        ensureActiveVisible();
        if (!KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
            List<Integer> old = selection.selectedIndices();
            boolean changed = KeyModifiers.has(modifiers, KeyModifiers.SHIFT)
                    ? selection.selectRange(src)
                    : selection.select(src);
            emitSelectionChangeIfChanged(old, changed);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void moveActiveColumn(int col) {
        int norm = clampColumnOrNone(col);
        if (norm < 0) return;
        if (activeRow < 0) activeRow = sourceRowAt(navigationBaseVisualRow());
        activeColumn = norm;
        ensureActiveVisibleHorizontal();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void selectActiveRow(boolean toggle, boolean range) {
        if (activeRow < 0) activeRow = sourceRowAt(navigationBaseVisualRow());
        if (activeColumn < 0) activeColumn = firstColumnOrNone();
        if (activeRow < 0) return;
        List<Integer> old = selection.selectedIndices();
        boolean changed = range ? selection.selectRange(activeRow)
                : (toggle ? selection.toggle(activeRow) : selection.select(activeRow));
        emitSelectionChangeIfChanged(old, changed);
        ensureActiveVisible();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Scroll helpers
    // ══════════════════════════════════════════════════════════════════════

    private void ensureActiveVisible() {
        ensureActiveVisibleVertical();
        ensureActiveVisibleHorizontal();
    }

    private void ensureActiveVisibleVertical() {
        int vr = visualRowOfSource(activeRow);
        if (vr < 0) return;
        updateVirtualizerViewport();
        float top    = vr * virtualizer.itemExtent();
        float bottom = top + virtualizer.itemExtent();
        float vtop   = virtualizer.scrollOffset();
        float vbot   = vtop + rowViewportHeight();
        if (top < vtop)      scrollTo(top);
        else if (bottom > vbot) scrollTo(bottom - rowViewportHeight());
    }

    private void ensureActiveVisibleHorizontal() {
        int col = activeColumn;
        if (col < 0 || col >= columns.size()) return;
        float vw = viewportWidth();
        float left = 0.0f;
        Object[] raw = columns.elements();
        for (int ci = 0; ci < col; ci++) left += ((VirtualTableColumn) raw[ci]).width();
        float right = left + ((VirtualTableColumn) raw[col]).width();
        if (left < scrollX)       scrollXTo(left);
        else if (right > scrollX + vw) scrollXTo(right - vw);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Column resize internals
    // ══════════════════════════════════════════════════════════════════════

    private void startColumnResize(int colIdx, float rootX, int pointerId) {
        resizingColumn = normalizeColumnIndex(colIdx);
        if (resizingColumn < 0) return;
        resizeStartRootX = rootX;
        resizeStartWidth = columns.get(resizingColumn).width();
        if (editing()) commitEdit();
        UIContext ctx = uiContext();
        if (ctx != null) { ctx.focusManager().requestFocus(this); ctx.capturePointer(pointerId, this); }
        invalidate(InvalidationFlags.VISUAL);
    }

    private void updateColumnResize(float rootX) {
        if (resizingColumn < 0) return;
        setColumnWidth(resizingColumn, resizeStartWidth + rootX - resizeStartRootX, true);
    }

    private void endColumnResize() {
        if (resizingColumn < 0) return;
        resizingColumn = -1; resizeStartRootX = 0.0f; resizeStartWidth = 0.0f;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setColumnWidth(int colIdx, float width, boolean emitChange) {
        int norm = normalizeColumnIndex(colIdx);
        if (norm < 0) return;
        VirtualTableColumn col     = columns.get(norm);
        float normalized           = Float.isFinite(width) ? Math.max(minColumnWidth, width) : Math.max(minColumnWidth, col.width());
        if (col.width() == normalized) return;
        columns.set(norm, col.width(normalized));
        scrollXTo(scrollX);
        arrangeCellEditor();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) emit(new TableColumnResizedEvent(this, norm, col.width(), normalized));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Layout helpers
    // ══════════════════════════════════════════════════════════════════════

    private void updateVirtualizerViewport() {
        virtualizer.viewportExtent(rowViewportHeight());
    }

    private float effectiveHeaderHeight() {
        return Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()));
    }

    private float rowViewportY() {
        return layoutBounds().y() + effectiveHeaderHeight();
    }

    private float rowViewportHeight() {
        float h = layoutBounds().height() - effectiveHeaderHeight();
        if (horizontalBarVisible) h -= SCROLLBAR_WIDTH + 2.0f;
        return Math.max(0.0f, h);
    }

    private float viewportWidth() {
        float w = layoutBounds().width();
        if (verticalBarVisible) w -= SCROLLBAR_WIDTH + SCROLLBAR_GAP;
        return Math.max(0.0f, w);
    }

    private boolean hasVerticalScrollBar()   { return verticalBarVisible; }
    private boolean hasHorizontalScrollBar() { return horizontalBarVisible; }

    /**
     * Вычисляет, нужны ли вертикальный и горизонтальный скроллбары.
     *
     * <p>Цикл решает fixed-point: вертикальный бар уменьшает ширину, что может убрать
     * горизонтальный, и наоборот.</p>
     */
    private void settleScrollBars() {
        verticalBarVisible   = false;
        horizontalBarVisible  = false;
        for (int i = 0; i < 4; i++) {
            boolean v = contentHeight() - rowViewportHeight() > 0.5f;
            boolean h = contentWidth()  - viewportWidth()   > 0.5f;
            if (v == verticalBarVisible && h == horizontalBarVisible) break;
            verticalBarVisible   = v;
            horizontalBarVisible  = h;
        }
    }

    private void arrangeScrollBars() {
        updateVirtualizerViewport();
        settleScrollBars();
        if (verticalBarVisible) {
            verticalScrollBar.arrange(new MutableRect(
                    layoutBounds().x() + layoutBounds().width() - SCROLLBAR_WIDTH,
                    rowViewportY(),
                    SCROLLBAR_WIDTH,
                    rowViewportHeight()));
        }
        if (hasHorizontalScrollBar()) {
            horizontalScrollBar.arrange(new MutableRect(
                    layoutBounds().x(),
                    layoutBounds().y() + layoutBounds().height() - SCROLLBAR_WIDTH - 1.0f,
                    viewportWidth(),
                    SCROLLBAR_WIDTH));
        }
        syncScrollBars();
    }

    private void syncScrollBars() {
        verticalScrollBar
                .range(0.0f, maxScrollY())
                .pageSize(Math.max(1.0f, rowViewportHeight()))
                .step(scrollStep)
                .silentValue(virtualizer.scrollOffset());
        horizontalScrollBar
                .range(0.0f, maxScrollX())
                .pageSize(Math.max(1.0f, viewportWidth()))
                .step(scrollStep)
                .silentValue(scrollX);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Cell editor
    // ══════════════════════════════════════════════════════════════════════

    private void arrangeCellEditor() {
        if (!editing()) return;
        MutableRect cb = cellBounds(editingRow, editingColumn);
        if (cb == null) { cellEditor.arrange(new MutableRect(0.0f, 0.0f, 0.0f, 0.0f)); return; }
        StackPanel.arrangeChild(cellEditor, cb.x(), cb.y(), cb.width(), cb.height());
    }

    private MutableRect cellBounds(int srcRow, int colIdx) {
        int vr = visualRowOfSource(srcRow);
        if (vr < 0 || colIdx < 0 || colIdx >= columns.size()) return null;
        float y  = rowViewportY() + virtualizer.itemOffset(vr);
        float rh = rowHeight();
        if (y + rh <= rowViewportY() || y >= rowViewportY() + rowViewportHeight()) return null;
        float x  = layoutBounds().x() - scrollX;
        Object[] raw = columns.elements();
        for (int ci = 0; ci < colIdx; ci++) x += ((VirtualTableColumn) raw[ci]).width();
        float w = Math.min(columns.get(colIdx).width(),
                Math.max(0.0f, viewportWidth() - (x - layoutBounds().x()) - scrollX));
        if (w <= 0.0f) return null;
        x = Math.max(layoutBounds().x(), x);
        return new MutableRect(x, y, w, rh);
    }

    private void endEditFocus() {
        cellEditor.handle(new FocusLostEvent(cellEditor, this));
        cellEditor.clearSelection();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Hit-testing
    // ══════════════════════════════════════════════════════════════════════

    private int rowAt(float localY) {
        float lry = localY - effectiveHeaderHeight();
        int row   = (int) Math.floor((lry + virtualizer.scrollOffset()) / virtualizer.itemExtent());
        return row >= 0 && row < virtualizer.itemCount() ? row : -1;
    }

    private int columnAt(float localX) {
        float x = -scrollX;
        Object[] raw = columns.elements();
        for (int ci = 0, size = columns.size(); ci < size; ci++) {
            float w = ((VirtualTableColumn) raw[ci]).width();
            if (localX >= x && localX < x + w) return ci;
            x += w;
        }
        return -1;
    }

    /** Returns the column display index whose right divider is within slop of {@code localX}. */
    private int resizeColumnAt(float localX) {
        if (localX < 0.0f || localX >= viewportWidth()) return -1;
        float x = -scrollX;
        Object[] raw = columns.elements();
        for (int ci = 0, size = columns.size(); ci < size; ci++) {
            x += ((VirtualTableColumn) raw[ci]).width();
            if (Math.abs(localX - x) <= HEADER_RESIZE_HIT_SLOP) return ci;
        }
        return -1;
    }

    private int columnDropIndexAt(float localXWithScroll) {
        float x = 0.0f;
        Object[] raw = columns.elements();
        for (int ci = 0, size = columns.size(); ci < size; ci++) {
            float hw = ((VirtualTableColumn) raw[ci]).width() * 0.5f;
            if (localXWithScroll < x + hw) return ci;
            x += ((VirtualTableColumn) raw[ci]).width();
        }
        return columns.size();
    }

    private boolean isHeaderLocalY(float localY) {
        return localY >= 0.0f && localY < effectiveHeaderHeight();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Sort index
    // ══════════════════════════════════════════════════════════════════════

    private void ensureSortIndex() {
        if (!sortDirty) return;
        sortDirty = false;
        if (sortColumnIndex < 0 || sortDirection == SortDirection.NONE || virtualizer.itemCount() == 0) {
            sortedRows = null; return;
        }
        List<Integer> rows = new ObjectArrayList<>(virtualizer.itemCount());
        for (int i = 0; i < virtualizer.itemCount(); i++) rows.add(i);
        Comparator<Integer> comp = rowComparator();
        if (sortDirection == SortDirection.DESCENDING) comp = comp.reversed();
        rows.sort(comp.thenComparingInt(Integer::intValue));
        sortedRows = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) sortedRows[i] = rows.get(i);
    }

    private Comparator<Integer> rowComparator() {
        int modelIdx = modelIndexOf(sortColumnIndex);
        Comparator<Integer> custom = columnComparators.get(modelIdx);
        if (custom != null) return custom;
        return (l, r) -> compareSortKeys(sortKey(l, modelIdx), sortKey(r, modelIdx));
    }

    private Comparable<?> sortKey(int row, int modelCol) {
        Comparable<?> key = sortKeyProvider.apply(row, modelCol);
        return key == null ? "" : key;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareSortKeys(Comparable l, Comparable r) {
        if (l == r) return 0;
        if (l == null) return -1;
        if (r == null) return 1;
        return l.compareTo(r);
    }

    private int sourceRowAt(int visualRow) {
        ensureSortIndex();
        if (visualRow < 0 || visualRow >= virtualizer.itemCount()) return visualRow;
        return sortedRows == null ? visualRow : sortedRows[visualRow];
    }

    private int visualRowOfSource(int srcRow) {
        if (srcRow < 0 || srcRow >= virtualizer.itemCount()) return -1;
        ensureSortIndex();
        if (sortedRows == null) return srcRow;
        for (int vr = 0; vr < sortedRows.length; vr++) {
            if (sortedRows[vr] == srcRow) return vr;
        }
        return -1;
    }

    private void normalizeSortColumn() {
        if (sortColumnIndex >= columns.size()) {
            sortColumnIndex = -1; sortDirection = SortDirection.NONE;
        }
        sortDirty = true;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Cell text helpers
    // ══════════════════════════════════════════════════════════════════════

    private String cellText(int row, int modelCol) {
        String t = cellTextProvider.apply(row, modelCol);
        return t == null ? "" : t;
    }

    private RichText cellRichText(int row, int modelCol) {
        if (cellRichTextProvider == null) return null;
        RichText t = cellRichTextProvider.apply(row, modelCol);
        return (t == null || t.isEmpty()) ? null : t;
    }

    private RichText cellDisplayText(int row, int modelCol) {
        RichText rt = cellRichText(row, modelCol);
        return rt != null ? rt : RichText.plain(cellText(row, modelCol));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Text layout helpers (unchanged from original)
    // ══════════════════════════════════════════════════════════════════════

    private List<TextSegment> textSegments(RenderContext context, RichText text,
                                           VirtualTableColumn column,
                                           float x, float y, float width, float height,
                                           float padding, boolean allowColumnAlignment) {
        if (context == null || text == null || text.isEmpty() || width <= 0.0f || height <= 0.0f) return List.of();
        VirtualTableColumn safe = column == null ? new VirtualTableColumn("", width) : column;
        float innerX     = x + padding;
        float innerWidth = Math.max(0.0f, width - padding * 2.0f);
        if (innerWidth <= 0.0f) return List.of();
        Alignment h = allowColumnAlignment ? safe.horizontalAlignment() : Alignment.START;
        Alignment v = safe.verticalAlignment();
        return richTextSegments(context, text, innerX, y, innerWidth, height, h, v, safe.overflowMode());
    }

    private List<TextSegment> richTextSegments(RenderContext context, RichText text,
                                               float x, float y, float width, float height,
                                               Alignment h, Alignment v, TextOverflowMode overflow) {
        if (text == null || text.isEmpty()) return List.of();
        TextSegment seg = overflow == TextOverflowMode.SHRINK_TO_FIT
                ? shrinkToFitSegment(context, text, x, y, width, height, h, v)
                : alignedTextSegment(context, text, x, y, width, height, h, v, null);
        return seg == null ? List.of() : List.of(seg);
    }

    private TextSegment alignedTextSegment(RenderContext context, RichText text,
                                           float x, float y, float width, float height,
                                           Alignment h, Alignment v, Transform transform) {
        if (text == null || text.isEmpty()) return null;
        float tw = Math.min(width,  TextEngine.measureLineWidth(context, text));
        float th = Math.min(height, TextEngine.measureTextHeight(context, text));
        float dx = TextEngine.alignedStart(x, width,  tw, h);
        float dy = TextEngine.alignedStart(y, height, th, v);
        return new TextSegment(text, dx, dy, Math.max(0.0f, width - (dx - x)), th, transform);
    }

    private TextSegment shrinkToFitSegment(RenderContext context, RichText text,
                                           float x, float y, float width, float height,
                                           Alignment h, Alignment v) {
        float tw = TextEngine.measureLineWidth(context, text);
        if (tw <= 0.0f) return null;
        float sh = TextEngine.measureTextHeight(context, text);
        float scale = width <= 0.0f ? 1.0f : Math.min(1.0f, width / tw);
        float sw = tw * scale, sch = sh * scale;
        float dx = TextEngine.alignedStart(x, width,  sw, h);
        float dy = TextEngine.alignedStart(y, height, sch, v);
        Transform scaled = transform().copy();
        scaled.scale().set(transform().scale().x() * scale, transform().scale().y() * scale);
        return new TextSegment(text, dx, dy, tw, sh, scaled);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Column model-index management
    // ══════════════════════════════════════════════════════════════════════

    private VirtualTableColumn adoptColumn(VirtualTableColumn col) {
        if (col.modelIndex() != VirtualTableColumn.UNASSIGNED) {
            // track highest used model index so future auto-assigns don't collide
            nextModelIndex = Math.max(nextModelIndex, col.modelIndex() + 1);
            return col;
        }
        return col.modelIndex(nextModelIndex++);
    }

    private int modelIndexOf(int displayColIndex) {
        if (displayColIndex < 0 || displayColIndex >= columns.size()) return -1;
        return columns.get(displayColIndex).modelIndex();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Column comparator remap
    // ══════════════════════════════════════════════════════════════════════

    private void remapColumnComparators(int oldIndex, int newIndex) {
        // comparators are already keyed by modelIndex – no remapping needed for model-indexed map
        // (this method is kept for symmetry; the indices don't change on move)
    }

    private void remapColumnComparatorsForInsert(int insertedDisplayIndex) {
        // model-index keyed map: no remapping needed
    }

    private void remapColumnComparatorsForRemove(int removedDisplayIndex) {
        // model-index keyed map: caller already removed the entry
    }

    // ══════════════════════════════════════════════════════════════════════
    // Misc private helpers
    // ══════════════════════════════════════════════════════════════════════

    private int remapColumnIndex(int colIdx, int oldIndex, int newIndex) {
        if (colIdx < 0) return -1;
        if (colIdx == oldIndex) return newIndex;
        if (oldIndex < newIndex && colIdx > oldIndex && colIdx <= newIndex) return colIdx - 1;
        if (oldIndex > newIndex && colIdx >= newIndex && colIdx < oldIndex) return colIdx + 1;
        return colIdx;
    }

    private int navigationBaseVisualRow() {
        int av = visualRowOfSource(activeRow);
        if (av >= 0) return av;
        int sv = visualRowOfSource(selection.selectedIndex());
        if (sv >= 0) return sv;
        return Math.min(Math.max(0, firstVisibleRow()), Math.max(0, virtualizer.itemCount() - 1));
    }

    private int pageRowCount() {
        float extent = Math.max(1.0f, virtualizer.itemExtent());
        return Math.max(1, (int) Math.floor(Math.max(extent, rowViewportHeight()) / extent));
    }

    private int clampRowOrNone(int row)    { return row >= 0 && row < virtualizer.itemCount() ? row : -1; }
    private int clampColumnOrNone(int col) { return col >= 0 && col < columns.size() ? col : -1; }
    private int firstColumnOrNone()        { return columns.isEmpty() ? -1 : 0; }
    private int normalizeColumnIndex(int colIdx) { return colIdx >= 0 && colIdx < columns.size() ? colIdx : -1; }

    private float localX(PointerEvent p) { return p.rootX() - layoutBounds().x(); }
    private float localY(PointerEvent p) { return p.rootY() - layoutBounds().y(); }

    private ColorView resolveTextColor() {
        return styleValue(StyleKeys.TEXT_COLOR, new MutableColor(1.0f, 1.0f, 1.0f, 1.0f));
    }
    private ColorView resolveHeaderTextColor() {
        ColorView c = styleValue(StyleKeys.TABLE_HEADER_TEXT_COLOR, (ColorView) null);
        return c != null ? c : resolveTextColor();
    }
    private ColorView resolveGridColor() {
        return styleValue(StyleKeys.TABLE_GRID_COLOR, new MutableColor(0.25f, 0.25f, 0.25f, 0.65f));
    }

    private void requestFocus() {
        UIContext ctx = uiContext();
        if (ctx != null) ctx.focusManager().requestFocus(this);
    }
    private boolean isFocused() {
        UIContext ctx = uiContext();
        return ctx != null && ctx.focusManager().isFocused(this);
    }

    private void emitSelectionChangeIfChanged(List<Integer> old, boolean changed) {
        if (changed) emit(new SelectionChangedEvent(this, old, selection.selectedIndices()));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Enums / records
    // ══════════════════════════════════════════════════════════════════════

    /** Фаза render-прохода, которую видит {@link WidgetRender}. */
    public enum RenderPhase { HEADER, ROWS }

    record TextSegment(RichText text, float x, float y, float width, float height, Transform transform) {}
}
