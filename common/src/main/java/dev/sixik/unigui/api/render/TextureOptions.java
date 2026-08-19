package dev.sixik.unigui.api.render;

import java.util.Objects;

/**
 * Immutable sampling/wrap параметры текстуры.
 *
 * <p>Options не владеют GPU ресурсом. Они только описывают, как backend должен сэмплировать texture:
 * min/mag filtering, wrap mode, mipmaps и premultiplied alpha. Методы-модификаторы возвращают новый
 * объект или текущий instance, если значение не изменилось.</p>
 */
public final class TextureOptions {
    private static final TextureOptions DEFAULTS = new TextureOptions(
            TextureFilter.NEAREST,
            TextureFilter.NEAREST,
            TextureWrap.CLAMP_TO_EDGE,
            TextureWrap.CLAMP_TO_EDGE,
            false,
            false);
    private static final TextureOptions LINEAR = DEFAULTS.sampling(TextureFilter.LINEAR);

    private final TextureFilter minFilter;
    private final TextureFilter magFilter;
    private final TextureWrap wrapS;
    private final TextureWrap wrapT;
    private final boolean mipmaps;
    private final boolean premultipliedAlpha;

    private TextureOptions(TextureFilter minFilter,
                           TextureFilter magFilter,
                           TextureWrap wrapS,
                           TextureWrap wrapT,
                           boolean mipmaps,
                           boolean premultipliedAlpha) {
        this.minFilter = minFilter == null ? TextureFilter.NEAREST : minFilter;
        this.magFilter = magFilter == null ? TextureFilter.NEAREST : magFilter;
        this.wrapS = wrapS == null ? TextureWrap.CLAMP_TO_EDGE : wrapS;
        this.wrapT = wrapT == null ? TextureWrap.CLAMP_TO_EDGE : wrapT;
        this.mipmaps = mipmaps;
        this.premultipliedAlpha = premultipliedAlpha;
    }

    /**
     * @return стандартные options UniGUI: nearest filtering и clamp-to-edge
     */
    public static TextureOptions defaults() {
        return DEFAULTS;
    }

    /**
     * @return options для pixel-perfect nearest sampling
     */
    public static TextureOptions nearest() {
        return DEFAULTS;
    }

    /**
     * @return options для сглаженного linear sampling
     */
    public static TextureOptions linear() {
        return LINEAR;
    }

    /** @return фильтр уменьшения текстуры */
    public TextureFilter minFilter() {
        return minFilter;
    }

    /** @return фильтр увеличения текстуры */
    public TextureFilter magFilter() {
        return magFilter;
    }

    /** @return wrap mode по U/S координате */
    public TextureWrap wrapS() {
        return wrapS;
    }

    /** @return wrap mode по V/T координате */
    public TextureWrap wrapT() {
        return wrapT;
    }

    /** @return {@code true}, если backend должен использовать mipmaps */
    public boolean mipmaps() {
        return mipmaps;
    }

    /** @return {@code true}, если texture data уже хранит premultiplied alpha */
    public boolean premultipliedAlpha() {
        return premultipliedAlpha;
    }

    /**
     * Возвращает копию с новым min filter.
     *
     * @param filter новый фильтр
     * @return текущий или новый options object
     */
    public TextureOptions minFilter(TextureFilter filter) {
        TextureFilter normalized = filter == null ? TextureFilter.NEAREST : filter;
        if (minFilter == normalized) return this;
        return new TextureOptions(normalized, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    /**
     * Возвращает копию с новым mag filter.
     *
     * @param filter новый фильтр
     * @return текущий или новый options object
     */
    public TextureOptions magFilter(TextureFilter filter) {
        TextureFilter normalized = filter == null ? TextureFilter.NEAREST : filter;
        if (magFilter == normalized) return this;
        return new TextureOptions(minFilter, normalized, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    /**
     * Задаёт min/mag sampling одним значением.
     *
     * @param filter базовый фильтр sampling'а
     * @return текущий или новый options object
     */
    public TextureOptions sampling(TextureFilter filter) {
        TextureFilter normalized = filter == null ? TextureFilter.NEAREST : filter;
        TextureFilter normalizedMag = magCompatible(normalized);
        if (minFilter == normalized && magFilter == normalizedMag) return this;
        return new TextureOptions(normalized, normalizedMag, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    /**
     * Задаёт одинаковый wrap mode для обеих осей.
     *
     * @param wrap режим wrap для текстуры
     * @return текущий или новый options object
     */
    public TextureOptions wrap(TextureWrap wrap) {
        return wrap(wrap, wrap);
    }

    /**
     * Задаёт wrap mode по обеим осям.
     *
     * @param s wrap mode по U/S координате
     * @param t wrap mode по V/T координате
     * @return текущий или новый options object
     */
    public TextureOptions wrap(TextureWrap s, TextureWrap t) {
        TextureWrap normalizedS = s == null ? TextureWrap.CLAMP_TO_EDGE : s;
        TextureWrap normalizedT = t == null ? TextureWrap.CLAMP_TO_EDGE : t;
        if (wrapS == normalizedS && wrapT == normalizedT) return this;
        return new TextureOptions(minFilter, magFilter, normalizedS, normalizedT, mipmaps, premultipliedAlpha);
    }

    /**
     * Включает или выключает mipmaps.
     *
     * @param mipmaps новое значение
     * @return текущий или новый options object
     */
    public TextureOptions mipmaps(boolean mipmaps) {
        if (this.mipmaps == mipmaps) return this;
        return new TextureOptions(minFilter, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    /**
     * Указывает, хранит ли texture data premultiplied alpha.
     *
     * @param premultipliedAlpha новое значение
     * @return текущий или новый options object
     */
    public TextureOptions premultipliedAlpha(boolean premultipliedAlpha) {
        if (this.premultipliedAlpha == premultipliedAlpha) return this;
        return new TextureOptions(minFilter, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    /** @return {@code true}, если options равны {@link #defaults()} */
    public boolean isDefault() {
        return equals(DEFAULTS);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TextureOptions that)) return false;
        return mipmaps == that.mipmaps
                && premultipliedAlpha == that.premultipliedAlpha
                && minFilter == that.minFilter
                && magFilter == that.magFilter
                && wrapS == that.wrapS
                && wrapT == that.wrapT;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minFilter, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    @Override
    public String toString() {
        return "TextureOptions{" +
                "minFilter=" + minFilter +
                ", magFilter=" + magFilter +
                ", wrapS=" + wrapS +
                ", wrapT=" + wrapT +
                ", mipmaps=" + mipmaps +
                ", premultipliedAlpha=" + premultipliedAlpha +
                '}';
    }

    private static TextureFilter magCompatible(TextureFilter filter) {
        return switch (filter) {
            case NEAREST, NEAREST_MIPMAP_NEAREST -> TextureFilter.NEAREST;
            case LINEAR, LINEAR_MIPMAP_LINEAR -> TextureFilter.LINEAR;
        };
    }
}
