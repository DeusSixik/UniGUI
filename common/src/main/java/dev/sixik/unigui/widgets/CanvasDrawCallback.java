package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.render.RenderContext;

@FunctionalInterface
public interface CanvasDrawCallback {
    void draw(RenderContext context);
}
