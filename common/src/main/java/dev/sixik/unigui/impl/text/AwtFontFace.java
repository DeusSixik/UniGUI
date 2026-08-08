package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.text.FontMetrics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Font face backed by Java font data, independent from Minecraft's font stack.
 * It rasterizes glyph masks only when an SDF atlas requests them.
 */
public final class AwtFontFace implements SdfGlyphProvider {
    private static final float UNIT_PIXEL_SIZE = 64.0f;
    private static final int SDF_SUPERSAMPLE = 4;
    private static final float INF = 1.0e20f;
    private static final FontRenderContext FONT_CONTEXT =
            new FontRenderContext(null, true, true);

    private final String id;
    private final Font source;
    private final Map<GlyphKey, SdfGlyph> glyphCache = new ConcurrentHashMap<>();
    private final Map<Integer, Float> unitAdvanceCache = new ConcurrentHashMap<>();
    private volatile UnitMetrics unitMetrics;

    public AwtFontFace(String id, Font source) {
        this.id = id == null || id.isBlank() ? "font" : id;
        this.source = Objects.requireNonNull(source, "source");
    }

    public Font source() {
        return source;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public FontMetrics metrics(float pixelSize) {
        float size = normalizedPixelSize(pixelSize);
        UnitMetrics metrics = unitMetrics();
        return new FontMetrics(
                metrics.ascent * size,
                metrics.descent * size,
                metrics.leading * size,
                metrics.lineHeight * size);
    }

    @Override
    public float advance(int codePoint, float pixelSize) {
        float size = normalizedPixelSize(pixelSize);
        return unitAdvanceCache.computeIfAbsent(codePoint, ignored -> {
            GlyphVector glyphs = glyphVector(codePoint, UNIT_PIXEL_SIZE);
            return Math.max(0.0f, (float) glyphs.getGlyphMetrics(0).getAdvanceX()) / UNIT_PIXEL_SIZE;
        }) * size;
    }

    @Override
    public SdfGlyph sdfGlyph(int codePoint, int pixelSize, int spread) {
        int normalizedSize = Math.max(4, pixelSize);
        int normalizedSpread = Math.max(1, spread);
        GlyphKey key = new GlyphKey(codePoint, normalizedSize, normalizedSpread);
        return glyphCache.computeIfAbsent(key,
                ignored -> rasterize(codePoint, normalizedSize, normalizedSpread));
    }

    private SdfGlyph rasterize(int codePoint, int pixelSize, int spread) {
        GlyphVector glyphs = glyphVector(codePoint, pixelSize);
        Rectangle2D visual = glyphs.getGlyphVisualBounds(0).getBounds2D();
        int width = Math.max(1, (int) Math.ceil(visual.getWidth()) + spread * 2);
        int height = Math.max(1, (int) Math.ceil(visual.getHeight()) + spread * 2);
        float bearingX = (float) visual.getX() - spread;
        float bearingY = (float) visual.getY() - spread;
        float advance = advance(codePoint, pixelSize);

        return new SdfGlyph(codePoint, width, height, advance, bearingX, bearingY, spread,
                toSignedDistance(glyphs.getGlyphOutline(0), visual, width, height, spread));
    }

    private byte[] toSignedDistance(Shape outline, Rectangle2D visual, int width, int height, int spread) {
        int sampleWidth = width * SDF_SUPERSAMPLE;
        int sampleHeight = height * SDF_SUPERSAMPLE;
        byte[] mask = rasterizeMask(outline, visual, sampleWidth, sampleHeight, spread);
        float[] toInside = new float[mask.length];
        float[] toOutside = new float[mask.length];
        for (int i = 0; i < mask.length; i++) {
            boolean inside = (mask[i] & 0xFF) != 0;
            toInside[i] = inside ? 0.0f : INF;
            toOutside[i] = inside ? INF : 0.0f;
        }
        distanceTransform(toInside, sampleWidth, sampleHeight);
        distanceTransform(toOutside, sampleWidth, sampleHeight);

        byte[] pixels = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0.0f;
                int sampleBase = y * SDF_SUPERSAMPLE * sampleWidth + x * SDF_SUPERSAMPLE;
                for (int sy = 0; sy < SDF_SUPERSAMPLE; sy++) {
                    int row = sampleBase + sy * sampleWidth;
                    for (int sx = 0; sx < SDF_SUPERSAMPLE; sx++) {
                        int sampleIndex = row + sx;
                        boolean inside = (mask[sampleIndex] & 0xFF) != 0;
                        float squaredDistance = inside ? toOutside[sampleIndex] : toInside[sampleIndex];
                        float distance = Float.isFinite(squaredDistance)
                                ? (float) Math.sqrt(squaredDistance) / SDF_SUPERSAMPLE
                                : spread;
                        sum += inside ? distance : -distance;
                    }
                }
                float signed = Math.max(-spread, Math.min(spread,
                        sum / (SDF_SUPERSAMPLE * SDF_SUPERSAMPLE)));
                float normalized = 0.5f + signed / (2.0f * spread);
                pixels[y * width + x] = (byte) Math.round(
                        Math.max(0.0f, Math.min(1.0f, normalized)) * 255.0f);
            }
        }
        return pixels;
    }

    private static byte[] rasterizeMask(Shape outline, Rectangle2D visual,
                                        int sampleWidth, int sampleHeight, int spread) {
        BufferedImage image = new BufferedImage(sampleWidth, sampleHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            graphics.setColor(Color.WHITE);
            graphics.scale(SDF_SUPERSAMPLE, SDF_SUPERSAMPLE);
            graphics.translate(-(visual.getX() - spread), -(visual.getY() - spread));
            graphics.fill(outline);
        } finally {
            graphics.dispose();
        }
        return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    }

    private static void distanceTransform(float[] distances, int width, int height) {
        int scratchLength = Math.max(width, height);
        float[] source = new float[scratchLength];
        float[] target = new float[scratchLength];
        int[] sites = new int[scratchLength];
        float[] boundaries = new float[scratchLength + 1];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) source[y] = distances[y * width + x];
            distanceTransform1d(source, target, height, sites, boundaries);
            for (int y = 0; y < height; y++) distances[y * width + x] = target[y];
        }
        for (int y = 0; y < height; y++) {
            int row = y * width;
            System.arraycopy(distances, row, source, 0, width);
            distanceTransform1d(source, target, width, sites, boundaries);
            System.arraycopy(target, 0, distances, row, width);
        }
    }

    private static void distanceTransform1d(float[] source, float[] target, int length,
                                            int[] sites, float[] boundaries) {
        int first = -1;
        for (int i = 0; i < length; i++) {
            if (Float.isFinite(source[i]) && source[i] < INF) {
                first = i;
                break;
            }
        }
        if (first < 0) {
            java.util.Arrays.fill(target, 0, length, INF);
            return;
        }

        int last = 0;
        sites[0] = first;
        boundaries[0] = Float.NEGATIVE_INFINITY;
        boundaries[1] = Float.POSITIVE_INFINITY;
        for (int q = first + 1; q < length; q++) {
            if (!Float.isFinite(source[q]) || source[q] >= INF) continue;
            float boundary;
            while (true) {
                int site = sites[last];
                boundary = ((source[q] + q * q) - (source[site] + site * site))
                        / (2.0f * (q - site));
                if (boundary > boundaries[last] || last == 0) break;
                last--;
            }
            if (boundary <= boundaries[last]) {
                sites[0] = q;
                boundaries[0] = Float.NEGATIVE_INFINITY;
                boundaries[1] = Float.POSITIVE_INFINITY;
                last = 0;
            } else {
                last++;
                sites[last] = q;
                boundaries[last] = boundary;
                boundaries[last + 1] = Float.POSITIVE_INFINITY;
            }
        }

        int siteIndex = 0;
        for (int q = 0; q < length; q++) {
            while (boundaries[siteIndex + 1] < q) siteIndex++;
            float delta = q - sites[siteIndex];
            target[q] = delta * delta + source[sites[siteIndex]];
        }
    }

    private UnitMetrics unitMetrics() {
        UnitMetrics cached = unitMetrics;
        if (cached != null) return cached;
        LineMetrics line = font(UNIT_PIXEL_SIZE).getLineMetrics("Ag", FONT_CONTEXT);
        UnitMetrics computed = new UnitMetrics(
                line.getAscent() / UNIT_PIXEL_SIZE,
                line.getDescent() / UNIT_PIXEL_SIZE,
                Math.max(0.0f, line.getLeading()) / UNIT_PIXEL_SIZE,
                Math.max(0.0f, line.getHeight()) / UNIT_PIXEL_SIZE);
        unitMetrics = computed;
        return computed;
    }

    private GlyphVector glyphVector(int codePoint, float pixelSize) {
        String value = new String(Character.toChars(codePoint));
        return font(pixelSize).createGlyphVector(FONT_CONTEXT, value);
    }

    private Font font(float pixelSize) {
        return source.deriveFont(normalizedPixelSize(pixelSize));
    }

    private static float normalizedPixelSize(float pixelSize) {
        return Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : 16.0f;
    }

    private record GlyphKey(int codePoint, int pixelSize, int spread) {
    }

    private record UnitMetrics(float ascent, float descent, float leading, float lineHeight) {
    }
}
