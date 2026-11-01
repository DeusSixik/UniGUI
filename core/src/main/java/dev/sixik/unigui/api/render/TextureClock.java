package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.function.LongSupplier;

/**
 * Источник «тиков» для анимированных текстур.
 *
 * <p>Один тик равен {@link #MILLIS_PER_TICK} миллисекундам, как тик Minecraft (20 тиков в секунду).
 * Core ничего не знает о конкретном игровом цикле, поэтому по умолчанию тики считаются по системным часам.
 * Backend может подставить настоящий счётчик клиентских тиков через {@link #source(LongSupplier)}:
 * тогда анимация встаёт на паузу вместе с игрой.</p>
 */
@AiDoc(
        summary = "Global tick source driving AnimatedTexture frame selection; defaults to wall-clock time at 20 ticks per second.",
        purpose = "Lets render backends substitute the real game tick counter while keeping core backend-neutral.",
        useWhen = {
                "Bind animated textures to the game tick counter (pauses with the game) from a backend.",
                "Make animation frame selection deterministic in tests by installing a fixed tick source."
        },
        criticalRules = {
                "The source must be cheap and monotonic: it is read once per animated texture draw command.",
                "`source(null)` restores the wall-clock default."
        },
        related = {"AnimatedTexture", "TextureHandle"})
public final class TextureClock {
    /**
     * Длительность одного тика в миллисекундах.
     */
    public static final long MILLIS_PER_TICK = 50L;

    private static final LongSupplier WALL_CLOCK = () -> System.nanoTime() / (MILLIS_PER_TICK * 1_000_000L);

    private static volatile LongSupplier source = WALL_CLOCK;

    private TextureClock() {
    }

    /**
     * @return текущее число тиков по активному источнику
     */
    public static long ticks() {
        return source.getAsLong();
    }

    /**
     * Подменяет источник тиков.
     *
     * @param ticks новый источник; {@code null} возвращает системные часы
     */
    public static void source(LongSupplier ticks) {
        source = ticks == null ? WALL_CLOCK : ticks;
    }
}
