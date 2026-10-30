package dev.sixik.unigui.api.render;

/**
 * Backend-neutral handle текстуры.
 *
 * <p>UniGUI хранит в draw-командах только этот контракт. Реальный OpenGL/Minecraft/native объект
 * может быть доступен через {@link #nativeHandle()}, но виджеты не должны зависеть от его типа.</p>
 */
public interface TextureHandle {
    /** @return стабильный id текстуры */
    String id();

    /** @return ширина текстуры в пикселях */
    int width();

    /** @return высота текстуры в пикселях */
    int height();

    /** @return sampling/wrap параметры текстуры */
    default TextureOptions options() {
        return TextureOptions.defaults();
    }

    /**
     * Возвращает backend-specific handle.
     *
     * @return native texture object или {@code null}
     */
    default Object nativeHandle() {
        return null;
    }
}