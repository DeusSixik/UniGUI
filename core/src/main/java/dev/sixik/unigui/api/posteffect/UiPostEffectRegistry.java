package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Registry именованных UI PostEffect definition'ов.
 *
 * <p>Registry хранит только backend-neutral данные. Backend сам решает, как загрузить shader source,
 * создать временные targets и выполнить pass'ы. Global registry используется Java API, XML/XAML и
 * будущим StylePack resolver'ом как общее место для стандартных и пользовательских эффектов.</p>
 */
@WikiPage(
        title = "UiPostEffectRegistry",
        slug = "ui-post-effect-registry",
        category = "<key:ui.post.effect.registry.category>",
        summary = "<key:ui.post.effect.registry.summary>",
        order = 60
)
@WikiSection(
        title = "<key:ui.post.effect.registry.section.overview.title>",
        markdown = "<key:ui.post.effect.registry.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.post.effect.registry.example.register.title>",
        code = "<key:ui.post.effect.registry.example.register.code>",
        order = 20
)
@AiDoc(
        summary = "Registry repository storing named post-effect definitions accessible by string identifier.",
        purpose = "Acts as a central repository for built-in, modded, and StylePack post-processing effects.",
        useWhen = {"Registering custom effects or resolving effect pipelines by name."},
        related = {"UiPostEffectDefinition", "UiPostEffects", "UiPostEffectChain"}
)
public final class UiPostEffectRegistry {
    private static final UiPostEffectRegistry GLOBAL = new UiPostEffectRegistry();

    private final LinkedHashMap<String, UiPostEffectDefinition> definitions = new LinkedHashMap<>();

    /** @return глобальный registry эффектов */
    @WikiMethod(
            title = "<key:ui.post.effect.registry.method.global.title>",
            summary = "<key:ui.post.effect.registry.method.global.summary>"
    )
    @WikiReturn("<key:ui.post.effect.registry.method.global.return>")
    public static UiPostEffectRegistry global() {
        return GLOBAL;
    }

    /** Регистрирует definition по его id. Повторная регистрация id заменяет старую запись. */
    @WikiMethod(
            title = "<key:ui.post.effect.registry.method.register.title>",
            summary = "<key:ui.post.effect.registry.method.register.summary>"
    )
    @WikiReturn("<key:ui.post.effect.registry.method.register.return>")
    public synchronized UiPostEffectRegistry register(
            @WikiParameter(description = "<key:ui.post.effect.registry.method.register.definition>") UiPostEffectDefinition definition) {
        if (definition == null) return this;
        definitions.put(definition.id(), definition.copy());
        return this;
    }

    /** Удаляет эффект из registry. */
    @WikiMethod(
            title = "<key:ui.post.effect.registry.method.unregister.title>",
            summary = "<key:ui.post.effect.registry.method.unregister.summary>"
    )
    @WikiReturn("<key:ui.post.effect.registry.method.unregister.return>")
    public synchronized boolean unregister(
            @WikiParameter(description = "<key:ui.post.effect.registry.method.unregister.id>") String id) {
        String normalized = normalizeId(id);
        return normalized != null && definitions.remove(normalized) != null;
    }

    /** Ищет definition по id. */
    @WikiMethod(
            title = "<key:ui.post.effect.registry.method.find.title>",
            summary = "<key:ui.post.effect.registry.method.find.summary>"
    )
    @WikiReturn("<key:ui.post.effect.registry.method.find.return>")
    public synchronized Optional<UiPostEffectDefinition> find(
            @WikiParameter(description = "<key:ui.post.effect.registry.method.find.id>") String id) {
        String normalized = normalizeId(id);
        if (normalized == null) return Optional.empty();
        UiPostEffectDefinition definition = definitions.get(normalized);
        return definition == null ? Optional.empty() : Optional.of(definition.copy());
    }

    /** @return snapshot всех зарегистрированных definition'ов */
    @WikiMethod(
            title = "<key:ui.post.effect.registry.method.definitions.title>",
            summary = "<key:ui.post.effect.registry.method.definitions.summary>"
    )
    @WikiReturn("<key:ui.post.effect.registry.method.definitions.return>")
    public synchronized List<UiPostEffectDefinition> definitions() {
        return definitions.values().stream()
                .map(UiPostEffectDefinition::copy)
                .toList();
    }

    /** @return snapshot id зарегистрированных эффектов */
    @WikiMethod(
            title = "<key:ui.post.effect.registry.method.ids.title>",
            summary = "<key:ui.post.effect.registry.method.ids.summary>"
    )
    @WikiReturn("<key:ui.post.effect.registry.method.ids.return>")
    public synchronized List<String> ids() {
        return List.copyOf(definitions.keySet());
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
