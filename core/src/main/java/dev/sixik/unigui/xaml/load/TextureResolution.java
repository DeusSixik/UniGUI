package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.xml.XmlTextureResolver;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;

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
    private static final ThreadLocal<XmlTextureResolver> CURRENT = new ThreadLocal<>();

    private TextureResolution() {
    }

    /**
     * Делает резолвер действующим в текущем потоке до закрытия возвращённой области.
     *
     * @param resolver резолвер; {@code null} означает резолвер по умолчанию
     * @return область, которую нужно закрыть; закрытие восстанавливает предыдущий резолвер
     */
    public static Scope push(XmlTextureResolver resolver) {
        XmlTextureResolver previous = CURRENT.get();
        CURRENT.set(resolver == null ? XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER : resolver);
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
        XmlTextureResolver resolver = CURRENT.get();
        XmlTextureResolver effective = resolver == null ? XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER : resolver;
        return effective.resolve(id, Math.max(1, width), Math.max(1, height),
                options == null ? TextureOptions.defaults() : options);
    }

    /** Область действия резолвера. */
    public static final class Scope implements AutoCloseable {
        private final XmlTextureResolver previous;

        private Scope(XmlTextureResolver previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
