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
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Fixed-row-height virtualized table/data-grid prototype.
 */
public class VirtualTableView extends WidgetBase {
    private static final float SCROLLBAR_WIDTH = 6.0f;
    private static final MutableColor HEADER_BACKGROUND = new MutableColor(0.10f, 0.10f, 0.10f, 1.0f);
    private static final MutableColor ROW_BACKGROUND = new MutableColor(0.04f, 0.04f, 0.04f, 0.35f);
    private static final MutableColor ALTERNATE_ROW_BACKGROUND = new MutableColor(0.08f, 0.08f, 0.08f, 0.35f);
    private static final MutableColor GRID_COLOR = new MutableColor(0.25f, 0.25f, 0.25f, 0.65f);
    private static final MutableColor TEXT_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);

    private final ScrollBar verticalScrollBar = new ScrollBar().orientation(Orientation.VERTICAL);
    private final List<VirtualTableColumn> columns = new ArrayList<>();
    private BiFunction<Integer, Integer, String> cellTextProvider = (row, column) -> "";
    private int rowCount;
    private float rowHeight = 18.0f;
    private float headerHeight = 18.0f;
    private int overscan = 1;
    private float scrollY;
    private float scrollStep = 16.0f;
    private int firstVisibleRow;
    private int lastVisibleRowExclusive;

    public VirtualTableView() {
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.onValueChanged(event -> scrollTo(event.newValue()));
    }

    public int rowCount() {
        return rowCount;
    }

    public VirtualTableView rowCount(int rowCount) {
        int normalized = Math.max(0, rowCount);
        if (this.rowCount == normalized) return this;
        this.rowCount = normalized;
        scrollTo(scrollY);
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
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView addColumn(String header, float width) {
        columns.add(new VirtualTableColumn(header, width));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<VirtualTableColumn> columns() {
        return Collections.unmodifiableList(columns);
    }

    public VirtualTableView cellTextProvider(BiFunction<Integer, Integer, String> cellTextProvider) {
        this.cellTextProvider = cellTextProvider == null ? (row, column) -> "" : cellTextProvider;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float rowHeight() {
        return rowHeight;
    }

    public VirtualTableView rowHeight(float rowHeight) {
        float normalized = Float.isFinite(rowHeight) ? Math.max(1.0f, rowHeight) : 18.0f;
        if (this.rowHeight == normalized) return this;
        this.rowHeight = normalized;
        scrollTo(scrollY);
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
        scrollTo(scrollY);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int overscan() {
        return overscan;
    }

    public VirtualTableView overscan(int overscan) {
        int normalized = Math.max(0, overscan);
        if (this.overscan == normalized) return this;
        this.overscan = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView scrollStep(float scrollStep) {
        this.scrollStep = Float.isFinite(scrollStep) ? Math.max(1.0f, scrollStep) : 16.0f;
        syncScrollBar();
        return this;
    }

    public float scrollY() {
        return scrollY;
    }

    public float contentWidth() {
        float width = 0.0f;
        for (VirtualTableColumn column : columns) {
            width += column.width();
        }
        return width;
    }

    public float contentHeight() {
        return rowCount * rowHeight;
    }

    public float maxScrollY() {
        return Math.max(0.0f, contentHeight() - rowViewportHeight());
    }

    public int firstVisibleRow() {
        return firstVisibleRow;
    }

    public int lastVisibleRowExclusive() {
        return lastVisibleRowExclusive;
    }

    public int realizedRowCount() {
        return Math.max(0, lastVisibleRowExclusive - firstVisibleRow);
    }

    public ScrollBar verticalScrollBar() {
        return verticalScrollBar;
    }

    public VirtualTableView scrollTo(float y) {
        float clamped = clamp(y, 0.0f, maxScrollY());
        if (scrollY == clamped) return this;
        scrollY = clamped;
        syncScrollBar();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public VirtualTableView scrollBy(float dy) {
        return scrollTo(scrollY + dy);
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.setUiContextInternal(uiContext);
    }

    @Override
    public List<Widget> children() {
        return hasVerticalScrollBar() ? List.of(verticalScrollBar) : Collections.emptyList();
    }

    @Override
    public void measure(LayoutContext context) {
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        scrollTo(scrollY);
        updateVisibleRows();
        arrangeScrollBar();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        if (hasVerticalScrollBar()) {
            verticalScrollBar.tick(frame);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        renderHeader(context);
        context.pushClip(layoutBounds().x(), rowViewportY(), viewportWidth(), rowViewportHeight());
        renderRows(context);
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

    private void updateVisibleRows() {
        int first = rowCount == 0 ? 0 : clampRow((int) Math.floor(scrollY / rowHeight) - overscan);
        int visibleRows = (int) Math.ceil(rowViewportHeight() / rowHeight) + overscan * 2 + 1;
        int last = Math.min(rowCount, first + Math.max(0, visibleRows));
        firstVisibleRow = first;
        lastVisibleRowExclusive = last;
    }

    private void renderHeader(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float height = Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()));
        context.rect(x, y, viewportWidth(), height, Paint.fill(HEADER_BACKGROUND), transform());
        float columnX = x;
        for (VirtualTableColumn column : columns) {
            float width = Math.min(column.width(), Math.max(0.0f, viewportWidth() - (columnX - x)));
            if (width <= 0.0f) break;
            context.text(column.header(), columnX + 3.0f, y, Math.max(0.0f, width - 6.0f), height, Paint.fill(TEXT_COLOR), transform());
            context.line(columnX + width, y, columnX + width, y + height, Paint.stroke(GRID_COLOR, 1.0f), transform());
            columnX += column.width();
        }
        context.line(x, y + height, x + viewportWidth(), y + height, Paint.stroke(GRID_COLOR, 1.0f), transform());
    }

    private void renderRows(RenderContext context) {
        float tableX = layoutBounds().x();
        for (int row = firstVisibleRow; row < lastVisibleRowExclusive; row++) {
            float y = rowViewportY() + row * rowHeight - scrollY;
            context.rect(tableX, y, viewportWidth(), rowHeight, Paint.fill((row & 1) == 0 ? ROW_BACKGROUND : ALTERNATE_ROW_BACKGROUND), transform());
            float columnX = tableX;
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                VirtualTableColumn column = columns.get(columnIndex);
                float width = Math.min(column.width(), Math.max(0.0f, viewportWidth() - (columnX - tableX)));
                if (width <= 0.0f) break;
                String text = cellText(row, columnIndex);
                if (!text.isEmpty()) {
                    context.text(text, columnX + 3.0f, y, Math.max(0.0f, width - 6.0f), rowHeight, Paint.fill(TEXT_COLOR), transform());
                }
                context.line(columnX + width, y, columnX + width, y + rowHeight, Paint.stroke(GRID_COLOR, 1.0f), transform());
                columnX += column.width();
            }
            context.line(tableX, y + rowHeight, tableX + viewportWidth(), y + rowHeight, Paint.stroke(GRID_COLOR, 1.0f), transform());
        }
    }

    private String cellText(int row, int column) {
        String text = cellTextProvider.apply(row, column);
        return text == null ? "" : text;
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
                .silentValue(scrollY);
    }

    private boolean hasVerticalScrollBar() {
        return maxScrollY() > 0.0f;
    }

    private float viewportWidth() {
        return Math.max(0.0f, layoutBounds().width() - (hasVerticalScrollBar() ? SCROLLBAR_WIDTH : 0.0f));
    }

    private float rowViewportY() {
        return layoutBounds().y() + Math.min(headerHeight, Math.max(0.0f, layoutBounds().height()));
    }

    private float rowViewportHeight() {
        return Math.max(0.0f, layoutBounds().height() - headerHeight);
    }

    private int clampRow(int index) {
        return Math.max(0, Math.min(rowCount, index));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
