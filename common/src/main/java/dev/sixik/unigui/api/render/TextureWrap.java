package dev.sixik.unigui.api.render;

/**
 * Режим повторения texture coordinates за пределами диапазона {@code 0..1}.
 */
public enum TextureWrap {
    /** Прижимает координаты к краю текстуры. */
    CLAMP_TO_EDGE,

    /** Повторяет текстуру. */
    REPEAT,

    /** Повторяет текстуру с зеркальным отражением каждого второго тайла. */
    MIRRORED_REPEAT
}