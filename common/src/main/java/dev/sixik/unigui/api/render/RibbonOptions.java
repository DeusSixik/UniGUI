package dev.sixik.unigui.api.render;

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
    public static RibbonOptions defaults() {
        return DEFAULTS;
    }

    /** @return {@code true}, если контур замкнут */
    public boolean closed() {
        return closed;
    }

    /** @return лимит miter в половинах ширины ленты, минимум {@code 1} */
    public float miterLimit() {
        return miterLimit;
    }

    /** @return {@code true}, если U и V меняются местами */
    public boolean swapUv() {
        return swapUv;
    }

    /** @return {@code true}, если финальная U отражается */
    public boolean flipU() {
        return flipU;
    }

    /** @return {@code true}, если финальная V отражается */
    public boolean flipV() {
        return flipV;
    }

    /** @return {@code true}, если на внутренних стыках рисуются круглые точки */
    public boolean jointDots() {
        return jointDots;
    }

    /**
     * Возвращает число сегментов круглого стыка.
     *
     * @return число сегментов; {@code 0} означает авто-подбор по радиусу
     */
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
    public int packed() {
        return packed;
    }

    /**
     * Возвращает копию с новым флагом замыкания контура.
     *
     * @param closed {@code true} для замкнутого контура
     * @return текущий или новый options object
     */
    public RibbonOptions closed(boolean closed) {
        if (this.closed == closed) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым лимитом miter.
     *
     * @param miterLimit лимит в половинах ширины; значения меньше {@code 1} нормализуются к {@code 1}
     * @return текущий или новый options object
     */
    public RibbonOptions miterLimit(float miterLimit) {
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
    public RibbonOptions swapUv(boolean swapUv) {
        if (this.swapUv == swapUv) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым флагом отражения U.
     *
     * @param flipU {@code true}, чтобы отразить финальную U
     * @return текущий или новый options object
     */
    public RibbonOptions flipU(boolean flipU) {
        if (this.flipU == flipU) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым флагом отражения V.
     *
     * @param flipV {@code true}, чтобы отразить финальную V
     * @return текущий или новый options object
     */
    public RibbonOptions flipV(boolean flipV) {
        if (this.flipV == flipV) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым флагом круглых стыков.
     *
     * @param jointDots {@code true}, чтобы закрывать внутренние углы дисками
     * @return текущий или новый options object
     */
    public RibbonOptions jointDots(boolean jointDots) {
        if (this.jointDots == jointDots) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, jointSegments);
    }

    /**
     * Возвращает копию с новым числом сегментов стыка.
     *
     * @param jointSegments число сегментов диска; {@code 0} — авто-подбор по радиусу
     * @return текущий или новый options object
     */
    public RibbonOptions jointSegments(int jointSegments) {
        int normalized = Math.max(0, jointSegments);
        if (this.jointSegments == normalized) return this;
        return new RibbonOptions(closed, miterLimit, swapUv, flipU, flipV, jointDots, normalized);
    }

    /** @return {@code true}, если options равны {@link #defaults()} */
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
