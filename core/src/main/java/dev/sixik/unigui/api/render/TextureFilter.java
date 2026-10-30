package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Режим фильтрации texture sampling.
 *
 * <p>Конкретный backend сопоставляет эти значения со своими sampler/filter state.</p>
 */
@AiDoc(
        summary = "Texture sampling interpolation filter governing minification and magnification algorithms.",
        purpose = "Controls pixel interpolation when textures are rendered at scales different from their native resolution.",
        useWhen = {
                "Use `NEAREST` for retro pixel-art UI, crisp Minecraft textures, and 1:1 pixel alignments.",
                "Use `LINEAR` for smoothly scaled icons, vector rasters, photographic backdrops, and rotated badges.",
                "Use `NEAREST_MIPMAP_NEAREST` or `LINEAR_MIPMAP_LINEAR` for 3D world projections or heavily downscaled surfaces to eliminate moire aliasing artifacts."
        },
        criticalRules = {
                "Default in UniGUI is `NEAREST` to preserve crisp Minecraft pixel-art aesthetics.",
                "Mipmap modes require mipmaps to be generated or provided by the graphics asset loader; if mipmaps are missing, backends fall back to standard minification.",
                "Magnification filter (`magFilter`) cannot use mipmaps because textures are only enlarged; `TextureOptions` automatically maps mipmap modes to non-mipmap filters for mag."
        },
        related = {"TextureOptions", "TextureHandle", "TextureWrap", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Configure linear sampling with mipmaps",
                        order = 10,
                        code = """
                                TextureOptions options = TextureOptions.defaults()
                                        .minFilter(TextureFilter.LINEAR_MIPMAP_LINEAR)
                                        .magFilter(TextureFilter.LINEAR);
                                """)
        })
public enum TextureFilter {
    /** Ближайший пиксель, подходит для pixel-art UI. */
    NEAREST,

    /** Линейная фильтрация для сглаженного масштабирования. */
    LINEAR,

    /** Nearest sampling с nearest mipmap level. */
    NEAREST_MIPMAP_NEAREST,

    /** Linear sampling с linear mipmap interpolation. */
    LINEAR_MIPMAP_LINEAR
}
