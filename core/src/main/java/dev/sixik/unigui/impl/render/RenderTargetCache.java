package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.ManagedRenderTarget;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;

import java.util.Objects;

public final class RenderTargetCache implements AutoCloseable {
    private final RenderBackend backend;
    private RenderTarget target;
    private RenderTargetOptions options;

    public RenderTargetCache(RenderBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public RenderTarget acquire(int width, int height) {
        return acquire(width, height, RenderTargetOptions.COLOR);
    }

    public RenderTarget acquire(int width, int height, RenderTargetOptions options) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        RenderTargetOptions safeOptions = options == null ? RenderTargetOptions.COLOR : options;

        if (target == null || target.width() != safeWidth || target.height() != safeHeight || !safeOptions.equals(this.options)) {
            closeTarget();
            target = backend.createRenderTarget(safeWidth, safeHeight, safeOptions);
            this.options = safeOptions;
        }

        return target;
    }

    public RenderTarget current() {
        return target;
    }

    @Override
    public void close() {
        closeTarget();
    }

    private void closeTarget() {
        if (target instanceof ManagedRenderTarget managed && !managed.isDisposed()) {
            managed.close();
        }
        target = null;
        options = null;
    }
}
