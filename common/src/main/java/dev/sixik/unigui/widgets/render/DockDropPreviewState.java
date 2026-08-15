package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.widgets.docking.DockArea;

public record DockDropPreviewState(
        boolean visible,
        String sourcePaneId,
        String targetPaneId,
        DockArea area,
        float x,
        float y,
        float width,
        float height
) {
    public DockDropPreviewState {
        sourcePaneId = sourcePaneId == null ? "" : sourcePaneId;
        targetPaneId = targetPaneId == null ? "" : targetPaneId;
        area = area == null ? DockArea.CENTER : area;
    }
}
