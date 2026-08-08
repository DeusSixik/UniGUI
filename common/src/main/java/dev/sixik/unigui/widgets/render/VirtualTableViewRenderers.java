package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

public final class VirtualTableViewRenderers {
    public static final VirtualTableViewRenderer DEFAULT = (draw, state) -> {
        if (state.phase() == VirtualTableViewRenderPhase.HEADER) {
            drawHeader(draw, state);
        } else {
            drawRows(draw, state);
        }
    };

    private VirtualTableViewRenderers() {
    }

    private static void drawHeader(DrawScope draw, VirtualTableViewState state) {
        draw.rect(state.x(), state.y(), state.viewportWidth(), state.headerHeight(),
                Paint.fill(state.headerBackground()));
        for (VirtualTableViewColumnState column : state.columns()) {
            drawText(draw, state, column.clipped(), column.clipX(), column.clipY(), column.clipWidth(), column.clipHeight(),
                    column.textSegments());
            float thickness = column.resizing() ? 2.0f : 1.0f;
            draw.line(column.x() + column.width(), column.y(), column.x() + column.width(), column.y() + column.height(),
                    Paint.stroke(column.resizing() ? state.activeCellColor() : state.gridColor(), thickness));
        }
        draw.line(state.x(), state.y() + state.headerHeight(), state.x() + state.viewportWidth(),
                state.y() + state.headerHeight(), Paint.stroke(state.gridColor(), 1.0f));
    }

    private static void drawRows(DrawScope draw, VirtualTableViewState state) {
        for (VirtualTableViewRowState row : state.rows()) {
            draw.rect(row.x(), row.y(), row.width(), row.height(), Paint.fill(row.background()));
            for (VirtualTableViewCellState cell : row.cells()) {
                if (!cell.editing()) {
                    drawText(draw, state, cell.clipped(), cell.clipX(), cell.clipY(), cell.clipWidth(), cell.clipHeight(),
                            cell.textSegments());
                }
                draw.line(cell.x() + cell.width(), cell.y(), cell.x() + cell.width(), cell.y() + cell.height(),
                        Paint.stroke(state.gridColor(), 1.0f));
                if (state.focused() && cell.active()) {
                    draw.rect(cell.x(), cell.y(), cell.width(), cell.height(),
                            Paint.stroke(state.activeCellColor(), 1.0f));
                }
            }
            draw.line(row.x(), row.y() + row.height(), row.x() + row.width(), row.y() + row.height(),
                    Paint.stroke(state.gridColor(), 1.0f));
        }
    }

    private static void drawText(DrawScope draw,
                                 VirtualTableViewState state,
                                 boolean clipped,
                                 float clipX,
                                 float clipY,
                                 float clipWidth,
                                 float clipHeight,
                                 Iterable<VirtualTableViewTextSegment> segments) {
        if (clipped) {
            draw.pushClip(clipX, clipY, clipWidth, clipHeight);
        }
        try {
            for (VirtualTableViewTextSegment segment : segments) {
                if (segment.text() == null || segment.text().isEmpty()) continue;
                DrawScope segmentDraw = segment.transform() == null ? draw : draw.withTransform(segment.transform());
                segmentDraw.text(segment.text(), segment.x(), segment.y(), segment.width(), segment.height(),
                        Paint.fill(state.textColor()));
            }
        } finally {
            if (clipped) {
                draw.popClip();
            }
        }
    }
}
