package dev.sixik.unigui.api.render;

/**
 * Режим фильтрации texture sampling.
 *
 * <p>Конкретный backend сопоставляет эти значения со своими sampler/filter state.</p>
 */
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