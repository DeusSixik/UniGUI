package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Режим смешивания одного PostEffect pass'а.
 *
 * <p>В первом срезе режим используется backend-ом как подсказка для shader draw command.
 * Большинство post-processing pass'ов должны писать в пустой временный target через {@link #REPLACE}.</p>
 */
@WikiPage(
        title = "UiPostEffectBlendMode",
        slug = "ui-post-effect-blend-mode",
        category = "<key:ui.post.effect.blend.mode.category>",
        summary = "<key:ui.post.effect.blend.mode.summary>",
        order = 70
)
@WikiSection(
        title = "<key:ui.post.effect.blend.mode.section.overview.title>",
        markdown = "<key:ui.post.effect.blend.mode.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.post.effect.blend.mode.example.inspect.title>",
        code = "<key:ui.post.effect.blend.mode.example.inspect.code>",
        order = 20
)
@AiDoc(
        summary = "Enumerates framebuffer blending modes for post-processing passes (REPLACE, ALPHA_BLEND, ADDITIVE).",
        purpose = "Informs the graphics backend whether alpha blending or additive equation should be applied.",
        useWhen = {"Configuring blend modes on a UiPostEffectPass."},
        related = {"UiPostEffectPass"}
)
public enum UiPostEffectBlendMode {
    /** Pass заменяет содержимое target'а. Это дефолтный режим для ping-pong post-processing. */
    REPLACE(false),

    /** Pass смешивается обычным alpha blending. */
    ALPHA_BLEND(true),

    /** Additive режим зарезервирован для backend-ов, которые умеют отдельные blend equations. */
    ADDITIVE(true);

    private final boolean blendEnabled;

    UiPostEffectBlendMode(boolean blendEnabled) {
        this.blendEnabled = blendEnabled;
    }

    /** @return {@code true}, если backend должен включить blending для pass'а */
    @WikiMethod(
            title = "<key:ui.post.effect.blend.mode.method.blend_enabled.title>",
            summary = "<key:ui.post.effect.blend.mode.method.blend_enabled.summary>"
    )
    @WikiReturn("<key:ui.post.effect.blend.mode.method.blend_enabled.return>")
    public boolean blendEnabled() {
        return blendEnabled;
    }
}
