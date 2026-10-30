package dev.sixik.unigui.api.animation;

/** Источник времени для animation engine. */
@FunctionalInterface
public interface AnimationClock {
    /** Максимальный delta за один шаг. */
    float DEFAULT_MAX_DELTA_SECONDS = 0.25f;
    /** @return delta времени в секундах */
    float deltaSeconds();
    /** Нормализует внешний delta. */
    static float sanitizeDelta(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds <= 0.0f) return 0.0f;
        return Math.min(deltaSeconds, DEFAULT_MAX_DELTA_SECONDS);
    }
    /** Создаёт clock с постоянным шагом. */
    static AnimationClock fixed(float deltaSeconds) {
        final float delta = sanitizeDelta(deltaSeconds);
        return () -> delta;
    }
}
