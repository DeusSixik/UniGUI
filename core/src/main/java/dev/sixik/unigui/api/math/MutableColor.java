package dev.sixik.unigui.api.math;

import java.util.Objects;

public final class MutableColor implements ColorView {
    private float r;
    private float g;
    private float b;
    private float a;
    private Runnable onChanged;

    public MutableColor() {
        this(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public MutableColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static MutableColor rgba255(int r, int g, int b, int a) {
        return new MutableColor(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public static MutableColor rgba(float r, float g, float b, float a) {
        return new MutableColor(r, g, b, a);
    }

    public static MutableColor fromHex(String hex) {
        Objects.requireNonNull(hex, "hex");
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() != 6 && s.length() != 8) {
            throw new IllegalArgumentException("Expected RRGGBB or RRGGBBAA, got: " + hex);
        }
        int r = Integer.parseInt(s.substring(0, 2), 16);
        int g = Integer.parseInt(s.substring(2, 4), 16);
        int b = Integer.parseInt(s.substring(4, 6), 16);
        int a = s.length() == 8 ? Integer.parseInt(s.substring(6, 8), 16) : 255;
        return fromRgbaInts(r, g, b, a);
    }

    public static MutableColor fromArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return fromRgbaInts(r, g, b, a);
    }

    public static MutableColor fromRgba(int rgba) {
        int r = (rgba >> 24) & 0xFF;
        int g = (rgba >> 16) & 0xFF;
        int b = (rgba >> 8) & 0xFF;
        int a = rgba & 0xFF;
        return fromRgbaInts(r, g, b, a);
    }

    public String toHexString() {
        return toHexString(true);
    }

    public String toHexString(boolean includeAlpha) {
        int r = to255(this.r), g = to255(this.g), b = to255(this.b), a = to255(this.a);
        return includeAlpha
                ? String.format("#%02X%02X%02X%02X", r, g, b, a)
                : String.format("#%02X%02X%02X", r, g, b);
    }

    @Override
    public float r() {
        return r;
    }

    @Override
    public float g() {
        return g;
    }

    @Override
    public float b() {
        return b;
    }

    @Override
    public float a() {
        return a;
    }

    public MutableColor set255(int r, int g, int b, int a) {
        return set(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public MutableColor setHex(String hex) {
        return set(fromHex(hex));
    }

    public MutableColor setArgb(int argb) {
        return set(fromArgb(argb));
    }

    public MutableColor setRgba(int rgba) {
        return set(fromRgba(rgba));
    }

    public MutableColor set(float r, float g, float b, float a) {
        if (this.r == r && this.g == g && this.b == b && this.a == a) return this;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        changed();
        return this;
    }

    public MutableColor set(ColorView other) {
        Objects.requireNonNull(other, "other");
        return set(other.r(), other.g(), other.b(), other.a());
    }

    public MutableColor onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public MutableColor copy() {
        return new MutableColor(r, g, b, a);
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }

    public int toArgbInt() {
        int a = to255(this.a), r = to255(this.r), g = to255(this.g), b = to255(this.b);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int toRgbaInt() {
        int r = to255(this.r), g = to255(this.g), b = to255(this.b), a = to255(this.a);
        return (r << 24) | (g << 16) | (b << 8) | a;
    }

    private static MutableColor fromRgbaInts(int r, int g, int b, int a) {
        return new MutableColor(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    private static int to255(float v) {
        return Math.round(Math.max(0f, Math.min(1f, v)) * 255f);
    }
}
