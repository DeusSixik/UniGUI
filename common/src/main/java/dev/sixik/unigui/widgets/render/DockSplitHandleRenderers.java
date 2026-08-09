package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.DockSplitOrientation;

public final class DockSplitHandleRenderers {
    // DEFAULT renderer (block with a centre line)
    private static final MutableColor TRACK        = new MutableColor(0.055f, 0.062f, 0.080f, 0.98f);
    private static final MutableColor TRACK_HOVER  = new MutableColor(0.10f,  0.13f,  0.18f,  0.98f);
    private static final MutableColor TRACK_PRESS  = new MutableColor(0.14f,  0.18f,  0.26f,  0.98f);
    private static final MutableColor LINE         = new MutableColor(0.25f,  0.78f,  1.0f,   0.42f);
    private static final MutableColor LINE_HOVER   = new MutableColor(0.35f,  0.88f,  1.0f,   0.75f);
    private static final MutableColor LINE_PRESS   = new MutableColor(0.45f,  0.95f,  1.0f,   1.00f);

    public static final DockSplitHandleRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;
        MutableColor track = state.pressed() ? TRACK_PRESS : state.hovered() ? TRACK_HOVER : TRACK;
        MutableColor line  = state.pressed() ? LINE_PRESS  : state.hovered() ? LINE_HOVER  : LINE;
        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.fill(track));
        if (state.orientation() == DockSplitOrientation.HORIZONTAL) {
            float cx = state.x() + state.width() * 0.5f;
            draw.line(cx, state.y() + 4.0f, cx, state.y() + Math.max(4.0f, state.height() - 4.0f),
                    Paint.stroke(line, state.pressed() ? 2.0f : 1.0f));
        } else {
            float cy = state.y() + state.height() * 0.5f;
            draw.line(state.x() + 4.0f, cy, state.x() + Math.max(4.0f, state.width() - 4.0f), cy,
                    Paint.stroke(line, state.pressed() ? 2.0f : 1.0f));
        }
    };

    // IMGUI_STYLE renderer
    // Panels touch each other — the handle is just a shared border line.
    // No background fill; a thin seam line is drawn at the centre of the gap.
    // On hover a subtle highlight band appears; on drag the line turns bright.
    private static final MutableColor IM_SEAM        = new MutableColor(0.22f, 0.26f, 0.36f, 0.70f);
    private static final MutableColor IM_SEAM_HOVER  = new MutableColor(0.35f, 0.88f, 1.0f,  0.90f);
    private static final MutableColor IM_SEAM_PRESS  = new MutableColor(0.50f, 0.98f, 1.0f,  1.00f);
    private static final MutableColor IM_GLOW        = new MutableColor(0.25f, 0.78f, 1.0f,  0.18f);
    private static final MutableColor IM_GLOW_PRESS  = new MutableColor(0.30f, 0.85f, 1.0f,  0.30f);

    /**
     * ImGui-style split handle: panels share a common border line — no
     * separate background block, just a 1 px seam at the split edge.
     * <p>
     * On hover a translucent highlight band is drawn over the handle area.
     * During drag the seam turns into a bright 2 px accent line.
     */
    public static final DockSplitHandleRenderer IMGUI_STYLE = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;

        boolean h = state.orientation() == DockSplitOrientation.HORIZONTAL;

        // Hover / press: draw a subtle translucent band over the handle zone
        if (state.hovered() || state.pressed()) {
            MutableColor glow = state.pressed() ? IM_GLOW_PRESS : IM_GLOW;
            draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.fill(glow));
        }

        // Centre seam line
        MutableColor seam = state.pressed() ? IM_SEAM_PRESS : state.hovered() ? IM_SEAM_HOVER : IM_SEAM;
        float strokeW = state.pressed() ? 2.0f : 1.0f;
        if (h) {
            float cx = state.x() + state.width() * 0.5f;
            draw.line(cx, state.y(), cx, state.y() + state.height(), Paint.stroke(seam, strokeW));
        } else {
            float cy = state.y() + state.height() * 0.5f;
            draw.line(state.x(), cy, state.x() + state.width(), cy, Paint.stroke(seam, strokeW));
        }
    };

    private DockSplitHandleRenderers() {
    }
}
