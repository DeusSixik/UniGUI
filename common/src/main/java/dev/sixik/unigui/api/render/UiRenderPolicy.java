package dev.sixik.unigui.api.render;

/**
 * Политика частоты пересборки и рендера retained UI.
 *
 * <p>Policy позволяет runtime не перерисовывать UI каждый кадр, если экран статичен, или наоборот
 * держать непрерывный render для анимаций, shader effects и editor preview.</p>
 */
public final class UiRenderPolicy {
    /** Режим работы render policy. */
    public enum Mode {
        /** UI перестраивается и рендерится каждый кадр. */
        CONTINUOUS,
        /** UI рендерится только после invalidation/dirty signal. */
        ON_DIRTY,
        /** UI рендерится с фиксированной частотой. */
        FIXED_FPS,
        /** UI ориентируется на refresh rate окна/backend'а. */
        VSYNC
    }

    private static final UiRenderPolicy CONTINUOUS = new UiRenderPolicy(Mode.CONTINUOUS, 0.0f);
    private static final UiRenderPolicy ON_DIRTY = new UiRenderPolicy(Mode.ON_DIRTY, 0.0f);
    private static final UiRenderPolicy VSYNC = new UiRenderPolicy(Mode.VSYNC, 0.0f);

    private final Mode mode;
    private final float fps;

    private UiRenderPolicy(Mode mode, float fps) {
        this.mode = mode;
        this.fps = fps;
    }

    /** @return policy непрерывного рендера */
    public static UiRenderPolicy continuous() {
        return CONTINUOUS;
    }

    /** @return policy рендера только по dirty/invalidation signal */
    public static UiRenderPolicy onDirty() {
        return ON_DIRTY;
    }

    /**
     * Создаёт policy фиксированной частоты.
     *
     * @param fps частота кадров UI
     * @return policy с фиксированным fps
     */
    public static UiRenderPolicy fixedFps(float fps) {
        if (!Float.isFinite(fps) || fps <= 0.0f) {
            throw new IllegalArgumentException("fps must be finite and greater than zero");
        }
        return new UiRenderPolicy(Mode.FIXED_FPS, fps);
    }

    /**
     * Использует текущую частоту обновления Minecraft/window как интервал UI render.
     *
     * @return policy, синхронизированная с vsync
     */
    public static UiRenderPolicy vsync() {
        return VSYNC;
    }

    /** @return режим policy */
    public Mode mode() {
        return mode;
    }

    /** @return fps для {@link Mode#FIXED_FPS} или {@code 0.0f} */
    public float fps() {
        return fps;
    }

    /**
     * Возвращает интервал fixed-fps policy в наносекундах.
     *
     * @return интервал или {@code 0}, если режим не {@link Mode#FIXED_FPS}
     */
    public long intervalNanos() {
        return mode == Mode.FIXED_FPS
                ? Math.max(1L, Math.round(1_000_000_000.0 / fps))
                : 0L;
    }
}