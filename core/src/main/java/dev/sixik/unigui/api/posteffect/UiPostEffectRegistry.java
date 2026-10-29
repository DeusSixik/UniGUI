package dev.sixik.unigui.api.posteffect;

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
public final class UiPostEffectRegistry {
    private static final UiPostEffectRegistry GLOBAL = new UiPostEffectRegistry();

    private final LinkedHashMap<String, UiPostEffectDefinition> definitions = new LinkedHashMap<>();

    /** @return глобальный registry эффектов */
    public static UiPostEffectRegistry global() {
        return GLOBAL;
    }

    /** Регистрирует definition по его id. Повторная регистрация id заменяет старую запись. */
    public synchronized UiPostEffectRegistry register(UiPostEffectDefinition definition) {
        if (definition == null) return this;
        definitions.put(definition.id(), definition.copy());
        return this;
    }

    /** Удаляет эффект из registry. */
    public synchronized boolean unregister(String id) {
        String normalized = normalizeId(id);
        return normalized != null && definitions.remove(normalized) != null;
    }

    /** Ищет definition по id. */
    public synchronized Optional<UiPostEffectDefinition> find(String id) {
        String normalized = normalizeId(id);
        if (normalized == null) return Optional.empty();
        UiPostEffectDefinition definition = definitions.get(normalized);
        return definition == null ? Optional.empty() : Optional.of(definition.copy());
    }

    /** @return snapshot всех зарегистрированных definition'ов */
    public synchronized List<UiPostEffectDefinition> definitions() {
        return definitions.values().stream()
                .map(UiPostEffectDefinition::copy)
                .toList();
    }

    /** @return snapshot id зарегистрированных эффектов */
    public synchronized List<String> ids() {
        return List.copyOf(definitions.keySet());
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}