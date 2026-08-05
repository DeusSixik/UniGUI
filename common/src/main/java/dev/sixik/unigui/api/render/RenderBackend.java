package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.core.FrameContext;

public interface RenderBackend {
    default RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        throw new UnsupportedOperationException("Render target creation is not supported by this backend");
    }

    void beginFrame(FrameContext frame);

    void render(DrawList drawList, RenderTarget target);

    void endFrame();
}
