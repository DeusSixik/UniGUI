package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;

/**
 * Дополнительная capability для backend-ов, умеющих выполнять UI PostEffect chain.
 *
 * <p>Интерфейс отделён от {@link RenderBackend}, чтобы старые/простые backend-ы могли оставаться
 * только с прямым render path. Вызов через {@link UiPostEffectRenderer} автоматически откатится к
 * обычному {@link RenderBackend#render(DrawList, RenderTarget)}, если backend не поддерживает эффекты.</p>
 */
public interface UiPostEffectBackend {
    /** @return {@code true}, если backend может выполнить post-processing pass'ы */
    default boolean supportsPostEffects() {
        return false;
    }

    /** Рендерит draw list в экран/активный target с применением post effect. */
    default void renderWithPostEffect(DrawList drawList, UiLayerBounds bounds, UiPostEffectChain chain) {
        renderWithPostEffect(drawList, bounds, chain, null);
    }

    /** Рендерит draw list в указанный target с применением post effect. */
    default void renderWithPostEffect(DrawList drawList, UiLayerBounds bounds, UiPostEffectChain chain, RenderTarget target) {
        if (this instanceof RenderBackend backend) {
            backend.render(drawList, target);
            return;
        }
        throw new UnsupportedOperationException("Post effects are not supported by this backend");
    }

}
