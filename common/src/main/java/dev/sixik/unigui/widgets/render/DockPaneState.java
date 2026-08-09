package dev.sixik.unigui.widgets.render;

import java.util.List;

public record DockPaneState(
        float x,
        float y,
        float width,
        float height,
        float tabHeight,
        List<DockTabState> tabs,
        int selectedIndex,
        int firstVisibleTab,
        int lastVisibleTab,
        boolean overflow,
        float overflowButtonX,
        float overflowButtonY,
        float overflowButtonWidth,
        float overflowButtonHeight,
        boolean overflowMenuOpen
) {
    public DockPaneState {
        tabs = List.copyOf(tabs == null ? List.of() : tabs);
        firstVisibleTab = Math.max(0, firstVisibleTab);
        lastVisibleTab = Math.max(-1, lastVisibleTab);
        if (!overflow) {
            overflowButtonWidth = 0.0f;
            overflowButtonHeight = 0.0f;
            overflowMenuOpen = false;
        }
    }
}
