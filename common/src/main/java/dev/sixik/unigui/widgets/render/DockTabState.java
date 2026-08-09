package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.widgets.DockPaneKind;

public record DockTabState(
        String paneId,
        RichText title,
        float x,
        float y,
        float width,
        float height,
        boolean selected,
        boolean closable,
        DockPaneKind kind,
        boolean dirty,
        boolean pinned,
        boolean autoHide,
        boolean active,
        boolean hovered,
        boolean pressed,
        boolean dragging
) {
    public DockTabState {
        paneId = paneId == null ? "" : paneId;
        title = title == null ? RichText.plain("") : title;
        kind = kind == null ? DockPaneKind.TOOL : kind;
    }
}
