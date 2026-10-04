package dev.sixik.unigui.api.layout;

/** Неизменяемое значение размера в пикселях, процентах или автоматическое, используемое компоновкой V2. */
public record SizeValue(SizeUnit unit, float value) {
    public static final SizeValue AUTO = new SizeValue(SizeUnit.AUTO, 0.0f);

    public SizeValue {
        unit = unit == null ? SizeUnit.AUTO : unit;
        value = unit == SizeUnit.PIXELS || unit == SizeUnit.PERCENT || unit == SizeUnit.FIT_CONTENT
                ? sanitize(value)
                : 0.0f;
    }

    /** Возвращает общее автоматическое значение размера. */
    public static SizeValue auto() {
        return AUTO;
    }

    /** Создаёт неотрицательное значение в пикселях. */
    public static SizeValue px(float value) {
        return new SizeValue(SizeUnit.PIXELS, value);
    }

    /** Создаёт неотрицательное значение в процентах. */
    public static SizeValue percent(float value) {
        return new SizeValue(SizeUnit.PERCENT, value);
    }

    /** Создаёт значение по содержимому, вычисляемое из измеренного содержимого. */
    public static SizeValue content() {
        return new SizeValue(SizeUnit.CONTENT, 0.0f);
    }

    /** Создаёт значение минимального размера содержимого. */
    public static SizeValue minContent() {
        return new SizeValue(SizeUnit.MIN_CONTENT, 0.0f);
    }

    /** Создаёт значение максимального размера содержимого. */
    public static SizeValue maxContent() {
        return new SizeValue(SizeUnit.MAX_CONTENT, 0.0f);
    }

    /**
     * Создаёт размер по содержимому, ограниченный пиксельным пределом:
     * {@code min(max-content, max(min-content, limit))}.
     *
     * @param pixels предел в пикселях
     */
    public static SizeValue fitContent(float pixels) {
        return new SizeValue(SizeUnit.FIT_CONTENT, pixels);
    }

    public boolean isAuto() {
        return unit == SizeUnit.AUTO;
    }

    public boolean isPixels() {
        return unit == SizeUnit.PIXELS;
    }

    public boolean isPercent() {
        return unit == SizeUnit.PERCENT;
    }

    public boolean isContent() {
        return unit == SizeUnit.CONTENT;
    }

    public boolean isMinContent() {
        return unit == SizeUnit.MIN_CONTENT;
    }

    public boolean isMaxContent() {
        return unit == SizeUnit.MAX_CONTENT;
    }

    public boolean isFitContent() {
        return unit == SizeUnit.FIT_CONTENT;
    }

    public boolean isIntrinsic() {
        return unit == SizeUnit.MIN_CONTENT || unit == SizeUnit.MAX_CONTENT || unit == SizeUnit.FIT_CONTENT;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
