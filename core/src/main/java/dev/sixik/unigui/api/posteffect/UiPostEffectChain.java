package dev.sixik.unigui.api.posteffect;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

/**
 * Ссылка на PostEffect pipeline для UI-слоя.
 *
 * <p>Chain может быть пустым, ссылаться на зарегистрированный effect id или хранить inline список
 * pass'ов. Backend разрешает pass'ы в момент рендера и при неизвестном id возвращает no-op fallback.</p>
 */
public final class UiPostEffectChain {
    private static final UiPostEffectChain NONE = new UiPostEffectChain(null, List.of());

    private final String effectId;
    private final List<UiPostEffectPass> passes;

    private UiPostEffectChain(String effectId, List<UiPostEffectPass> passes) {
        this.effectId = normalizeOptionalId(effectId);
        this.passes = copyPasses(passes);
    }

    /** @return chain без post-processing */
    public static UiPostEffectChain none() {
        return NONE;
    }

    /** Создаёт chain как ссылку на зарегистрированный effect id. */
    public static UiPostEffectChain of(String effectId) {
        String normalized = normalizeOptionalId(effectId);
        return normalized == null ? none() : new UiPostEffectChain(normalized, List.of());
    }

    /** Создаёт chain из definition. */
    public static UiPostEffectChain of(UiPostEffectDefinition definition) {
        return definition == null ? none() : new UiPostEffectChain(null, definition.passes());
    }

    /** Создаёт inline chain из pass'ов. */
    public static UiPostEffectChain of(List<UiPostEffectPass> passes) {
        return passes == null || passes.isEmpty() ? none() : new UiPostEffectChain(null, passes);
    }

    /** @return {@code true}, если chain не содержит post-processing */
    public boolean isNone() {
        return effectId == null && passes.isEmpty();
    }

    /** @return effect id или {@code null}, если chain inline/none */
    public String effectId() {
        return effectId;
    }

    /** @return inline pass'ы, если chain создан не через id */
    public List<UiPostEffectPass> inlinePasses() {
        return passes;
    }

    /** Разрешает chain в список pass'ов через указанный registry. */
    public List<UiPostEffectPass> resolve(UiPostEffectRegistry registry) {
        if (!passes.isEmpty()) return passes;
        if (effectId == null) return List.of();
        UiPostEffectRegistry source = registry == null ? UiPostEffectRegistry.global() : registry;
        return source.find(effectId).map(UiPostEffectDefinition::passes).orElse(List.of());
    }

    /** @return независимая копия chain */
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