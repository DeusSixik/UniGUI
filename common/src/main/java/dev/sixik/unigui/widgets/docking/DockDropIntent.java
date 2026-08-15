package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;

public record DockDropIntent(
        boolean valid,
        String sourcePaneId,
        String targetPaneId,
        DockArea area,
        float x,
        float y,
        float width,
        float height,
        float pointerX,
        float pointerY
) {
    private static final DockDropIntent NONE = new DockDropIntent(
            false, "", "", DockArea.CENTER,
            0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f);

    public DockDropIntent {
        sourcePaneId = sourcePaneId == null ? "" : sourcePaneId;
        targetPaneId = targetPaneId == null ? "" : targetPaneId;
        area = area == null ? DockArea.CENTER : area;
        if (!valid) {
            x = 0.0f;
            y = 0.0f;
            width = 0.0f;
            height = 0.0f;
        }
    }

    public static DockDropIntent none() {
        return NONE;
    }

    public static DockDropIntent of(String sourcePaneId, String targetPaneId, DockArea area,
                                    RectView previewBounds, float pointerX, float pointerY) {
        RectView bounds = previewBounds == null ? new MutableRect() : previewBounds;
        return new DockDropIntent(true, sourcePaneId, targetPaneId, area,
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), pointerX, pointerY);
    }

    public static DockDropIntent floating(String sourcePaneId, float pointerX, float pointerY) {
        return new DockDropIntent(true, sourcePaneId, "", DockArea.FLOAT,
                pointerX - 80.0f, pointerY - 11.0f, 160.0f, 96.0f, pointerX, pointerY);
    }

    public boolean floating() {
        return valid && area == DockArea.FLOAT;
    }

    public boolean tabbed() {
        return valid && (area == DockArea.CENTER || area == DockArea.TAB);
    }

    public boolean split() {
        return valid && area != DockArea.CENTER && area != DockArea.TAB && area != DockArea.FLOAT;
    }
}
