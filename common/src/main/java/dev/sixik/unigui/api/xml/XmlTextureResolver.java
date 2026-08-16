package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

/** Преобразует XML id текстуры в runtime handle текстуры, не привязывая XML-загрузку к backend-у. */
@FunctionalInterface
public interface XmlTextureResolver {
    TextureHandle resolve(String id, int width, int height, TextureOptions options);

    static XmlTextureResolver simple() {
        return (id, width, height, options) -> new SimpleTextureHandle(id, width, height, null, options);
    }
}
