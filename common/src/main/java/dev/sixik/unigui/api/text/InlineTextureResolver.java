package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.render.TextureHandle;

/**
 * Поставщик texture-handle'ов для marker-based inline icons.
 *
 * <p>Resolver получает строковый id и уже нормализованный размер в UI-пикселях. Это позволяет
 * подключить Minecraft assets, SDM shop assets или любой другой модовый источник без зависимости
 * {@code api.text} от конкретного backend'а.</p>
 *
 * @see InlineContentResolvers#textureMarkers(InlineTextureResolver)
 */
@FunctionalInterface
public interface InlineTextureResolver {
    /**
     * Возвращает texture-handle для inline marker'а.
     *
     * @param id id из marker'а без префикса {@code icon:} или {@code texture:}
     * @param width запрошенная ширина в UI-пикселях
     * @param height запрошенная высота в UI-пикселях
     * @return texture-handle или {@code null}, если marker нужно оставить обычным текстом
     */
    TextureHandle resolve(String id, int width, int height);
}
