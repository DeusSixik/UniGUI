package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
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
@WikiPage(
        title = "UiPostEffectRenderer",
        slug = "ui-post-effect-renderer",
        category = "<key:ui.post.effect.renderer.category>",
        summary = "<key:ui.post.effect.renderer.summary>",
        order = 90
)
@WikiSection(
        title = "<key:ui.post.effect.renderer.section.overview.title>",
        markdown = "<key:ui.post.effect.renderer.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.post.effect.renderer.example.render.title>",
        code = "<key:ui.post.effect.renderer.example.render.code>",
        order = 20
)
@AiDoc(
        summary = "Rendering dispatcher executing post-processed draw lists with transparent fallback.",
        purpose = "Dispatches rendering to UiPostEffectBackend when available, falling back to direct render when unsupported or chain is empty.",
        useWhen = {"Rendering a DrawList subject to potential post-processing filters."},
        related = {"UiPostEffectBackend", "UiPostEffectChain", "UiLayerBounds"}
)
public final class UiPostEffectRenderer {
    private UiPostEffectRenderer() {
    }

    /** Рендерит слой в экран/активный target. */
    @WikiMethod(
            title = "<key:ui.post.effect.renderer.method.render_default.title>",
            summary = "<key:ui.post.effect.renderer.method.render_default.summary>"
    )
    public static void render(
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_default.backend>") RenderBackend backend,
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_default.draw_list>") DrawList drawList,
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_default.bounds>") UiLayerBounds bounds,
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_default.chain>") UiPostEffectChain chain) {
        render(backend, drawList, bounds, chain, null);
    }

    /** Рендерит слой в указанный target или экран. */
    @WikiMethod(
            title = "<key:ui.post.effect.renderer.method.render_target.title>",
            summary = "<key:ui.post.effect.renderer.method.render_target.summary>"
    )
    public static void render(
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_target.backend>") RenderBackend backend,
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_target.draw_list>") DrawList drawList,
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_target.bounds>") UiLayerBounds bounds,
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_target.chain>") UiPostEffectChain chain,
            @WikiParameter(description = "<key:ui.post.effect.renderer.method.render_target.target>") RenderTarget target) {
        Objects.requireNonNull(backend, "backend");
        if (chain == null || chain.isNone() || !(backend instanceof UiPostEffectBackend postBackend)
                || !postBackend.supportsPostEffects()) {
            backend.render(drawList, target);
            return;
        }
        postBackend.renderWithPostEffect(drawList, bounds, chain, target);
    }
}
