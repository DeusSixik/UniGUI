package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;

import java.util.function.Function;

/**
 * Кэш нативного объекта узла: строка или объект пользователя разрешаются платформой один раз.
 *
 * <p>Результат запоминается вместе с платформой. {@code null} от платформы означает «пока недоступно» и
 * не кэшируется. Ошибка {@link IllegalArgumentException} или {@link UnsupportedOperationException}
 * кэшируется и пишется в лог один раз, чтобы не повторяться каждый кадр.</p>
 */
final class ResolvedContent {
    /**
     * Лог ошибок разрешения.
     */
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(ResolvedContent.class);

    /**
     * Платформа, для которой сохранён результат, или null.
     */
    private ScenePlatform platform;
    /**
     * Разрешённый объект или null.
     */
    private Object value;
    /**
     * Запомненная ошибка или null.
     */
    private RuntimeException failure;

    /**
     * Возвращает объект, разрешая его при первом обращении для этой платформы.
     *
     * @param target   платформа или null.
     * @param resolver вызов платформы.
     * @param what     описание контента для лога.
     * @param rethrow  бросить ли запомненную ошибку вместо возврата null.
     * @return нативный объект или null, если он недоступен или неверен.
     */
    Object get(ScenePlatform target, Function<ScenePlatform, Object> resolver, Object what, boolean rethrow) {
        if (target == null) return null;
        if (platform == target) {
            if (failure != null && rethrow) throw failure;
            return value;
        }
        Object resolved;
        try {
            resolved = resolver.apply(target);
        } catch (IllegalArgumentException | UnsupportedOperationException error) {
            platform = target;
            value = null;
            failure = error;
            LOGGER.warn("Scene content '{}' cannot be shown: {}", what, error.getMessage());
            if (rethrow) throw error;
            return null;
        }
        if (resolved == null) return null;
        platform = target;
        value = resolved;
        failure = null;
        return resolved;
    }

    /**
     * Платформа последнего успешного или ошибочного разрешения.
     *
     * @return платформа или null.
     */
    ScenePlatform platform() {
        return platform;
    }

    /**
     * Забывает результат после смены источника.
     */
    void reset() {
        platform = null;
        value = null;
        failure = null;
    }
}
