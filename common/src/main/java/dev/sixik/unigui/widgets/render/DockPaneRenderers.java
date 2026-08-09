package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.DockPaneKind;

public final class DockPaneRenderers {
    private static final MutableColor PANE_BORDER = new MutableColor(0.24f, 0.28f, 0.36f, 0.95f);
    private static final MutableColor TAB_BACKGROUND = new MutableColor(0.075f, 0.085f, 0.115f, 0.96f);
    private static final MutableColor TAB_SELECTED = new MutableColor(0.12f, 0.16f, 0.23f, 0.98f);
    private static final MutableColor TAB_DOCUMENT = new MutableColor(0.10f, 0.14f, 0.21f, 0.98f);
    private static final MutableColor TAB_TOOL = new MutableColor(0.085f, 0.095f, 0.120f, 0.98f);
    private static final MutableColor TAB_BORDER = new MutableColor(0.26f, 0.32f, 0.42f, 0.95f);
    private static final MutableColor TAB_TEXT = new MutableColor(0.88f, 0.93f, 1.0f, 1.0f);
    private static final MutableColor TAB_TEXT_MUTED = new MutableColor(0.62f, 0.68f, 0.78f, 0.88f);
    private static final MutableColor TAB_DIRTY = new MutableColor(1.0f, 0.66f, 0.24f, 1.0f);
    private static final MutableColor OVERFLOW = new MutableColor(0.28f, 0.78f, 1.0f, 0.85f);
    private static final MutableColor TAB_HOVER = new MutableColor(0.18f, 0.24f, 0.34f, 0.96f);
    private static final MutableColor TAB_PRESSED = new MutableColor(0.08f, 0.11f, 0.17f, 0.98f);
    private static final MutableColor TAB_DRAGGING = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);

    public static final DockPaneRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;
        float tabHeight = Math.min(state.tabHeight(), state.height());
        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.stroke(PANE_BORDER, 1.0f));
        draw.rect(state.x(), state.y(), state.width(), tabHeight, Paint.fill(TAB_BACKGROUND));

        for (DockTabState tab : state.tabs()) {
            if (tab.width() <= 0.0f || tab.height() <= 0.0f) continue;
            MutableColor base = tab.pressed()
                    ? TAB_PRESSED
                    : tab.hovered()
                    ? TAB_HOVER
                    : tab.selected()
                    ? TAB_SELECTED
                    : tab.kind() == DockPaneKind.DOCUMENT ? TAB_DOCUMENT : TAB_TOOL;
            draw.roundedRect(tab.x(), tab.y(), tab.width(), tab.height() + 2.0f, 3.0f,
                    Paint.fill(base));
            draw.line(tab.x(), tab.y() + tab.height(),
                    tab.x() + tab.width(), tab.y() + tab.height(),
                    Paint.stroke(tab.dragging() ? TAB_DRAGGING : tab.active() ? TAB_TEXT : TAB_BORDER,
                            tab.dragging() ? 2.0f : tab.active() ? 1.5f : 1.0f));
            if (tab.dirty()) {
                draw.circle(tab.x() + Math.max(7.0f, tab.width() - 12.0f),
                        tab.y() + Math.max(6.0f, tab.height() * 0.5f - 2.0f),
                        4.0f, 4.0f, Paint.fill(TAB_DIRTY));
            }
            if (tab.autoHide()) {
                draw.line(tab.x() + 4.0f, tab.y() + 3.0f,
                        tab.x() + 4.0f, tab.y() + Math.max(4.0f, tab.height() - 4.0f),
                        Paint.stroke(OVERFLOW, 1.0f));
            }
            draw.pushClip(tab.x() + 6.0f, tab.y(), Math.max(0.0f, tab.width() - 12.0f), tab.height());
            try {
                draw.text(tab.title(), tab.x() + 6.0f, tab.y() + 4.0f,
                        Math.max(0.0f, tab.width() - 12.0f), Math.max(0.0f, tab.height() - 6.0f),
                        Paint.fill(tab.selected() ? TAB_TEXT : TAB_TEXT_MUTED));
            } finally {
                draw.popClip();
            }
        }
        if (state.overflow()) {
            float bx = state.overflowButtonX();
            float by = state.overflowButtonY();
            float bw = state.overflowButtonWidth();
            float bh = state.overflowButtonHeight();
            draw.roundedRect(bx + 2.0f, by + 2.0f, Math.max(0.0f, bw - 4.0f), Math.max(0.0f, bh - 4.0f), 3.0f,
                    Paint.fill(state.overflowMenuOpen() ? TAB_HOVER : TAB_BACKGROUND));
            draw.rect(bx + 2.0f, by + 2.0f, Math.max(0.0f, bw - 4.0f), Math.max(0.0f, bh - 4.0f),
                    Paint.stroke(OVERFLOW, 1.0f));
            float cx = bx + bw * 0.5f;
            float cy = by + bh * 0.5f;
            draw.circle(cx - 5.0f, cy, 1.6f, 1.6f, Paint.fill(OVERFLOW));
            draw.circle(cx, cy, 1.6f, 1.6f, Paint.fill(OVERFLOW));
            draw.circle(cx + 5.0f, cy, 1.6f, 1.6f, Paint.fill(OVERFLOW));
        }
    };

    private DockPaneRenderers() {
    }
}
