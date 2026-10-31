package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Immutable параметры генерации ribbon-ленты.
 *
 * <p>Схлопывает растущий список перегрузок {@code addRibbon}: вместо новых
 * сигнатур под каждый флаг — один options-объект. Методы-модификаторы
 * возвращают новый объект или текущий instance, если значение не изменилось.
 * Стиль повторяет {@link TextureOptions}.</p>
 *
 * <pre>{@code
 * RibbonOptions cables = RibbonOptions.defaults()
 *         .swapUv(true)
 *         .flipV(true);
 * context.addRibbon(texture, points, 12.0f, 32.0f, tint, cables, null);
 * }</pre>
 */
@AiDoc(
        summary = "Immutable value object configuring polyline ribbon mesh generation, corner miters, joint dots, and UV texture transforms.",
        purpose = "Encapsulates ribbon generation parameters into an immutable structure with copy-with mutation and packed 32-bit caching keys.",
        useWhen = {
                "Pass customized ribbon generation settings to DrawScope#ribbon or RibbonMesh#builder.",
                "Rotate or flip UV textures along ribbon paths without modifying texture assets.",
                "Cache precomputed ribbon meshes using packed() as part of a compound cache key."
        },
        criticalRules = {
                "RibbonOptions is immutable. Modifier methods return new instances or this if value unchanged.",
                "Miter limits less than 1.0 are automatically normalized to 1.0.",
                "Use swapUv(true) when ribbon texture pattern is oriented vertically."
        },
        related = {"RibbonMesh", "DrawScope", "DrawMesh"},
        examples = {
                @AiDoc.Example(
                        title = "Using Default Options",
                        order = 10,
                        code = """
                                RibbonOptions basic = RibbonOptions.defaults();
                                scope.ribbon(texture, points, 10.0f, 32.0f, ColorView.WHITE, basic);
                                """),
                @AiDoc.Example(
                        title = "Configuring Vertical Cable UVs and Corner Dots",
                        order = 20,
                        code = """
                                RibbonOptions cableOptions = RibbonOptions.defaults()
                                        .swapUv(true)
                                        .jointDots(true)
                                        .jointSegments(12)
                                        .miterLimit(3.0f);
                                scope.ribbon(cableTexture, wirePoints, 6.0f, 24.0f, ColorView.rgb(0, 220, 255), cableOptions);
                                """)
        })
@WikiPage(
        title = "<key:ribbon.options.title>",
        slug = "ribbon-options",
        category = "<key:ribbon.options.category>",
        summary = "<key:ribbon.options.summary>",
        order = 45)
@WikiSection(
        title = "<key:ribbon.options.section.overview.title>",
        markdown = "<key:ribbon.options.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:ribbon.options.section.immutability.title>",
        markdown = "<key:ribbon.options.section.immutability.body>",
        order = 20)
@WikiSection(
        title = "<key:ribbon.options.section.caching.title>",
        markdown = "<key:ribbon.options.section.caching.body>",
        order = 30)
@WikiExample(
        title = "<key:ribbon.options.example.defaults.title>",
        code = "<key:ribbon.options.example.defaults.code>",
        order = 10)
@WikiExample(
        title = "<key:ribbon.options.example.cable.title>",
        code = "<key:ribbon.options.example.cable.code>",
        order = 20)
public final class RibbonOptions {
    /** Дефолтные параметры: butt caps, без стыков, обычный UV. */
    private static final RibbonOptions DEFAULTS = new RibbonOptions(
            false,
            RibbonMesh.DEFAULT_MITER_LIMIT,
            false,
            false,
            false,
            false,
            0);

    /** Замкнуть контур: последний сегмент соединяет конец с началом. */
    private final boolean closed;
    /** Лимит miter на стыках в половинах ширины ленты; минимум {@code 1}. */
    private final float miterLimit;
    /** Повернуть текстуру на 90°: {@code u} вдоль пути, {@code v} поперёк. */
    private final boolean swapUv;
    /** Отразить финальную U-координату ({@code f(c) = -c}). */
    private final boolean flipU;
    /** Отразить финальную V-координату ({@code f(c) = -c}). */
    private final boolean flipV;
    /** Добавить круглые стыки на внутренних углах. */
    private final boolean jointDots;
    /** Число сегментов круглого стыка; {@code 0} означает авто-подбор. */
    private final int jointSegments;
    /** Компактный ключ boolean-флагов и лимита для кэшей. */
    private final int packed;

    private RibbonOptions(boolean closed,
                          float miterLimit,
                          boolean swapUv,
                          boolean flipU,
                          boolean flipV,
                          boolean jointDots,
                          int jointSegments) {
        this.closed = closed;
        this.miterLimit = Float.isFinite(miterLimit) ? Math.max(1.0f, miterLimit) : RibbonMesh.DEFAULT_MITER_LIMIT;
        this.swapUv = swapUv;
        this.flipU = flipU;
        this.flipV = flipV;
        this.jointDots = jointDots;
        this.jointSegments = Math.max(0, jointSegments);
        this.packed = pack(closed, this.miterLimit, swapUv, flipU, flipV, jointDots, this.jointSegments);
    }

    /**
     * Возвращает дефолтные параметры ленты.
     *
     * @return shared instance с butt caps, без стыков и UV-трансформаций
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.defaults.title>",
            summary = "<key:ribbon.options.method.defaults.summary>",
            order = 10)
    @WikiReturn("<key:ribbon.options.method.defaults.return>")
    public static RibbonOptions defaults() {
        return DEFAULTS;
    }

    /** @return {@code true}, если контур замкнут */
    @WikiMethod(
            title = "<key:ribbon.options.method.closed.get.title>",
            summary = "<key:ribbon.options.method.closed.get.summary>",
            order = 20)
    @WikiReturn("<key:ribbon.options.method.closed.get.return>")
    public boolean closed() {
        return closed;
    }

    /** @return лимит miter в половинах ширины ленты, минимум {@code 1} */
    @WikiMethod(
            title = "<key:ribbon.options.method.miter_limit.get.title>",
            summary = "<key:ribbon.options.method.miter_limit.get.summary>",
            order = 30)
    @WikiReturn("<key:ribbon.options.method.miter_limit.get.return>")
    public float miterLimit() {
        return miterLimit;
    }

    /** @return {@code true}, если U и V меняются местами */
    @WikiMethod(
            title = "<key:ribbon.options.method.swap_uv.get.title>",
            summary = "<key:ribbon.options.method.swap_uv.get.summary>",
            order = 40)
    @WikiReturn("<key:ribbon.options.method.swap_uv.get.return>")
    public boolean swapUv() {
        return swapUv;
    }

    /** @return {@code true}, если финальная U отражается */
    @WikiMethod(
            title = "<key:ribbon.options.method.flip_u.get.title>",
            summary = "<key:ribbon.options.method.flip_u.get.summary>",
            order = 50)
    @WikiReturn("<key:ribbon.options.method.flip_u.get.return>")
    public boolean flipU() {
        return flipU;
    }

    /** @return {@code true}, если финальная V отражается */
    @WikiMethod(
            title = "<key:ribbon.options.method.flip_v.get.title>",
            summary = "<key:ribbon.options.method.flip_v.get.summary>",
            order = 60)
    @WikiReturn("<key:ribbon.options.method.flip_v.get.return>")
    public boolean flipV() {
        return flipV;
    }

    /** @return {@code true}, если на внутренних стыках рисуются круглые точки */
    @WikiMethod(
            title = "<key:ribbon.options.method.joint_dots.get.title>",
            summary = "<key:ribbon.options.method.joint_dots.get.summary>",
            order = 70)
    @WikiReturn("<key:ribbon.options.method.joint_dots.get.return>")
    public boolean jointDots() {
        return jointDots;
    }

    /**
     * Возвращает число сегментов круглого стыка.
     *
     * @return число сегментов; {@code 0} означает авто-подбор по радиусу
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.joint_segments.get.title>",
            summary = "<key:ribbon.options.method.joint_segments.get.summary>",
            order = 80)
    @WikiReturn("<key:ribbon.options.method.joint_segments.get.return>")
    public int jointSegments() {
        return jointSegments;
    }

    /**
     * Возвращает компактный ключ параметров для инвалидации кэшей.
     *
     * <p>Младшие 5 бит — boolean-флаги, дальше квантованный miter-лимит
     * и число сегментов стыка. Два равных options дают равный ключ.</p>
     *
     * @return стабильное целочисленное представление options
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.packed.title>",
            summary = "<key:ribbon.options.method.packed.summary>",
            order = 90)
    @WikiReturn("<key:ribbon.options.method.packed.return>")
    public int packed() {
        return packed;
    }

    /**
     * Возвращает копию с новым флагом замыкания контура.
     *
     * @param closed {@code true} для замкнутого контура
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.closed.set.title>",
            summary = "<key:ribbon.options.method.closed.set.summary>",
            order = 100)
    @WikiReturn("<key:ribbon.options.method.closed.set.return>")
    public RibbonOptions closed(
            @WikiParameter(name = "closed", description = "<key:ribbon.options.method.closed.set.closed>")
            boolean closed) {
        if (this.closed == closed) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым лимитом miter.
     *
     * @param miterLimit лимит в половинах ширины; значения меньше {@code 1} нормализуются к {@code 1}
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.miter_limit.set.title>",
            summary = "<key:ribbon.options.method.miter_limit.set.summary>",
            order = 110)
    @WikiReturn("<key:ribbon.options.method.miter_limit.set.return>")
    public RibbonOptions miterLimit(
            @WikiParameter(name = "miterLimit", description = "<key:ribbon.options.method.miter_limit.set.miter_limit>")
            float miterLimit) {
        float normalized = Float.isFinite(miterLimit) ? Math.max(1.0f, miterLimit) : RibbonMesh.DEFAULT_MITER_LIMIT;
        if (Float.compare(this.miterLimit, normalized) == 0) return this;
        return new RibbonOptions(closed, normalized, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым флагом поворота текстуры.
     *
     * @param swapUv {@code true}: {@code u} вдоль пути, {@code v} поперёк
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.swap_uv.set.title>",
            summary = "<key:ribbon.options.method.swap_uv.set.summary>",
            order = 120)
    @WikiReturn("<key:ribbon.options.method.swap_uv.set.return>")
    public RibbonOptions swapUv(
            @WikiParameter(name = "swapUv", description = "<key:ribbon.options.method.swap_uv.set.swap_uv>")
            boolean swapUv) {
        if (this.swapUv == swapUv) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым флагом отражения U.
     *
     * @param flipU {@code true}, чтобы отразить финальную U
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.flip_u.set.title>",
            summary = "<key:ribbon.options.method.flip_u.set.summary>",
            order = 130)
    @WikiReturn("<key:ribbon.options.method.flip_u.set.return>")
    public RibbonOptions flipU(
            @WikiParameter(name = "flipU", description = "<key:ribbon.options.method.flip_u.set.flip_u>")
            boolean flipU) {
        if (this.flipU == flipU) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым флагом отражения V.
     *
     * @param flipV {@code true}, чтобы отразить финальную V
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.flip_v.set.title>",
            summary = "<key:ribbon.options.method.flip_v.set.summary>",
            order = 140)
    @WikiReturn("<key:ribbon.options.method.flip_v.set.return>")
    public RibbonOptions flipV(
            @WikiParameter(name = "flipV", description = "<key:ribbon.options.method.flip_v.set.flip_v>")
            boolean flipV) {
        if (this.flipV == flipV) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым флагом круглых стыков.
     *
     * @param jointDots {@code true}, чтобы закрывать внутренние углы дисками
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.joint_dots.set.title>",
            summary = "<key:ribbon.options.method.joint_dots.set.summary>",
            order = 150)
    @WikiReturn("<key:ribbon.options.method.joint_dots.set.return>")
    public RibbonOptions jointDots(
            @WikiParameter(name = "jointDots", description = "<key:ribbon.options.method.joint_dots.set.joint_dots>")
            boolean jointDots) {
        if (this.jointDots == jointDots) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым числом сегментов стыка.
     *
     * @param jointSegments число сегментов диска; {@code 0} — авто-подбор по радиусу
     * @return текущий или новый options object
     */
    @WikiMethod(
            title = "<key:ribbon.options.method.joint_segments.set.title>",
            summary = "<key:ribbon.options.method.joint_segments.set.summary>",
            order = 160)
    @WikiReturn("<key:ribbon.options.method.joint_segments.set.return>")
    public RibbonOptions jointSegments(
            @WikiParameter(name = "jointSegments", description = "<key:ribbon.options.method.joint_segments.set.joint_segments>")
            int jointSegments) {
        int normalized = Math.max(0, jointSegments);
        if (this.jointSegments == normalized) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, normalized);
    }

    /** @return {@code true}, если options равны {@link #defaults()} */
    @WikiMethod(
            title = "<key:ribbon.options.method.is_default.title>",
            summary = "<key:ribbon.options.method.is_default.summary>",
            order = 170)
    @WikiReturn("<key:ribbon.options.method.is_default.return>")
    public boolean isDefault() {
        return packed == DEFAULTS.packed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RibbonOptions that)) return false;
        return closed == that.closed
                && Float.compare(miterLimit, that.miterLimit) == 0
                && swapUv == that.swapUv
                && flipU == that.flipU
                && flipV == that.flipV
                && jointDots == that.jointDots
                && jointSegments == that.jointSegments;
    }

    @Override
    public int hashCode() {
        return packed;
    }

    @Override
    public String toString() {
        return "RibbonOptions{"
                + "closed=" + closed
                + ", miterLimit=" + miterLimit
                + ", swapUv=" + swapUv
                + ", flipU=" + flipU
                + ", flipV=" + flipV
                + ", jointDots=" + jointDots
                + ", jointSegments=" + jointSegments
                + '}';
    }

    private static int pack(boolean closed,
                            float miterLimit,
                            boolean swapUv,
                            boolean flipU,
                            boolean flipV,
                            boolean jointDots,
                            int jointSegments) {
        int flags = (closed ? 1 : 0)
                | (swapUv ? 1 << 1 : 0)
                | (flipU ? 1 << 2 : 0)
                | (flipV ? 1 << 3 : 0)
                | (jointDots ? 1 << 4 : 0);
        // miter-лимит квантуем до сотых: дробный шум не должен ломать кэш-ключи.
        int quantizedMiter = Math.round(miterLimit * 100.0f);
        int segments = Math.min(jointSegments, 255);
        return flags | (quantizedMiter << 5) | (segments << 21);
    }
}
