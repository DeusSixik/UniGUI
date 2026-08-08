package dev.sixik.unigui.api.widget.render;

import dev.sixik.unigui.api.render.DrawScope;

@FunctionalInterface
public interface WidgetRenderer<S> {
    void render(DrawScope draw, S state);
}
