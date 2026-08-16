package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

/**
 * Преобразует XML texture id в runtime {@link TextureHandle}.
 *
 * <p>Resolver отделяет XML от конкретного render backend-а. Один и тот же XML
 * может использовать simple in-memory handles в тестах, Minecraft handles в игре
 * или editor-preview handles в инструментах.</p>
 */
@FunctionalInterface
public interface XmlTextureResolver {
    /**
     * Разрешает texture id в handle.
     *
     * @param id строковый id из XML-атрибута
     * @param width желаемая ширина source texture
     * @param height желаемая высота source texture
     * @param options параметры sampling/wrap/mipmaps; может быть {@code null}
     * @return runtime texture handle
     */
    TextureHandle resolve(String id, int width, int height, TextureOptions options);

    /**
     * Создаёт resolver, который возвращает {@link SimpleTextureHandle} без backend native handle.
     *
     * <p>Такой resolver удобен для self-test'ов, headless editor logic и документации.</p>
     *
     * @return simple resolver без внешних зависимостей
     */
    static XmlTextureResolver simple() {
        return (id, width, height, options) -> new SimpleTextureHandle(id, width, height, null, options);
    }
}
