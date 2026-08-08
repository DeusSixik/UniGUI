package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.text.FontMetrics;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Font face backed by Java font data, independent from Minecraft's font stack.
 * It rasterizes glyph masks only when an SDF atlas requests them.
 */
public final class AwtFontFace implements SdfGlyphProvider {
    private static final FontRenderContext FONT_CONTEXT =
            new FontRenderContext(null, true, true);

    private final String id;
    private final Font source;
    private final Map<GlyphKey, SdfGlyph> glyphCache = new ConcurrentHashMap<>();
    private final Map<SizeKey, FontMetrics> metricsCache = new ConcurrentHashMap<>();
    private final Map<AdvanceKey, Float> advanceCache = new ConcurrentHashMap<>();

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
        float normalized = normalizedPixelSize(pixelSize);
        return metricsCache.computeIfAbsent(new SizeKey(Float.floatToIntBits(normalized)), ignored -> {
            Font font = font(normalized);
            LineMetrics line = font.getLineMetrics("Ag", FONT_CONTEXT);
            return new FontMetrics(line.getAscent(), line.getDescent(),
                    Math.max(0.0f, line.getLeading()), Math.max(0.0f, line.getHeight()));
        });
    }

    @Override
    public float advance(int codePoint, float pixelSize) {
        float normalized = normalizedPixelSize(pixelSize);
        return advanceCache.computeIfAbsent(
                new AdvanceKey(codePoint, Float.floatToIntBits(normalized)),
                ignored -> {
                    GlyphVector glyphs = glyphVector(codePoint, normalized);
                    return Math.max(0.0f, (float) glyphs.getGlyphMetrics(0).getAdvanceX());
                });
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
        float advance = Math.max(0.0f, (float) glyphs.getGlyphMetrics(0).getAdvanceX());

        return new SdfGlyph(codePoint, width, height, advance, bearingX, bearingY, spread,
                toSignedDistance(glyphs.getGlyphOutline(0), visual, width, height, spread));
    }

    private byte[] toSignedDistance(Shape outline, Rectangle2D visual, int width, int height, int spread) {
        byte[] pixels = new byte[width * height];
        List<Segment> segments = flatten(outline);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double glyphX = visual.getX() - spread + x + 0.5;
                double glyphY = visual.getY() - spread + y + 0.5;
                int index = y * width + x;
                float signed;
                if (segments.isEmpty()) {
                    signed = -spread;
                } else {
                    float distance = distanceToOutline(glyphX, glyphY, segments);
                    signed = outline.contains(glyphX, glyphY) ? distance : -distance;
                }
                signed = Math.max(-spread, Math.min(spread, signed));
                float normalized = 0.5f + signed / (2.0f * spread);
                pixels[index] = (byte) Math.round(
                        Math.max(0.0f, Math.min(1.0f, normalized)) * 255.0f);
            }
        }
        return pixels;
    }

    private static List<Segment> flatten(Shape outline) {
        List<Segment> segments = new ArrayList<>();
        PathIterator iterator = outline.getPathIterator(null, 0.125);
        double[] coords = new double[6];
        double startX = 0.0;
        double startY = 0.0;
        double lastX = 0.0;
        double lastY = 0.0;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO -> {
                    startX = lastX = coords[0];
                    startY = lastY = coords[1];
                }
                case PathIterator.SEG_LINETO -> {
                    addSegment(segments, lastX, lastY, coords[0], coords[1]);
                    lastX = coords[0];
                    lastY = coords[1];
                }
                case PathIterator.SEG_CLOSE -> {
                    addSegment(segments, lastX, lastY, startX, startY);
                    lastX = startX;
                    lastY = startY;
                }
                default -> {
                    // Curves are flattened by PathIterator, so this should not be reached.
                }
            }
            iterator.next();
        }
        return segments;
    }

    private static void addSegment(List<Segment> segments, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx * dx + dy * dy > 0.000001) {
            segments.add(new Segment((float) x1, (float) y1, (float) x2, (float) y2));
        }
    }

    private static float distanceToOutline(double x, double y, List<Segment> segments) {
        double best = Double.POSITIVE_INFINITY;
        for (Segment segment : segments) {
            double dx = segment.x2 - segment.x1;
            double dy = segment.y2 - segment.y1;
            double lengthSq = dx * dx + dy * dy;
            double t = lengthSq <= 0.0 ? 0.0 : ((x - segment.x1) * dx + (y - segment.y1) * dy) / lengthSq;
            t = Math.max(0.0, Math.min(1.0, t));
            double nearestX = segment.x1 + t * dx;
            double nearestY = segment.y1 + t * dy;
            double distanceX = x - nearestX;
            double distanceY = y - nearestY;
            best = Math.min(best, distanceX * distanceX + distanceY * distanceY);
        }
        return (float) Math.sqrt(best);
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

    private record SizeKey(int pixelSizeBits) {
    }

    private record AdvanceKey(int codePoint, int pixelSizeBits) {
    }

    private record Segment(float x1, float y1, float x2, float y2) {
    }
}
