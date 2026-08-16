package dev.sixik.unigui.api.render;

import java.util.Objects;

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

    public static TextureOptions defaults() {
        return DEFAULTS;
    }

    public static TextureOptions nearest() {
        return DEFAULTS;
    }

    public static TextureOptions linear() {
        return LINEAR;
    }

    public TextureFilter minFilter() {
        return minFilter;
    }

    public TextureFilter magFilter() {
        return magFilter;
    }

    public TextureWrap wrapS() {
        return wrapS;
    }

    public TextureWrap wrapT() {
        return wrapT;
    }

    public boolean mipmaps() {
        return mipmaps;
    }

    public boolean premultipliedAlpha() {
        return premultipliedAlpha;
    }

    public TextureOptions minFilter(TextureFilter filter) {
        TextureFilter normalized = filter == null ? TextureFilter.NEAREST : filter;
        if (minFilter == normalized) return this;
        return new TextureOptions(normalized, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    public TextureOptions magFilter(TextureFilter filter) {
        TextureFilter normalized = filter == null ? TextureFilter.NEAREST : filter;
        if (magFilter == normalized) return this;
        return new TextureOptions(minFilter, normalized, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    public TextureOptions sampling(TextureFilter filter) {
        TextureFilter normalized = filter == null ? TextureFilter.NEAREST : filter;
        TextureFilter normalizedMag = magCompatible(normalized);
        if (minFilter == normalized && magFilter == normalizedMag) return this;
        return new TextureOptions(normalized, normalizedMag, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    public TextureOptions wrap(TextureWrap wrap) {
        return wrap(wrap, wrap);
    }

    public TextureOptions wrap(TextureWrap s, TextureWrap t) {
        TextureWrap normalizedS = s == null ? TextureWrap.CLAMP_TO_EDGE : s;
        TextureWrap normalizedT = t == null ? TextureWrap.CLAMP_TO_EDGE : t;
        if (wrapS == normalizedS && wrapT == normalizedT) return this;
        return new TextureOptions(minFilter, magFilter, normalizedS, normalizedT, mipmaps, premultipliedAlpha);
    }

    public TextureOptions mipmaps(boolean mipmaps) {
        if (this.mipmaps == mipmaps) return this;
        return new TextureOptions(minFilter, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    public TextureOptions premultipliedAlpha(boolean premultipliedAlpha) {
        if (this.premultipliedAlpha == premultipliedAlpha) return this;
        return new TextureOptions(minFilter, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

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
