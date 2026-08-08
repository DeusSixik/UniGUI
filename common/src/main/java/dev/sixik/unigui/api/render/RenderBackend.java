package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;

public interface RenderBackend {
    default RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        throw new UnsupportedOperationException("Render target creation is not supported by this backend");
    }

    default float measureTextWidth(String text) {
        return TextEngine.measureLineWidth(text);
    }

    default float measureTextWidth(RichText text) {
        return TextEngine.measureLineWidth(text);
    }

    default FontFace defaultTextFace() {
        return Fonts.defaultFace();
    }

    void beginFrame(FrameContext frame);

    void render(DrawList drawList, RenderTarget target);

    void endFrame();
}
