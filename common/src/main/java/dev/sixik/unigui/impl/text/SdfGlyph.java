package dev.sixik.unigui.impl.text;

/** CPU-generated single-channel SDF glyph data used by a render backend. */
public final class SdfGlyph {
    private final int codePoint;
    private final int width;
    private final int height;
    private final float advance;
    private final float bearingX;
    private final float bearingY;
    private final int spread;
    private final byte[] pixels;

    public SdfGlyph(int codePoint, int width, int height, float advance,
                    float bearingX, float bearingY, int spread, byte[] pixels) {
        this.codePoint = codePoint;
        this.width = width;
        this.height = height;
        this.advance = advance;
        this.bearingX = bearingX;
        this.bearingY = bearingY;
        this.spread = spread;
        this.pixels = pixels;
    }

    public int codePoint() { return codePoint; }
    public int width() { return width; }
    public int height() { return height; }
    public float advance() { return advance; }
    public float bearingX() { return bearingX; }
    public float bearingY() { return bearingY; }
    public int spread() { return spread; }
    public byte[] pixels() { return pixels; }
}
