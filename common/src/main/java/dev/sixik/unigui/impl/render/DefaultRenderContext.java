package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;

public final class DefaultRenderContext implements RenderContext {
    private final DrawList drawList;
    private RenderBackend backend;

    public DefaultRenderContext(DrawList drawList) {
        this.drawList = drawList;
    }

    public DefaultRenderContext backend(RenderBackend backend) {
        this.backend = backend;
        return this;
    }

    @Override
    public DrawList drawList() {
        return drawList;
    }

    @Override
    public RenderBackend backend() {
        return backend;
    }
}
