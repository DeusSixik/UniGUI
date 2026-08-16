package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

import java.util.function.UnaryOperator;

/**
 * Вспомогательные операции для XML-атрибутов текстур.
 *
 * <p>Texture handle в UniGUI immutable по смыслу: изменение ширины, высоты или
 * sampling/wrap options создаёт новый handle через текущий {@link XmlTextureResolver}.
 * Этот helper держит такую glue-логику рядом с XML API, чтобы widget setter-ы могли
 * оставаться обычными {@link XmlAttribute} методами.</p>
 */
public final class XmlTextureAttributes {
    private XmlTextureAttributes() {
    }

    /**
     * Возвращает texture handle с обновлённой шириной и/или высотой.
     *
     * <p>{@code null} в width/height означает "оставить старое значение". Если
     * texture отсутствует, метод возвращает {@code null}: XML-атрибут размера,
     * применённый до самого texture id, не создаёт фиктивную текстуру.</p>
     *
     * @param texture исходный texture handle
     * @param width новая ширина или {@code null}
     * @param height новая высота или {@code null}
     * @return исходный handle, если размер не изменился, иначе handle от текущего resolver-а
     */
    public static TextureHandle resize(TextureHandle texture, Integer width, Integer height) {
        if (texture == null) return null;
        int nextWidth = width == null ? texture.width() : positive(width, "width");
        int nextHeight = height == null ? texture.height() : positive(height, "height");
        if (texture.width() == nextWidth && texture.height() == nextHeight) return texture;
        return copy(texture, nextWidth, nextHeight, texture.options());
    }

    /**
     * Возвращает texture handle с изменёнными {@link TextureOptions}.
     *
     * @param texture исходный texture handle
     * @param mutation функция изменения options; {@code null} оставляет options как есть
     * @return исходный handle, если options не изменились, иначе handle от текущего resolver-а
     */
    public static TextureHandle options(TextureHandle texture, UnaryOperator<TextureOptions> mutation) {
        if (texture == null) return null;
        TextureOptions options = texture.options() == null ? TextureOptions.defaults() : texture.options();
        TextureOptions nextOptions = mutation == null ? options : mutation.apply(options);
        if (options.equals(nextOptions)) return texture;
        return copy(texture, texture.width(), texture.height(), nextOptions);
    }

    private static TextureHandle copy(TextureHandle texture, int width, int height, TextureOptions options) {
        return XmlValueParsers.resolveTexture(texture.id(), width, height, options);
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("Expected positive texture " + name + ", got: " + value);
        }
        return value;
    }
}
