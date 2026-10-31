package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

/**
 * Ссылка на PostEffect pipeline для UI-слоя.
 *
 * <p>Chain может быть пустым, ссылаться на зарегистрированный effect id или хранить inline список
 * pass'ов. Backend разрешает pass'ы в момент рендера и при неизвестном id возвращает no-op fallback.</p>
 */
@WikiPage(
        title = "UiPostEffectChain",
        slug = "ui-post-effect-chain",
        category = "<key:ui.post.effect.chain.category>",
        summary = "<key:ui.post.effect.chain.summary>",
        order = 20
)
@WikiSection(
        title = "<key:ui.post.effect.chain.section.overview.title>",
        markdown = "<key:ui.post.effect.chain.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.post.effect.chain.example.create.title>",
        code = "<key:ui.post.effect.chain.example.create.code>",
        order = 20
)
@AiDoc(
        summary = "Immutable pipeline reference encapsulating post-processing shader passes for a UI layer.",
        purpose = "Allows layers to reference named effects by ID or attach inline shader passes with lazy backend resolution.",
        useWhen = {"Attaching post-processing effects to UniRenderLayer or passing to UiPostEffectRenderer."},
        related = {"UiPostEffects", "UiPostEffectDefinition", "UniRenderLayer"}
)
public final class UiPostEffectChain {
    private static final UiPostEffectChain NONE = new UiPostEffectChain(null, List.of());

    private final String effectId;
    private final List<UiPostEffectPass> passes;

    private UiPostEffectChain(String effectId, List<UiPostEffectPass> passes) {
        this.effectId = normalizeOptionalId(effectId);
        this.passes = copyPasses(passes);
    }

    /** @return chain без post-processing */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.none.title>",
            summary = "<key:ui.post.effect.chain.method.none.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.none.return>")
    public static UiPostEffectChain none() {
        return NONE;
    }

    /** Создаёт chain как ссылку на зарегистрированный effect id. */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.of_id.title>",
            summary = "<key:ui.post.effect.chain.method.of_id.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.of_id.return>")
    public static UiPostEffectChain of(
            @WikiParameter(description = "<key:ui.post.effect.chain.method.of_id.effect_id>") String effectId) {
        String normalized = normalizeOptionalId(effectId);
        return normalized == null ? none() : new UiPostEffectChain(normalized, List.of());
    }

    /** Создаёт chain из definition. */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.of_definition.title>",
            summary = "<key:ui.post.effect.chain.method.of_definition.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.of_definition.return>")
    public static UiPostEffectChain of(
            @WikiParameter(description = "<key:ui.post.effect.chain.method.of_definition.definition>") UiPostEffectDefinition definition) {
        return definition == null ? none() : new UiPostEffectChain(null, definition.passes());
    }

    /** Создаёт inline chain из pass'ов. */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.of_passes.title>",
            summary = "<key:ui.post.effect.chain.method.of_passes.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.of_passes.return>")
    public static UiPostEffectChain of(
            @WikiParameter(description = "<key:ui.post.effect.chain.method.of_passes.passes>") List<UiPostEffectPass> passes) {
        return passes == null || passes.isEmpty() ? none() : new UiPostEffectChain(null, passes);
    }

    /** @return {@code true}, если chain не содержит post-processing */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.is_none.title>",
            summary = "<key:ui.post.effect.chain.method.is_none.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.is_none.return>")
    public boolean isNone() {
        return effectId == null && passes.isEmpty();
    }

    /** @return effect id или {@code null}, если chain inline/none */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.effect_id.title>",
            summary = "<key:ui.post.effect.chain.method.effect_id.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.effect_id.return>")
    public String effectId() {
        return effectId;
    }

    /** @return inline pass'ы, если chain создан не через id */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.inline_passes.title>",
            summary = "<key:ui.post.effect.chain.method.inline_passes.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.inline_passes.return>")
    public List<UiPostEffectPass> inlinePasses() {
        return passes;
    }

    /** Разрешает chain в список pass'ов через указанный registry. */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.resolve.title>",
            summary = "<key:ui.post.effect.chain.method.resolve.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.resolve.return>")
    public List<UiPostEffectPass> resolve(
            @WikiParameter(description = "<key:ui.post.effect.chain.method.resolve.registry>") UiPostEffectRegistry registry) {
        if (!passes.isEmpty()) return passes;
        if (effectId == null) return List.of();
        UiPostEffectRegistry source = registry == null ? UiPostEffectRegistry.global() : registry;
        return source.find(effectId).map(UiPostEffectDefinition::passes).orElse(List.of());
    }

    /** @return независимая копия chain */
    @WikiMethod(
            title = "<key:ui.post.effect.chain.method.copy.title>",
            summary = "<key:ui.post.effect.chain.method.copy.summary>"
    )
    @WikiReturn("<key:ui.post.effect.chain.method.copy.return>")
    public UiPostEffectChain copy() {
        if (this == NONE) return NONE;
        return new UiPostEffectChain(effectId, passes);
    }

    private static List<UiPostEffectPass> copyPasses(List<UiPostEffectPass> passes) {
        if (passes == null || passes.isEmpty()) return List.of();
        ObjectArrayList<UiPostEffectPass> copy = new ObjectArrayList<>(passes.size());
        for (UiPostEffectPass pass : passes) {
            if (pass != null) {
                copy.add(pass.copy());
            }
        }
        return copy.isEmpty() ? List.of() : Collections.unmodifiableList(copy);
    }

    private static String normalizeOptionalId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() || "none".equalsIgnoreCase(normalized) ? null : normalized;
    }
}
