package dev.sixik.unigui.api.core;

/**
 * Immutable snapshot параметров текущего UI-кадра.
 *
 * <p>Контекст передаётся в {@code tick(...)} и системные шаги кадра, чтобы виджеты и сервисы могли
 * одинаково интерпретировать время. Он не хранит ссылку на render backend и не управляет очередями:
 * это только данные кадра.</p>
 *
 * @see FramePhase
 * @see dev.sixik.unigui.api.widget.Widget#tick(FrameContext)
 */
public final class FrameContext {
    private final long frameIndex;
    private final float deltaSeconds;
    private final float partialTick;
    private final FramePhase phase;

    /**
     * Создаёт контекст кадра.
     *
     * @param frameIndex монотонный номер UI-кадра
     * @param deltaSeconds время с прошлого кадра в секундах
     * @param partialTick дробная часть Minecraft/backend tick'а, если она доступна
     * @param phase текущая фаза кадра
     */
    public FrameContext(long frameIndex, float deltaSeconds, float partialTick, FramePhase phase) {
        this.frameIndex = frameIndex;
        this.deltaSeconds = deltaSeconds;
        this.partialTick = partialTick;
        this.phase = phase;
    }

    /**
     * @return монотонный номер UI-кадра
     */
    public long frameIndex() {
        return frameIndex;
    }

    /**
     * @return время с прошлого кадра в секундах
     */
    public float deltaSeconds() {
        return deltaSeconds;
    }

    /**
     * @return дробная часть backend tick'а для плавной интерполяции
     */
    public float partialTick() {
        return partialTick;
    }

    /**
     * @return фаза UI pipeline, в которой создан контекст
     */
    public FramePhase phase() {
        return phase;
    }
}