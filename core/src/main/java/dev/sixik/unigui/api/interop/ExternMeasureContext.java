package dev.sixik.unigui.api.interop;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;

public final class ExternMeasureContext {
    private final LayoutContext layout;
    private LayoutSize desiredSize = LayoutSize.ZERO;

    public ExternMeasureContext(LayoutContext layout) {
        this.layout = layout;
    }

    public LayoutContext layout() {
        return layout;
    }

    public LayoutSize desiredSize() {
        return desiredSize;
    }

    public void desiredSize(float width, float height) {
        this.desiredSize = LayoutSize.of(width, height);
    }
}
