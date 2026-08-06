package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.math.RectView;

interface OverlayHostAware {
    void arrangeInHost(RectView hostBounds);
}
