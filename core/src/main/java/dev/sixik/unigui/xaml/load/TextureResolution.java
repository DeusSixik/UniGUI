package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

import java.util.function.UnaryOperator;

/**
 * Резолвер текстур, действующий на время загрузки документа в текущем потоке.
 *
 * <p>Нужен потому, что часть setter-ов виджетов ({@code textureWidth}, {@code textureSampling}…) перепривязывает
 * уже созданную текстуру к новым размерам или параметрам и должна делать это тем же резолвером, что и загрузка.
 * Эти методы входят в публичный Java-API виджетов и вызываются в том числе вне загрузки, поэтому передать
 * контекст аргументом нельзя. Вне загрузки действует резолвер по умолчанию.</p>
 *
 * <p>Область действия строго ограничена блоком {@code try-with-resources} вызывающего: загрузчик открывает её
 * на время прогона и всегда закрывает, восстанавливая предыдущее значение.</p>
 */
public final class TextureResolution {
    private static final ThreadLocal<XamlTextureResolver> CURRENT = new ThreadLocal<>();

    private TextureResolution() {
    }

    /**
     * Делает резолвер действующим в текущем потоке до закрытия возвращённой области.
     *
     * @param resolver резолвер; {@code null} означает резолвер по умолчанию
     * @return область, которую нужно закрыть; закрытие восстанавливает предыдущий резолвер
     */
    public static Scope push(XamlTextureResolver resolver) {
        XamlTextureResolver previous = CURRENT.get();
        CURRENT.set(resolver == null ? XamlTextureResolver.DEFAULT : resolver);
        return new Scope(previous);
    }

    /**
     * Разрешает идентификатор текстуры действующим резолвером.
     *
     * @param id      идентификатор
     * @param width   ширина исходной текстуры, минимум 1
     * @param height  высота исходной текстуры, минимум 1
     * @param options параметры; {@code null} означает значения по умолчанию
     * @return runtime-handle
     */
    public static TextureHandle resolve(String id, int width, int height, TextureOptions options) {
        XamlTextureResolver resolver = CURRENT.get();
        XamlTextureResolver effective = resolver == null ? XamlTextureResolver.DEFAULT : resolver;
        return effective.resolve(id, Math.max(1, width), Math.max(1, height),
                options == null ? TextureOptions.defaults() : options);
    }

    /**
     * Перепривязывает текстуру к новым размерам тем же резолвером. Нужен setter-ам {@code textureWidth} и
     * {@code textureHeight}.
     *
     * @param texture текущая текстура; {@code null} остаётся {@code null}
     * @param width   новая ширина или {@code null}, чтобы оставить прежнюю
     * @param height  новая высота или {@code null}, чтобы оставить прежнюю
     * @return та же текстура, если размеры не изменились, иначе новый handle
     * @throws IllegalArgumentException если размер не положительный
     */
    public static TextureHandle resize(TextureHandle texture, Integer width, Integer height) {
        if (texture == null) return null;
        int nextWidth = width == null ? texture.width() : positive(width, "width");
        int nextHeight = height == null ? texture.height() : positive(height, "height");
        if (texture.width() == nextWidth && texture.height() == nextHeight) return texture;
        return resolve(texture.id(), nextWidth, nextHeight, texture.options());
    }

    /**
     * Перепривязывает текстуру к изменённым параметрам выборки. Нужен setter-ам {@code textureSampling},
     * {@code textureWrap} и подобным.
     *
     * @param texture  текущая текстура; {@code null} остаётся {@code null}
     * @param mutation изменение параметров
     * @return та же текстура, если параметры не изменились, иначе новый handle
     */
    public static TextureHandle options(TextureHandle texture, UnaryOperator<TextureOptions> mutation) {
        if (texture == null) return null;
        TextureOptions current = texture.options() == null ? TextureOptions.defaults() : texture.options();
        TextureOptions next = mutation == null ? current : mutation.apply(current);
        if (current.equals(next)) return texture;
        return resolve(texture.id(), texture.width(), texture.height(), next);
    }

    private static int positive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException("Expected positive texture " + name + ", got: " + value);
        return value;
    }

    /** Область действия резолвера. */
    public static final class Scope implements AutoCloseable {
        private final XamlTextureResolver previous;

        private Scope(XamlTextureResolver previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
