package dev.sixik.unigui.api.layout;

/** Immutable pixel, percentage or automatic size value used by Layout v2. */
public record SizeValue(SizeUnit unit, float value) {
    public static final SizeValue AUTO = new SizeValue(SizeUnit.AUTO, 0.0f);

    public SizeValue {
        unit = unit == null ? SizeUnit.AUTO : unit;
        value = unit == SizeUnit.AUTO ? 0.0f : sanitize(value);
    }

    /** Returns the shared automatic size value. */
    public static SizeValue auto() {
        return AUTO;
    }

    /** Creates a non-negative pixel value. */
    public static SizeValue px(float value) {
        return new SizeValue(SizeUnit.PIXELS, value);
    }

    /** Creates a non-negative percentage value. */
    public static SizeValue percent(float value) {
        return new SizeValue(SizeUnit.PERCENT, value);
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

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
