package dev.sixik.unigui.api.interop;

import dev.sixik.unigui.api.layout.LayoutContext;

public final class ExternMeasureContext {
    private final LayoutContext layout;

    public ExternMeasureContext(LayoutContext layout) {
        this.layout = layout;
    }

    public LayoutContext layout() {
        return layout;
    }
}
