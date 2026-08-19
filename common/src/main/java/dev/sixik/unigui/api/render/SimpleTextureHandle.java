package dev.sixik.unigui.api.render;

import java.util.Objects;

/**
 * Простая immutable реализация {@link TextureHandle}.
 *
 * <p>Класс удобен для адаптеров backend'а, тестов и inline texture resolver'ов: он хранит id,
 * размеры, optional native handle и {@link TextureOptions}, не навязывая конкретный тип ресурса.</p>
 */
public final class SimpleTextureHandle implements TextureHandle {
    private final String id;
    private final int width;
    private final int height;
    private final Object nativeHandle;
    private final TextureOptions options;

    /**
     * Создаёт handle без native object.
     *
     * @param id id текстуры
     * @param width ширина в пикселях
     * @param height высота в пикселях
     */
    public SimpleTextureHandle(String id, int width, int height) {
        this(id, width, height, null);
    }

    /**
     * Создаёт handle с native object и default options.
     *
     * @param id id текстуры
     * @param width ширина в пикселях
     * @param height высота в пикселях
     * @param nativeHandle backend-specific объект текстуры
     */
    public SimpleTextureHandle(String id, int width, int height, Object nativeHandle) {
        this(id, width, height, nativeHandle, TextureOptions.defaults());
    }

    /**
     * Создаёт handle с полным набором параметров.
     *
     * @param id id текстуры
     * @param width ширина в пикселях
     * @param height высота в пикселях
     * @param nativeHandle backend-specific объект текстуры
     * @param options sampling/wrap параметры
     */
    public SimpleTextureHandle(String id, int width, int height, Object nativeHandle, TextureOptions options) {
        this.id = Objects.requireNonNull(id, "id");
        this.width = width;
        this.height = height;
        this.nativeHandle = nativeHandle;
        this.options = options == null ? TextureOptions.defaults() : options;
    }

    /**
     * Возвращает handle с другими texture options.
     *
     * @param options новые options
     * @return текущий handle, если options совпадают, иначе новый handle
     */
    public SimpleTextureHandle withOptions(TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (this.options.equals(normalized)) return this;
        return new SimpleTextureHandle(id, width, height, nativeHandle, normalized);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public TextureOptions options() {
        return options;
    }

    @Override
    public Object nativeHandle() {
        return nativeHandle;
    }
}