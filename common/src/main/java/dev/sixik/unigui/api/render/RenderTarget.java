package dev.sixik.unigui.api.render;

public interface RenderTarget {
    int width();

    int height();

    TextureHandle colorTexture();
}
