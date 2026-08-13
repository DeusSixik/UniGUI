package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderContext;

final class NoopRenderContext implements RenderContext {
    private final DrawList drawList = new DrawList();

    @Override
    public DrawList drawList() {
        return drawList;
    }
}
