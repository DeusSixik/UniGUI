package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Immutable sampling/wrap параметры текстуры.
 *
 * <p>Options не владеют GPU ресурсом. Они только описывают, как backend должен сэмплировать texture:
 * min/mag filtering, wrap mode, mipmaps и premultiplied alpha. Методы-модификаторы возвращают новый
 * объект или текущий instance, если значение не изменилось.</p>
 */
@AiDoc(
        summary = "Immutable texture sampling configuration defining min/mag filtering, UV wrapping, mipmaps, and premultiplied alpha.",
        purpose = "Governs GPU texture sampler state, ensuring reproducible visual quality across pixel-art and smooth scaling without owning GPU handles.",
        useWhen = {
                "Define custom texture sampling for `TextureHandle` via `TextureOptions`.",
                "Enable UV tiling patterns using `wrap(TextureWrap.REPEAT)`.",
                "Enable smooth scaling and antialiasing using `sampling(TextureFilter.LINEAR)` and `mipmaps(true)`.",
                "Eliminate dark edge fringing using `premultipliedAlpha(true)`."
        },
        criticalRules = {
                "TextureOptions is strictly immutable; modifier methods return new instances or `this` if values match.",
                "`packed()` provides a 10-bit integer key suitable for O(1) state caching in render backends without heap allocations.",
                "UniGUI defaults use `NEAREST` filtering and `CLAMP_TO_EDGE` wrapping to preserve crisp Minecraft pixel-art aesthetics."
        },
        related = {"TextureHandle", "TextureFilter", "TextureWrap", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Create repeating tiled texture options",
                        order = 10,
                        code = """
                                TextureOptions options = TextureOptions.defaults()
                                        .wrap(TextureWrap.REPEAT);
                                """),
                @AiDoc.Example(
                        title = "Create smooth linear options with mipmaps",
                        order = 20,
                        code = """
                                TextureOptions options = TextureOptions.linear()
                                        .mipmaps(true)
                                        .premultipliedAlpha(true);
                                """)
        })
@WikiPage(
        title = "<key:texture.options.title>",
        slug = "texture-options",
        category = "<key:texture.options.category>",
        summary = "<key:texture.options.summary>",
        order = 40)
@WikiSection(
        title = "<key:texture.options.section.overview.title>",
        markdown = "<key:texture.options.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:texture.options.section.filtering.title>",
        markdown = "<key:texture.options.section.filtering.body>",
        order = 20)
@WikiSection(
        title = "<key:texture.options.section.wrapping.title>",
        markdown = "<key:texture.options.section.wrapping.body>",
        order = 30)
@WikiSection(
        title = "<key:texture.options.section.alpha.title>",
        markdown = "<key:texture.options.section.alpha.body>",
        order = 40)
@WikiSection(
        title = "<key:texture.options.section.packed.title>",
        markdown = "<key:texture.options.section.packed.body>",
        order = 50)
@WikiExample(
        title = "<key:texture.options.example.tiled.title>",
        code = "<key:texture.options.example.tiled.code>",
        order = 10)
@WikiExample(
        title = "<key:texture.options.example.smooth.title>",
        code = "<key:texture.options.example.smooth.code>",
        order = 20)
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
    private final int packed;

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
        this.packed = pack(this.minFilter, this.magFilter, this.wrapS, this.wrapT,
                this.mipmaps, this.premultipliedAlpha);
    }

    /**
     * @return стандартные options UniGUI: nearest filtering и clamp-to-edge
     */
    @WikiMethod(
            title = "<key:texture.options.method.defaults.title>",
            summary = "<key:texture.options.method.defaults.summary>",
            section = "<key:texture.options.api.section.presets>",
            sectionOrder = 10,
            order = 10)
    @WikiReturn("<key:texture.options.method.defaults.return>")
    public static TextureOptions defaults() {
        return DEFAULTS;
    }

    /**
     * @return options для pixel-perfect nearest sampling
     */
    @WikiMethod(
            title = "<key:texture.options.method.nearest.title>",
            summary = "<key:texture.options.method.nearest.summary>",
            section = "<key:texture.options.api.section.presets>",
            sectionOrder = 10,
            order = 20)
    @WikiReturn("<key:texture.options.method.nearest.return>")
    public static TextureOptions nearest() {
        return DEFAULTS;
    }

    /**
     * @return options для сглаженного linear sampling
     */
    @WikiMethod(
            title = "<key:texture.options.method.linear.title>",
            summary = "<key:texture.options.method.linear.summary>",
            section = "<key:texture.options.api.section.presets>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:texture.options.method.linear.return>")
    public static TextureOptions linear() {
        return LINEAR;
    }

    /**
     * @return фильтр уменьшения текстуры
     */
    @WikiMethod(
            title = "<key:texture.options.method.min_filter_get.title>",
            summary = "<key:texture.options.method.min_filter_get.summary>",
            section = "<key:texture.options.api.section.properties>",
            sectionOrder = 20,
            order = 10)
    @WikiReturn("<key:texture.options.method.min_filter_get.return>")
    public TextureFilter minFilter() {
        return minFilter;
    }

    /**
     * @return фильтр увеличения текстуры
     */
    @WikiMethod(
            title = "<key:texture.options.method.mag_filter_get.title>",
            summary = "<key:texture.options.method.mag_filter_get.summary>",
            section = "<key:texture.options.api.section.properties>",
            sectionOrder = 20,
            order = 20)
    @WikiReturn("<key:texture.options.method.mag_filter_get.return>")
    public TextureFilter magFilter() {
        return magFilter;
    }

    /**
     * @return wrap mode по U/S координате
     */
    @WikiMethod(
            title = "<key:texture.options.method.wrap_s_get.title>",
            summary = "<key:texture.options.method.wrap_s_get.summary>",
            section = "<key:texture.options.api.section.properties>",
            sectionOrder = 20,
            order = 30)
    @WikiReturn("<key:texture.options.method.wrap_s_get.return>")
    public TextureWrap wrapS() {
        return wrapS;
    }

    /**
     * @return wrap mode по V/T координате
     */
    @WikiMethod(
            title = "<key:texture.options.method.wrap_t_get.title>",
            summary = "<key:texture.options.method.wrap_t_get.summary>",
            section = "<key:texture.options.api.section.properties>",
            sectionOrder = 20,
            order = 40)
    @WikiReturn("<key:texture.options.method.wrap_t_get.return>")
    public TextureWrap wrapT() {
        return wrapT;
    }

    /**
     * @return {@code true}, если backend должен использовать mipmaps
     */
    @WikiMethod(
            title = "<key:texture.options.method.mipmaps_get.title>",
            summary = "<key:texture.options.method.mipmaps_get.summary>",
            section = "<key:texture.options.api.section.properties>",
            sectionOrder = 20,
            order = 50)
    @WikiReturn("<key:texture.options.method.mipmaps_get.return>")
    public boolean mipmaps() {
        return mipmaps;
    }

    /**
     * @return {@code true}, если texture data уже хранит premultiplied alpha
     */
    @WikiMethod(
            title = "<key:texture.options.method.premultiplied_alpha_get.title>",
            summary = "<key:texture.options.method.premultiplied_alpha_get.summary>",
            section = "<key:texture.options.api.section.properties>",
            sectionOrder = 20,
            order = 60)
    @WikiReturn("<key:texture.options.method.premultiplied_alpha_get.return>")
    public boolean premultipliedAlpha() {
        return premultipliedAlpha;
    }

    /**
     * Возвращает компактный ключ всех параметров texture sampling.
     *
     * <p>Ключ занимает 10 младших бит: по два бита на каждый filter/wrap и по
     * одному биту на {@code mipmaps} и {@code premultipliedAlpha}. Значения enum
     * кодируются явно, поэтому ключ не зависит от порядка констант enum.</p>
     *
     * @return стабильное целочисленное представление options
     */
    @WikiMethod(
            title = "<key:texture.options.method.packed.title>",
            summary = "<key:texture.options.method.packed.summary>",
            section = "<key:texture.options.api.section.packed>",
            sectionOrder = 40,
            order = 10)
    @WikiReturn("<key:texture.options.method.packed.return>")
    public int packed() {
        return packed;
    }

    /**
     * Возвращает копию с новым min filter.
     *
     * @param filter новый фильтр
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:texture.options.method.min_filter_set.title>",
            summary = "<key:texture.options.method.min_filter_set.summary>",
            section = "<key:texture.options.api.section.modifiers>",
            sectionOrder = 30,
            order = 10)
    @WikiReturn("<key:texture.options.method.min_filter_set.return>")
    public TextureOptions minFilter(@WikiParameter(name = "filter", description = "<key:texture.options.param.filter>") TextureFilter filter) {
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
    @WikiMethod(
            title = "<key:texture.options.method.mag_filter_set.title>",
            summary = "<key:texture.options.method.mag_filter_set.summary>",
            section = "<key:texture.options.api.section.modifiers>",
            sectionOrder = 30,
            order = 20)
    @WikiReturn("<key:texture.options.method.mag_filter_set.return>")
    public TextureOptions magFilter(@WikiParameter(name = "filter", description = "<key:texture.options.param.filter>") TextureFilter filter) {
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
    @WikiMethod(
            title = "<key:texture.options.method.sampling_set.title>",
            summary = "<key:texture.options.method.sampling_set.summary>",
            section = "<key:texture.options.api.section.modifiers>",
            sectionOrder = 30,
            order = 30)
    @WikiReturn("<key:texture.options.method.sampling_set.return>")
    public TextureOptions sampling(@WikiParameter(name = "filter", description = "<key:texture.options.param.filter>") TextureFilter filter) {
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
    @WikiMethod(
            title = "<key:texture.options.method.wrap_uniform_set.title>",
            summary = "<key:texture.options.method.wrap_uniform_set.summary>",
            section = "<key:texture.options.api.section.modifiers>",
            sectionOrder = 30,
            order = 40)
    @WikiReturn("<key:texture.options.method.wrap_uniform_set.return>")
    public TextureOptions wrap(@WikiParameter(name = "wrap", description = "<key:texture.options.param.wrap>") TextureWrap wrap) {
        return wrap(wrap, wrap);
    }

    /**
     * Задаёт wrap mode по обеим осям.
     *
     * @param s wrap mode по U/S координате
     * @param t wrap mode по V/T координате
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:texture.options.method.wrap_axes_set.title>",
            summary = "<key:texture.options.method.wrap_axes_set.summary>",
            section = "<key:texture.options.api.section.modifiers>",
            sectionOrder = 30,
            order = 50)
    @WikiReturn("<key:texture.options.method.wrap_axes_set.return>")
    public TextureOptions wrap(@WikiParameter(name = "s", description = "<key:texture.options.param.s>") TextureWrap s, @WikiParameter(name = "t", description = "<key:texture.options.param.t>") TextureWrap t) {
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
    @WikiMethod(
            title = "<key:texture.options.method.mipmaps_set.title>",
            summary = "<key:texture.options.method.mipmaps_set.summary>",
            section = "<key:texture.options.api.section.modifiers>",
            sectionOrder = 30,
            order = 60)
    @WikiReturn("<key:texture.options.method.mipmaps_set.return>")
    public TextureOptions mipmaps(@WikiParameter(name = "mipmaps", description = "<key:texture.options.param.mipmaps>") boolean mipmaps) {
        if (this.mipmaps == mipmaps) return this;
        return new TextureOptions(minFilter, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    /**
     * Указывает, хранит ли texture data premultiplied alpha.
     *
     * @param premultipliedAlpha новое значение
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:texture.options.method.premultiplied_alpha_set.title>",
            summary = "<key:texture.options.method.premultiplied_alpha_set.summary>",
            section = "<key:texture.options.api.section.modifiers>",
            sectionOrder = 30,
            order = 70)
    @WikiReturn("<key:texture.options.method.premultiplied_alpha_set.return>")
    public TextureOptions premultipliedAlpha(@WikiParameter(name = "premultipliedAlpha", description = "<key:texture.options.param.premultipliedAlpha>") boolean premultipliedAlpha) {
        if (this.premultipliedAlpha == premultipliedAlpha) return this;
        return new TextureOptions(minFilter, magFilter, wrapS, wrapT, mipmaps, premultipliedAlpha);
    }

    /**
     * @return {@code true}, если options равны {@link #defaults()}
     */
    @WikiMethod(
            title = "<key:texture.options.method.is_default.title>",
            summary = "<key:texture.options.method.is_default.summary>",
            section = "<key:texture.options.api.section.presets>",
            sectionOrder = 10,
            order = 40)
    @WikiReturn("<key:texture.options.method.is_default.return>")
    public boolean isDefault() {
        return packed == DEFAULTS.packed;
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
        return packed;
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

    private static int pack(TextureFilter minFilter,
                            TextureFilter magFilter,
                            TextureWrap wrapS,
                            TextureWrap wrapT,
                            boolean mipmaps,
                            boolean premultipliedAlpha) {
        return filterCode(minFilter)
                | (filterCode(magFilter) << 2)
                | (wrapCode(wrapS) << 4)
                | (wrapCode(wrapT) << 6)
                | (mipmaps ? 1 << 8 : 0)
                | (premultipliedAlpha ? 1 << 9 : 0);
    }

    private static int filterCode(TextureFilter filter) {
        return switch (filter) {
            case NEAREST -> 0;
            case LINEAR -> 1;
            case NEAREST_MIPMAP_NEAREST -> 2;
            case LINEAR_MIPMAP_LINEAR -> 3;
        };
    }

    private static int wrapCode(TextureWrap wrap) {
        return switch (wrap) {
            case CLAMP_TO_EDGE -> 0;
            case REPEAT -> 1;
            case MIRRORED_REPEAT -> 2;
        };
    }
}
