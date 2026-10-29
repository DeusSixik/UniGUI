package dev.sixik.unigui.api.interop;

import dev.sixik.unigui.api.render.RenderContext;

public final class ExternRenderContext {
    private final RenderContext render;

    public ExternRenderContext(RenderContext render) {
        this.render = render;
    }

    public RenderContext render() {
        return render;
    }
}
