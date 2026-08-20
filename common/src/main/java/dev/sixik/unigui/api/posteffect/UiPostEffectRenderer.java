package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;

import java.util.Objects;

/**
 * Helper для рендера слоя с optional PostEffect fallback.
 *
 * <p>Код, которому не хочется знать конкретный backend, может всегда вызывать этот класс. Если chain
 * пустой или backend не поддерживает post-processing, слой будет отрисован обычным способом.</p>
 */
public final class UiPostEffectRenderer {
    private UiPostEffectRenderer() {
    }

    /** Рендерит слой в экран/активный target. */
    public static void render(RenderBackend backend, DrawList drawList, UiLayerBounds bounds, UiPostEffectChain chain) {
        render(backend, drawList, bounds, chain, null);
    }

    /** Рендерит слой в указанный target или экран. */
    public static void render(RenderBackend backend, DrawList drawList, UiLayerBounds bounds,
                              UiPostEffectChain chain, RenderTarget target) {
        Objects.requireNonNull(backend, "backend");
        if (chain == null || chain.isNone() || !(backend instanceof UiPostEffectBackend postBackend)
                || !postBackend.supportsPostEffects()) {
            backend.render(drawList, target);
            return;
        }
        postBackend.renderWithPostEffect(drawList, bounds, chain, target);
    }
}