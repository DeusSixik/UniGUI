package dev.sixik.unigui.api.render;

public interface ManagedRenderTarget extends RenderTarget, AutoCloseable {
    void resize(int width, int height);

    boolean isDisposed();

    @Override
    void close();
}
