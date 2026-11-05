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
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Именованное описание UI PostEffect.
 *
 * <p>Definition является immutable-данными: стабильный id, упорядоченный список pass'ов и флаг
 * включения по умолчанию. Такой объект можно зарегистрировать в {@link UiPostEffectRegistry},
 * сослаться на него из Java/XML/StylePack и позже показать в редакторе как обычный набор свойств.</p>
 */
@WikiPage(
        title = "UiPostEffectDefinition",
        slug = "ui-post-effect-definition",
        category = "<key:ui.post.effect.definition.category>",
        summary = "<key:ui.post.effect.definition.summary>",
        order = 30
)
@WikiSection(
        title = "<key:ui.post.effect.definition.section.overview.title>",
        markdown = "<key:ui.post.effect.definition.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.post.effect.definition.example.builder.title>",
        code = "<key:ui.post.effect.definition.example.builder.code>",
        order = 20
)
@AiDoc(
        summary = "Immutable specification of a post-processing effect composed of ordered shader passes.",
        purpose = "Allows defining reusable multi-pass effects that can be registered in UiPostEffectRegistry.",
        useWhen = {"Authoring custom post-processing effects in code or StylePacks."},
        related = {"UiPostEffectPass", "UiPostEffectRegistry", "UiPostEffectChain"}
)
public final class UiPostEffectDefinition {
    private final String id;
    private final List<UiPostEffectPass> passes;
    private final boolean enabledByDefault;

    private UiPostEffectDefinition(String id, List<UiPostEffectPass> passes, boolean enabledByDefault) {
        this.id = normalizeId(id);
        if (passes == null || passes.isEmpty()) {
            throw new IllegalArgumentException("PostEffect definition must contain at least one pass");
        }
        ObjectArrayList<UiPostEffectPass> copy = new ObjectArrayList<>(passes.size());
        for (UiPostEffectPass pass : passes) {
            copy.add(Objects.requireNonNull(pass, "pass").copy());
        }
        this.passes = Collections.unmodifiableList(copy);
        this.enabledByDefault = enabledByDefault;
    }

    /** Начинает сборку definition. */
    @WikiMethod(
            title = "<key:ui.post.effect.definition.method.create.title>",
            summary = "<key:ui.post.effect.definition.method.create.summary>"
    )
    @WikiReturn("<key:ui.post.effect.definition.method.create.return>")
    public static Builder create(
            @WikiParameter(description = "<key:ui.post.effect.definition.method.create.id>") String id) {
        return new Builder(id);
    }

    /** Создаёт definition из одного pass'а с тем же id, что и эффект. */
    @WikiMethod(
            title = "<key:ui.post.effect.definition.method.single_pass.title>",
            summary = "<key:ui.post.effect.definition.method.single_pass.summary>"
    )
    @WikiReturn("<key:ui.post.effect.definition.method.single_pass.return>")
    public static UiPostEffectDefinition singlePass(
            @WikiParameter(description = "<key:ui.post.effect.definition.method.single_pass.id>") String id) {
        return create(id).pass(id).build();
    }

    /** @return id эффекта */
    @WikiMethod(
            title = "<key:ui.post.effect.definition.method.id.title>",
            summary = "<key:ui.post.effect.definition.method.id.summary>"
    )
    @WikiReturn("<key:ui.post.effect.definition.method.id.return>")
    public String id() {
        return id;
    }

    /** @return read-only список pass'ов */
    @WikiMethod(
            title = "<key:ui.post.effect.definition.method.passes.title>",
            summary = "<key:ui.post.effect.definition.method.passes.summary>"
    )
    @WikiReturn("<key:ui.post.effect.definition.method.passes.return>")
    public List<UiPostEffectPass> passes() {
        return passes;
    }

    /** @return {@code true}, если эффект можно включать дефолтно в UI/editor presets */
    @WikiMethod(
            title = "<key:ui.post.effect.definition.method.enabled_by_default.title>",
            summary = "<key:ui.post.effect.definition.method.enabled_by_default.summary>"
    )
    @WikiReturn("<key:ui.post.effect.definition.method.enabled_by_default.return>")
    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    /** @return независимая копия definition */
    @WikiMethod(
            title = "<key:ui.post.effect.definition.method.copy.title>",
            summary = "<key:ui.post.effect.definition.method.copy.summary>"
    )
    @WikiReturn("<key:ui.post.effect.definition.method.copy.return>")
    public UiPostEffectDefinition copy() {
        return new UiPostEffectDefinition(id, passes, enabledByDefault);
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("PostEffect id must not be empty");
        }
        return normalized;
    }

    /** Builder для программного создания PostEffect definition'ов. */
    @AiDoc(
            summary = "Fluent builder for constructing UiPostEffectDefinition instances.",
            purpose = "Appends shader passes with optional uniforms configuration.",
            useWhen = {"Constructing a UiPostEffectDefinition via UiPostEffectDefinition.create(id)."},
            related = {"UiPostEffectDefinition", "UiPostEffectPass"}
    )
    public static final class Builder {
        private final String id;
        private final ObjectArrayList<UiPostEffectPass> passes = new ObjectArrayList<>();
        private boolean enabledByDefault = true;

        private Builder(String id) {
            this.id = normalizeId(id);
        }

        /** @return этот builder после добавления pass'а по shader id */
        public Builder pass(String shaderId) {
            passes.add(UiPostEffectPass.shader(shaderId));
            return this;
        }

        /** @return этот builder после добавления pass'а с uniforms callback */
        public Builder pass(String shaderId, Consumer<UiPostEffectUniforms> uniforms) {
            passes.add(UiPostEffectPass.shader(shaderId).uniforms(uniforms));
            return this;
        }

        /** @return этот builder после добавления готового pass'а */
        public Builder pass(UiPostEffectPass pass) {
            passes.add(Objects.requireNonNull(pass, "pass").copy());
            return this;
        }

        /** @return этот builder после установки флага включения по умолчанию */
        public Builder enabledByDefault(boolean enabledByDefault) {
            this.enabledByDefault = enabledByDefault;
            return this;
        }

        /** @return immutable definition */
        public UiPostEffectDefinition build() {
            return new UiPostEffectDefinition(id, passes, enabledByDefault);
        }
    }
}
