package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.GridAutoFlow;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.impl.layout.SlotLayout;
import dev.sixik.unigui.impl.layout.flex.FlexSolver;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid-бэкенд раскладки: явные шаблоны, размещение с объединением, автопоток и fr-треки.
 *
 * <p>Треки разрешаются как упрощённый CSS Grid относительно фиксированного размера контента: сначала
 * фиксированные и процентные треки, контентные треки ({@code auto}) — по самому крупному элементу,
 * затем {@code fr}-треки делят оставшееся свободное место. Элементы без явных позиций
 * размещаются автоматически в потоке строк или столбцов, при необходимости заполняя дыры в плотном режиме. Каждый элемент
 * помещается в область своей ячейки с выравниванием {@link SlotLayout}.</p>
 *
 * <p>Упрощения относительно полного CSS Grid: объединённые элементы не влияют на
 * размер треков {@code auto}, минимумы {@code fr} используют измеренные размеры (прокси max-content),
 * а процентные треки при неопределённых размерах ведут себя как {@code auto}.</p>
 */
public final class LayoutV3GridAdapter {
    private static final LayoutNodeId ROOT_ID = LayoutNodeId.of("root");

    private LayoutV3GridAdapter() {
    }

    public static LayoutSize measure(List<Widget> children,
                                     int columns,
                                     float horizontalSpacing,
                                     float verticalSpacing,
                                     LayoutContext context,
                                     LayoutStyle containerStyle) {
        LayoutStyle style = containerStyle == null ? new LayoutStyle() : containerStyle;
        EdgeInsets padding = style.padding();
        LayoutContext childContext = context == null
                ? null
                : new LayoutContext(
                subtractAvailable(context.availableWidth(), padding.horizontal()),
                subtractAvailable(context.availableHeight(), padding.vertical()));
        List<Widget> snapshot = visibleLayoutChildren(children);
        measureAbsoluteChildren(children, childContext);
        if (snapshot.isEmpty()) {
            return LayoutSize.of(padding.horizontal(), padding.vertical());
        }
        for (Widget child : snapshot) {
            child.measure(childContext);
        }
        float availableWidth = childContext == null ? Float.POSITIVE_INFINITY : childContext.availableWidth();
        float availableHeight = childContext == null ? Float.POSITIVE_INFINITY : childContext.availableHeight();
        GridPlan plan = planGrid(style, snapshot, columns, horizontalSpacing, verticalSpacing,
                availableWidth, availableHeight, true);
        return LayoutSize.of(plan.contentWidth + padding.horizontal(), plan.contentHeight + padding.vertical());
    }

    public static void arrange(List<Widget> children,
                               int columns,
                               float horizontalSpacing,
                               float verticalSpacing,
                               RectView bounds,
                               LayoutStyle containerStyle) {
        if (bounds == null) {
            return;
        }
        LayoutStyle style = containerStyle == null ? new LayoutStyle() : containerStyle;
        EdgeInsets padding = style.padding();
        MutableRect contentBounds = new MutableRect(
                bounds.x() + padding.left(),
                bounds.y() + padding.top(),
                Math.max(0.0f, bounds.width() - padding.horizontal()),
                Math.max(0.0f, bounds.height() - padding.vertical()));

        LayoutOutput.Builder output = LayoutOutput.builder(ROOT_ID)
                .add(new LayoutResult(ROOT_ID, 0.0f, 0.0f, bounds.width(), bounds.height(),
                        contentBounds.width(), contentBounds.height()));
        Map<LayoutNodeId, Widget> widgets = new LinkedHashMap<>();
        List<Widget> snapshot = visibleLayoutChildren(children);
        if (!snapshot.isEmpty()) {
            GridPlan plan = planGrid(style, snapshot, columns, horizontalSpacing, verticalSpacing,
                    contentBounds.width(), contentBounds.height(), false);
            for (int index = 0; index < snapshot.size(); index++) {
                Widget child = snapshot.get(index);
                GridCell cell = plan.cells.get(index);
                float x = contentBounds.x() + plan.columnPositions[cell.column()];
                float y = contentBounds.y() + plan.rowPositions[cell.row()];
                float width = spanSize(plan.columnSizes, cell.column(), cell.columnSpan(), plan.columnGap());
                float height = spanSize(plan.rowSizes, cell.row(), cell.rowSpan(), plan.rowGap());
                addResult(output, widgets, LayoutNodeId.of("cell" + originalIndex(children, child)),
                        child, SlotLayout.placeChild(child, x, y, width, height), bounds);
            }
        }

        addAbsoluteResults(children, output, widgets, contentBounds, bounds);
        LayoutApplier.apply(output.build(), widgets, bounds);
    }

    private static GridPlan planGrid(LayoutStyle style,
                                      List<Widget> children,
                                      int columns,
                                      float horizontalSpacing,
                                      float verticalSpacing,
                                      float contentWidth,
                                      float contentHeight,
                                      boolean forMeasure) {
        List<GridTrack> columnTemplate;
        if (!style.gridTemplateColumns().isEmpty() || !style.gridTemplateRows().isEmpty()) {
            columnTemplate = style.gridTemplateColumns();
        } else {
            columnTemplate = GridTrack.repeat(Math.max(1, columns), GridTrack.fr(1.0f));
        }
        List<GridTrack> rowTemplate = style.gridTemplateRows();
        int columnLines = !columnTemplate.isEmpty() ? columnTemplate.size() : Math.max(1, columns);
        List<GridCell> cells = placeItems(children, columnLines, rowTemplate.size(), style.gridAutoFlow());
        int columnCount = 0;
        int rowCount = 0;
        for (GridCell cell : cells) {
            columnCount = Math.max(columnCount, cell.column() + cell.columnSpan());
            rowCount = Math.max(rowCount, cell.row() + cell.rowSpan());
        }
        float[] columnSizes = resolveTracks(columnTemplate, style.gridAutoColumns(),
                children, cells, true, contentWidth, horizontalSpacing, columnCount, forMeasure);
        float[] rowSizes = resolveTracks(rowTemplate, style.gridAutoRows(),
                children, cells, false, contentHeight, verticalSpacing, rowCount, forMeasure);
        TrackLayout columnsLayout = layoutTracks(columnSizes, columnTemplate, style.gridAutoColumns(),
                contentWidth, horizontalSpacing, justifyMode(style), forMeasure);
        TrackLayout rowsLayout = layoutTracks(rowSizes, rowTemplate, style.gridAutoRows(),
                contentHeight, verticalSpacing, alignMode(style), forMeasure);
        float contentW = totalSize(columnSizes, columnsLayout.gap());
        float contentH = totalSize(rowSizes, rowsLayout.gap());
        return new GridPlan(cells, columnSizes, rowSizes,
                columnsLayout.positions(), rowsLayout.positions(),
                columnsLayout.gap(), rowsLayout.gap(), contentW, contentH);
    }

    private enum DistMode {
        START,
        CENTER,
        END,
        BETWEEN,
        AROUND,
        EVENLY,
        STRETCH
    }

    private static DistMode justifyMode(LayoutStyle style) {
        if (style == null || style.justifyContent() == null) {
            return DistMode.START;
        }
        return switch (style.justifyContent()) {
            case CENTER -> DistMode.CENTER;
            case END -> DistMode.END;
            case SPACE_BETWEEN -> DistMode.BETWEEN;
            case SPACE_AROUND -> DistMode.AROUND;
            case SPACE_EVENLY -> DistMode.EVENLY;
            default -> DistMode.START;
        };
    }

    private static DistMode alignMode(LayoutStyle style) {
        if (style == null || style.alignContent() == null) {
            return DistMode.STRETCH;
        }
        return switch (style.alignContent()) {
            case START -> DistMode.START;
            case CENTER -> DistMode.CENTER;
            case END -> DistMode.END;
            case SPACE_BETWEEN -> DistMode.BETWEEN;
            case SPACE_AROUND -> DistMode.AROUND;
            case SPACE_EVENLY -> DistMode.EVENLY;
            case STRETCH -> DistMode.STRETCH;
        };
    }

    private record TrackLayout(float[] positions, float gap) {
    }

    private static TrackLayout layoutTracks(float[] sizes,
                                             List<GridTrack> template,
                                             List<GridTrack> autoTracks,
                                             float contentSize,
                                             float gap,
                                             DistMode mode,
                                             boolean forMeasure) {
        if (forMeasure || sizes.length == 0) {
            return new TrackLayout(accumulate(sizes, gap), gap);
        }
        float used = totalSize(sizes, gap);
        float free = Float.isFinite(contentSize) ? Math.max(0.0f, contentSize - used) : 0.0f;
        float offset = 0.0f;
        float effectiveGap = gap;
        switch (mode) {
            case CENTER -> offset = free * 0.5f;
            case END -> offset = free;
            case BETWEEN -> {
                if (sizes.length > 1) {
                    effectiveGap += free / (sizes.length - 1);
                }
            }
            case AROUND -> {
                float share = free / sizes.length;
                offset = share * 0.5f;
                effectiveGap += share;
            }
            case EVENLY -> {
                float share = free / (sizes.length + 1);
                offset = share;
                effectiveGap += share;
            }
            case STRETCH -> stretchTracks(sizes, template, autoTracks, contentSize, free);
            default -> {
            }
        }
        float[] positions = accumulate(sizes, effectiveGap);
        for (int index = 0; index < positions.length; index++) {
            positions[index] += offset;
        }
        return new TrackLayout(positions, effectiveGap);
    }

    private static void stretchTracks(float[] sizes,
                                       List<GridTrack> template,
                                       List<GridTrack> autoTracks,
                                       float contentSize,
                                       float free) {
        if (free <= 0.0f) {
            return;
        }
        int stretchable = 0;
        for (int index = 0; index < sizes.length; index++) {
            if (isStretchableTrack(trackAt(template, autoTracks, index))) {
                stretchable++;
            }
        }
        if (stretchable == 0) {
            return;
        }
        float share = free / stretchable;
        for (int index = 0; index < sizes.length; index++) {
            GridTrack track = trackAt(template, autoTracks, index);
            if (!isStretchableTrack(track)) {
                continue;
            }
            float stretched = sizes[index] + share;
            if (track instanceof GridTrack.MinMax minmax) {
                float max = resolveMaxTrack(minmax.max(), contentSize);
                if (Float.isFinite(max)) {
                    stretched = Math.min(stretched, max);
                }
            }
            sizes[index] = stretched;
        }
    }

    private static boolean isStretchableTrack(GridTrack track) {
        if (track instanceof GridTrack.Auto) {
            return true;
        }
        if (track instanceof GridTrack.MinMax minmax) {
            return !(minmax.max() instanceof GridTrack.Fixed) && !(minmax.max() instanceof GridTrack.Percent);
        }
        return false;
    }

    private static List<GridCell> placeItems(List<Widget> children, int explicitColumns, int explicitRows, GridAutoFlow flow) {
        GridAutoFlow normalized = flow == null ? GridAutoFlow.ROW : flow;
        boolean columnFlow = normalized == GridAutoFlow.COLUMN || normalized == GridAutoFlow.COLUMN_DENSE;
        boolean dense = normalized == GridAutoFlow.ROW_DENSE || normalized == GridAutoFlow.COLUMN_DENSE;
        int lineCount = columnFlow ? explicitRows : explicitColumns;
        if (lineCount <= 0) {
            lineCount = 1;
        }
        Occupancy occupancy = new Occupancy();
        List<GridCell> output = new ObjectArrayList<>(children.size());
        for (Widget child : children) {
            LayoutStyle style = SlotLayout.childStyleOf(child);
            int columnStart = style.gridColumnStart() - 1;
            int rowStart = style.gridRowStart() - 1;
            int columnSpan = Math.max(1, style.gridColumnSpan());
            int rowSpan = Math.max(1, style.gridRowSpan());
            GridCell cell;
            if (columnStart >= 0 && rowStart >= 0) {
                cell = new GridCell(columnStart, rowStart, columnSpan, rowSpan);
                occupancy.mark(cell);
            } else if (columnStart >= 0 || rowStart >= 0) {
                cell = placeConstrained(occupancy, columnSpan, rowSpan,
                        columnStart >= 0 ? columnStart : -1,
                        rowStart >= 0 ? rowStart : -1,
                        columnFlow, dense);
            } else if (columnFlow) {
                cell = placeAuto(occupancy, lineCount, columnSpan, rowSpan, true, dense);
            } else {
                cell = placeAuto(occupancy, lineCount, columnSpan, rowSpan, false, dense);
            }
            output.add(cell);
        }
        return output;
    }

    private static GridCell placeAuto(Occupancy occupancy,
                                       int lineCount,
                                       int columnSpan,
                                       int rowSpan,
                                       boolean columnFlow,
                                       boolean dense) {
        int acrossSpan = Math.min(columnFlow ? rowSpan : columnSpan, Math.max(1, lineCount));
        int alongSpan = columnFlow ? columnSpan : rowSpan;
        int line = dense ? 0 : occupancy.cursorLine;
        int across = dense ? 0 : occupancy.cursorAcross;
        while (true) {
            while (across + acrossSpan <= lineCount) {
                GridCell candidate = columnFlow
                        ? new GridCell(line, across, alongSpan, acrossSpan)
                        : new GridCell(across, line, acrossSpan, alongSpan);
                if (!occupancy.collides(candidate)) {
                    occupancy.mark(candidate);
                    if (!dense) {
                        occupancy.cursorLine = line;
                        occupancy.cursorAcross = across + acrossSpan;
                        if (occupancy.cursorAcross >= lineCount) {
                            occupancy.cursorLine = line + 1;
                            occupancy.cursorAcross = 0;
                        }
                    }
                    return candidate;
                }
                across++;
            }
            line++;
            across = 0;
        }
    }

    private static GridCell placeConstrained(Occupancy occupancy,
                                              int columnSpan,
                                              int rowSpan,
                                              int fixedColumn,
                                              int fixedRow,
                                              boolean columnFlow,
                                              boolean dense) {
        int along = 0;
        while (true) {
            GridCell candidate = fixedColumn >= 0
                    ? new GridCell(fixedColumn, along, columnSpan, rowSpan)
                    : new GridCell(along, fixedRow, columnSpan, rowSpan);
            if (!occupancy.collides(candidate)) {
                occupancy.mark(candidate);
                if (!dense) {
                    // Sparse flow locks the cursor past definite positions on the cursor
                    // axis only: a locked row advances row flow, a locked column advances
                    // column flow. Column locks in row flow (and vice versa) leave the
                    // cursor alone so later auto items still fill earlier lines.
                    // Holes left behind are only backfilled in dense mode.
                    if (!columnFlow && fixedRow >= 0
                            && candidate.row() + candidate.rowSpan() > occupancy.cursorLine) {
                        occupancy.cursorLine = candidate.row() + candidate.rowSpan();
                        occupancy.cursorAcross = 0;
                    } else if (columnFlow && fixedColumn >= 0
                            && candidate.column() + candidate.columnSpan() > occupancy.cursorLine) {
                        occupancy.cursorLine = candidate.column() + candidate.columnSpan();
                        occupancy.cursorAcross = 0;
                    }
                }
                return candidate;
            }
            along++;
        }
    }

    private static float[] resolveTracks(List<GridTrack> template,
                                          List<GridTrack> autoTracks,
                                          List<Widget> children,
                                          List<GridCell> cells,
                                          boolean horizontal,
                                          float contentSize,
                                          float gap,
                                          int count,
                                          boolean forMeasure) {
        float[] sizes = new float[Math.max(0, count)];
        float[] frWeights = new float[Math.max(0, count)];
        float[] minFloors = new float[Math.max(0, count)];
        for (int index = 0; index < sizes.length; index++) {
            GridTrack track = trackAt(template, autoTracks, index);
            if (track instanceof GridTrack.Fixed fixed) {
                sizes[index] = fixed.pixels();
            } else if (track instanceof GridTrack.Percent percent) {
                sizes[index] = Float.isFinite(contentSize)
                        ? Math.max(0.0f, contentSize * percent.percent() / 100.0f)
                        : autoContent(children, cells, index, horizontal);
            } else if (track instanceof GridTrack.Flex) {
                sizes[index] = 0.0f;
            } else if (track instanceof GridTrack.Auto) {
                sizes[index] = autoContent(children, cells, index, horizontal);
            } else if (track instanceof GridTrack.MinMax minmax) {
                sizes[index] = resolveMinTrack(minmax.min(), children, cells, index, horizontal, contentSize);
                float max = resolveMaxTrack(minmax.max(), contentSize);
                if (Float.isFinite(max)) {
                    sizes[index] = Math.min(sizes[index], max);
                } else if (minmax.max() instanceof GridTrack.Flex flex) {
                    frWeights[index] = flex.weight();
                    minFloors[index] = sizes[index];
                    sizes[index] = minFloors[index];
                }
            }
            if (track instanceof GridTrack.Flex flex) {
                frWeights[index] = flex.weight();
            }
        }
        float totalFr = 0.0f;
        for (float weight : frWeights) {
            totalFr += weight;
        }
        if (totalFr > 0.0f && Float.isFinite(contentSize) && !forMeasure) {
            float fixedSum = 0.0f;
            for (float size : sizes) {
                fixedSum += size;
            }
            float free = Math.max(0.0f, contentSize - fixedSum - gap * Math.max(0, sizes.length - 1));
            for (int index = 0; index < sizes.length; index++) {
                if (frWeights[index] > 0.0f) {
                    sizes[index] = Math.max(minFloors[index], free * (frWeights[index] / totalFr));
                }
            }
        } else {
            for (int index = 0; index < sizes.length; index++) {
                if (frWeights[index] > 0.0f) {
                    sizes[index] = Math.max(minFloors[index], autoContent(children, cells, index, horizontal));
                }
            }
        }
        return sizes;
    }

    private static float resolveMinTrack(GridTrack min,
                                          List<Widget> children,
                                          List<GridCell> cells,
                                          int track,
                                          boolean horizontal,
                                          float contentSize) {
        if (min instanceof GridTrack.Fixed fixed) {
            return fixed.pixels();
        }
        if (min instanceof GridTrack.Percent percent) {
            return Float.isFinite(contentSize)
                    ? Math.max(0.0f, contentSize * percent.percent() / 100.0f)
                    : autoContent(children, cells, track, horizontal);
        }
        if (min instanceof GridTrack.Auto) {
            return autoContent(children, cells, track, horizontal);
        }
        if (min instanceof GridTrack.MinMax nested) {
            return resolveMinTrack(nested.min(), children, cells, track, horizontal, contentSize);
        }
        return 0.0f;
    }

    private static float resolveMaxTrack(GridTrack max, float contentSize) {
        if (max instanceof GridTrack.Fixed fixed) {
            return fixed.pixels();
        }
        if (max instanceof GridTrack.Percent percent) {
            return Float.isFinite(contentSize)
                    ? Math.max(0.0f, contentSize * percent.percent() / 100.0f)
                    : Float.POSITIVE_INFINITY;
        }
        return Float.POSITIVE_INFINITY;
    }

    private static float autoContent(List<Widget> children, List<GridCell> cells, int track, boolean horizontal) {
        float output = 0.0f;
        for (int index = 0; index < cells.size() && index < children.size(); index++) {
            GridCell cell = cells.get(index);
            int start = horizontal ? cell.column() : cell.row();
            int span = horizontal ? cell.columnSpan() : cell.rowSpan();
            if (start != track || span != 1) {
                continue;
            }
            Widget child = children.get(index);
            EdgeInsets margin = SlotLayout.marginOf(child);
            float content = horizontal
                    ? child.desiredSize().width() + margin.horizontal()
                    : child.desiredSize().height() + margin.vertical();
            output = Math.max(output, content);
        }
        return output;
    }

    private static GridTrack trackAt(List<GridTrack> template, List<GridTrack> autoTracks, int index) {
        if (index < template.size()) {
            GridTrack track = template.get(index);
            return track == null ? new GridTrack.Auto() : track;
        }
        int implicit = index - template.size();
        if (!autoTracks.isEmpty()) {
            GridTrack track = autoTracks.get(implicit % autoTracks.size());
            return track == null ? new GridTrack.Auto() : track;
        }
        return new GridTrack.Auto();
    }

    private static float[] accumulate(float[] sizes, float gap) {
        float[] positions = new float[sizes.length];
        float cursor = 0.0f;
        for (int index = 0; index < sizes.length; index++) {
            positions[index] = cursor;
            cursor += sizes[index] + gap;
        }
        return positions;
    }

    private static float totalSize(float[] sizes, float gap) {
        float output = 0.0f;
        for (float size : sizes) {
            output += size;
        }
        return output + gap * Math.max(0, sizes.length - 1);
    }

    private static float spanSize(float[] sizes, int start, int span, float gap) {
        float output = 0.0f;
        for (int index = start; index < start + span && index < sizes.length; index++) {
            output += sizes[index];
        }
        return output + gap * Math.max(0, span - 1);
    }

    private static List<Widget> visibleLayoutChildren(List<Widget> children) {
        List<Widget> output = new ObjectArrayList<>();
        if (children == null) {
            return output;
        }
        for (Widget child : children) {
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && !AbsoluteLayoutEngine.isAbsolute(child)) {
                output.add(child);
            }
        }
        return output;
    }

    private static void measureAbsoluteChildren(List<Widget> children, LayoutContext context) {
        if (children == null) {
            return;
        }
        for (Widget child : children) {
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && AbsoluteLayoutEngine.isAbsolute(child)) {
                child.measure(context);
            }
        }
    }

    private static void addAbsoluteResults(List<Widget> children,
                                            LayoutOutput.Builder output,
                                            Map<LayoutNodeId, Widget> widgets,
                                            RectView contentBounds,
                                            RectView hostBounds) {
        if (children == null) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            Widget child = children.get(index);
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && AbsoluteLayoutEngine.isAbsolute(child)) {
                addResult(output, widgets, LayoutNodeId.of("absolute" + index), child,
                        AbsoluteLayoutEngine.resolveRect(child, contentBounds), hostBounds);
            }
        }
    }

    private static void addResult(LayoutOutput.Builder output,
                                   Map<LayoutNodeId, Widget> widgets,
                                   LayoutNodeId id,
                                   Widget widget,
                                   RectView rect,
                                   RectView hostBounds) {
        output.add(LayoutResult.of(id,
                rect.x() - hostBounds.x(),
                rect.y() - hostBounds.y(),
                rect.width(),
                rect.height()));
        widgets.put(id, widget);
    }

    private static int originalIndex(List<Widget> children, Widget child) {
        return children == null ? 0 : Math.max(0, children.indexOf(child));
    }

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available)
                ? Math.max(0.0f, available - Math.max(0.0f, consumed))
                : Float.POSITIVE_INFINITY;
    }

    private record GridPlan(List<GridCell> cells,
                             float[] columnSizes,
                             float[] rowSizes,
                             float[] columnPositions,
                             float[] rowPositions,
                             float columnGap,
                             float rowGap,
                             float contentWidth,
                             float contentHeight) {
    }

    private record GridCell(int column, int row, int columnSpan, int rowSpan) {
    }

    private static final class Occupancy {
        private final java.util.Set<Long> taken = new java.util.HashSet<>();
        private int cursorLine;
        private int cursorAcross;

        private void mark(GridCell cell) {
            for (int row = cell.row(); row < cell.row() + cell.rowSpan(); row++) {
                for (int column = cell.column(); column < cell.column() + cell.columnSpan(); column++) {
                    taken.add(key(column, row));
                }
            }
        }

        private boolean collides(GridCell cell) {
            for (int row = cell.row(); row < cell.row() + cell.rowSpan(); row++) {
                for (int column = cell.column(); column < cell.column() + cell.columnSpan(); column++) {
                    if (taken.contains(key(column, row))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static long key(int column, int row) {
            return (((long) column) << 32) | (row & 0xFFFFFFFFL);
        }
    }
}
