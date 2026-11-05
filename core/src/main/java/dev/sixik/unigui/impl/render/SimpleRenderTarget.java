package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.ManagedRenderTarget;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;

import java.util.concurrent.atomic.AtomicInteger;

public final class SimpleRenderTarget implements ManagedRenderTarget {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final int id = NEXT_ID.incrementAndGet();
    private int width;
    private int height;
    private boolean disposed;
    private TextureHandle colorTexture;

    public SimpleRenderTarget(int width, int height) {
        resize(width, height);
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public TextureHandle colorTexture() {
        return colorTexture;
    }

    @Override
    public void resize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.colorTexture = new SimpleTextureHandle("unigui:simple_render_target/" + id, this.width, this.height);
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        disposed = true;
    }
}
