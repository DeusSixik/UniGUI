package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Backend-neutral handle текстуры.
 *
 * <p>UniGUI хранит в draw-командах только этот контракт. Реальный OpenGL/Minecraft/native объект
 * может быть доступен через {@link #nativeHandle()}, но виджеты не должны зависеть от его типа.</p>
 */
@AiDoc(
        summary = "Backend-neutral texture handle representing 2D image assets for UI drawing, batching, and sampling.",
        purpose = "Decouples widget rendering code from low-level graphics handles (OpenGL texture IDs, Vulkan descriptors, Minecraft ResourceLocations).",
        useWhen = {
                "Pass texture references into `DrawScope#texture`, `DrawScope#nineSlice`, or `DrawScope#addTexturedQuad`.",
                "Wrap platform texture assets in cross-backend custom widgets.",
                "Query physical pixel dimensions (`width()`, `height()`) to compute aspect ratios and normalized UV coordinates."
        },
        criticalRules = {
                "`id()` must return a stable, unique identifier string to maximize draw-call batching in render backends.",
                "Widgets must never cast `nativeHandle()` directly to maintain cross-platform compatibility.",
                "`options()` defines immutable sampling and wrapping parameters attached to the texture."
        },
        related = {"SimpleTextureHandle", "TextureOptions", "TextureFilter", "TextureWrap", "NineSlice", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Draw texture with normalized UV coordinates",
                        order = 10,
                        code = """
                                float u0 = 0.0f;
                                float v0 = 0.0f;
                                float uWidth = 1.0f;
                                float vHeight = 1.0f;
                                scope.texture(icon, x, y, width, height, u0, v0, uWidth, vHeight, paint);
                                """)
        })
@WikiPage(
        title = "<key:texture.handle.title>",
        slug = "texture-handle",
        category = "<key:texture.handle.category>",
        summary = "<key:texture.handle.summary>",
        order = 20)
@WikiSection(
        title = "<key:texture.handle.section.overview.title>",
        markdown = "<key:texture.handle.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:texture.handle.section.dimensions.title>",
        markdown = "<key:texture.handle.section.dimensions.body>",
        order = 20)
@WikiSection(
        title = "<key:texture.handle.section.options.title>",
        markdown = "<key:texture.handle.section.options.body>",
        order = 30)
@WikiSection(
        title = "<key:texture.handle.section.native.title>",
        markdown = "<key:texture.handle.section.native.body>",
        order = 40)
@WikiExample(
        title = "<key:texture.handle.example.title>",
        code = "<key:texture.handle.example.code>",
        order = 10)
public interface TextureHandle {
    /**
     * @return стабильный id текстуры
     */
    @WikiMethod(
            title = "<key:texture.handle.method.id.title>",
            summary = "<key:texture.handle.method.id.summary>",
            section = "<key:texture.handle.api.section.methods>",
            sectionOrder = 10,
            order = 10)
    @WikiReturn("<key:texture.handle.method.id.return>")
    String id();

    /**
     * @return ширина текстуры в пикселях
     */
    @WikiMethod(
            title = "<key:texture.handle.method.width.title>",
            summary = "<key:texture.handle.method.width.summary>",
            section = "<key:texture.handle.api.section.methods>",
            sectionOrder = 10,
            order = 20)
    @WikiReturn("<key:texture.handle.method.width.return>")
    int width();

    /**
     * @return высота текстуры в пикселях
     */
    @WikiMethod(
            title = "<key:texture.handle.method.height.title>",
            summary = "<key:texture.handle.method.height.summary>",
            section = "<key:texture.handle.api.section.methods>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:texture.handle.method.height.return>")
    int height();

    /**
     * @return sampling/wrap параметры текстуры
     */
    @WikiMethod(
            title = "<key:texture.handle.method.options.title>",
            summary = "<key:texture.handle.method.options.summary>",
            section = "<key:texture.handle.api.section.methods>",
            sectionOrder = 10,
            order = 40)
    @WikiReturn("<key:texture.handle.method.options.return>")
    default TextureOptions options() {
        return TextureOptions.defaults();
    }

    /**
     * Возвращает backend-specific handle.
     *
     * @return native texture object или {@code null}
     */
    @WikiMethod(
            title = "<key:texture.handle.method.native_handle.title>",
            summary = "<key:texture.handle.method.native_handle.summary>",
            section = "<key:texture.handle.api.section.methods>",
            sectionOrder = 10,
            order = 50)
    @WikiReturn("<key:texture.handle.method.native_handle.return>")
    default Object nativeHandle() {
        return null;
    }
}
