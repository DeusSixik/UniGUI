package dev.sixik.unigui.api.render;

/**
 * Цель рендера, в которую backend может вывести draw list.
 *
 * <p>Render target может быть экраном, offscreen framebuffer'ом или backend-specific texture target.
 * Публичный контракт отдаёт только размер и color texture, чтобы результат можно было использовать
 * дальше как обычную {@link TextureHandle}.</p>
 */
public interface RenderTarget {
    /** @return ширина target'а в backend pixels */
    int width();

    /** @return высота target'а в backend pixels */
    int height();

    /** @return color texture target'а */
    TextureHandle colorTexture();
}