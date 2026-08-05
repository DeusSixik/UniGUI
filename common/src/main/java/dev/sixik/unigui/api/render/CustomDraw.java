package dev.sixik.unigui.api.render;

@FunctionalInterface
public interface CustomDraw {
    void draw(RenderBackend backend);
}
