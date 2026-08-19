package dev.sixik.unigui.api.render;

/**
 * Render target, жизненным циклом которого владеет UniGUI/runtime.
 *
 * <p>Managed target можно resize'ить и закрывать. Он используется для offscreen render, cached subtree,
 * blur/composite passes и других сценариев, где backend создаёт временный framebuffer.</p>
 */
public interface ManagedRenderTarget extends RenderTarget, AutoCloseable {
    /**
     * Меняет размер target'а.
     *
     * @param width новая ширина в backend pixels
     * @param height новая высота в backend pixels
     */
    void resize(int width, int height);

    /**
     * @return {@code true}, если target уже освобождён
     */
    boolean isDisposed();

    /** Освобождает backend resources target'а. */
    @Override
    void close();
}