package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Режим повторения texture coordinates за пределами диапазона {@code 0..1}.
 */
@AiDoc(
        summary = "UV coordinate wrapping behaviors when sampling textures beyond the normalized 0.0 to 1.0 UV boundary.",
        purpose = "Defines edge tiling, repeating, or clamping behavior for UV texture coordinates.",
        useWhen = {
                "Use `CLAMP_TO_EDGE` for single UI icons, spritesheets, and standard 9-slice frames to prevent opposite edge bleeding.",
                "Use `REPEAT` for seamless scrolling backgrounds, tiled wallpaper panels, and pattern fills.",
                "Use `MIRRORED_REPEAT` for symmetric tiled textures where adjacent tiles mirror each other."
        },
        criticalRules = {
                "Default in UniGUI is `CLAMP_TO_EDGE`.",
                "When using `REPEAT` or `MIRRORED_REPEAT` on atlas sub-regions, wrapping must be handled via custom shader math or standalone textures; raw GL wrap modes repeat the entire texture atlas."
        },
        related = {"TextureOptions", "TextureHandle", "TextureFilter", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Configure repeating wallpaper wrap",
                        order = 10,
                        code = """
                                TextureOptions tiled = TextureOptions.defaults().wrap(TextureWrap.REPEAT);
                                """)
        })
public enum TextureWrap {
    /**
     * Прижимает координаты к краю текстуры.
     */
    CLAMP_TO_EDGE,

    /**
     * Повторяет текстуру.
     */
    REPEAT,

    /**
     * Повторяет текстуру с зеркальным отражением каждого второго тайла.
     */
    MIRRORED_REPEAT
}
