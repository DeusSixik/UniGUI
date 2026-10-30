package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.Objects;

/**
 * Простая immutable реализация {@link TextureHandle}.
 *
 * <p>Класс удобен для адаптеров backend'а, тестов и inline texture resolver'ов: он хранит id,
 * размеры, optional native handle и {@link TextureOptions}, не навязывая конкретный тип ресурса.</p>
 */
@AiDoc(
        summary = "Standard immutable implementation of TextureHandle for tests, dynamic texture providers, and backend adapters.",
        purpose = "Provides a concrete, unmodifiable TextureHandle instance encapsulating id, dimensions, optional native handle, and TextureOptions.",
        useWhen = {
                "Instantiate TextureHandle instances in platform backend adapters or test harnesses.",
                "Wrap custom dynamic procedural textures with known pixel dimensions.",
                "Derive texture variations with altered sampling using `withOptions(TextureOptions)`."
        },
        criticalRules = {
                "Dimensions must be non-negative integers.",
                "`withOptions(TextureOptions)` returns a new instance preserving id and nativeHandle."
        },
        related = {"TextureHandle", "TextureOptions", "RenderBackend"},
        examples = {
                @AiDoc.Example(
                        title = "Create simple texture handle",
                        order = 10,
                        code = """
                                TextureHandle handle = new SimpleTextureHandle("mod:icon", 16, 16, nativeTex, TextureOptions.defaults());
                                """)
        })
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
