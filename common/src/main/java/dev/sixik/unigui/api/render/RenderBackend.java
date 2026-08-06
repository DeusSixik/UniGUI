package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.impl.text.TextEngine;

public interface RenderBackend {
    default RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        throw new UnsupportedOperationException("Render target creation is not supported by this backend");
    }

    default float measureTextWidth(String text) {
        return TextEngine.measureLineWidth(text);
    }

    void beginFrame(FrameContext frame);

    void render(DrawList drawList, RenderTarget target);

    void endFrame();
}
