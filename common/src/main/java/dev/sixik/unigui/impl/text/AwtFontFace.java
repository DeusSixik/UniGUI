package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.text.FontMetrics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
        Font font = font(pixelSize);
        LineMetrics line = font.getLineMetrics("Ag", FONT_CONTEXT);
        return new FontMetrics(line.getAscent(), line.getDescent(),
                Math.max(0.0f, line.getLeading()), Math.max(0.0f, line.getHeight()));
    }

    @Override
    public float advance(int codePoint, float pixelSize) {
        GlyphVector glyphs = glyphVector(codePoint, pixelSize);
        return Math.max(0.0f, (float) glyphs.getGlyphMetrics(0).getAdvanceX());
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

        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = mask.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.drawGlyphVector(glyphs, -((float) visual.getX()) + spread,
                    -((float) visual.getY()) + spread);
        } finally {
            graphics.dispose();
        }

        return new SdfGlyph(codePoint, width, height, advance, bearingX, bearingY, spread,
                toSignedDistance(mask, spread));
    }

    private byte[] toSignedDistance(BufferedImage mask, int spread) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        byte[] pixels = new byte[width * height];
        boolean[] inside = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                inside[y * width + x] = mask.getRaster().getSample(x, y, 0) >= 128;
            }
        }

        float[] distanceToInside = distanceTransform(inside, width, height, true);
        float[] distanceToOutside = distanceTransform(inside, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                float signed = inside[index]
                        ? (float) Math.sqrt(distanceToOutside[index])
                        : -(float) Math.sqrt(distanceToInside[index]);
                signed = Math.max(-spread, Math.min(spread, signed));
                float normalized = 0.5f + signed / (2.0f * spread);
                pixels[index] = (byte) Math.round(
                        Math.max(0.0f, Math.min(1.0f, normalized)) * 255.0f);
            }
        }
        return pixels;
    }

    private static float[] distanceTransform(boolean[] mask, int width, int height, boolean featureValue) {
        float[] firstPass = new float[width * height];
        float[] result = new float[width * height];
        float[] source = new float[Math.max(width, height)];
        float[] target = new float[source.length];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                source[y] = mask[y * width + x] == featureValue ? 0.0f : Float.POSITIVE_INFINITY;
            }
            distanceTransform1d(source, target, height);
            for (int y = 0; y < height; y++) firstPass[y * width + x] = target[y];
        }

        for (int y = 0; y < height; y++) {
            System.arraycopy(firstPass, y * width, source, 0, width);
            distanceTransform1d(source, target, width);
            System.arraycopy(target, 0, result, y * width, width);
        }
        return result;
    }

    private static void distanceTransform1d(float[] source, float[] target, int length) {
        int first = -1;
        for (int i = 0; i < length; i++) {
            if (Float.isFinite(source[i])) {
                first = i;
                break;
            }
        }
        if (first < 0) {
            java.util.Arrays.fill(target, 0, length, Float.POSITIVE_INFINITY);
            return;
        }

        int[] sites = new int[length];
        float[] boundaries = new float[length + 1];
        int last = 0;
        sites[0] = first;
        boundaries[0] = Float.NEGATIVE_INFINITY;
        boundaries[1] = Float.POSITIVE_INFINITY;
        for (int q = first + 1; q < length; q++) {
            if (!Float.isFinite(source[q])) continue;
            float boundary;
            do {
                int site = sites[last];
                boundary = ((source[q] + q * q) - (source[site] + site * site))
                        / (2.0f * (q - site));
                if (boundary <= boundaries[last]) last--;
            } while (last >= 0 && boundary <= boundaries[last]);
            last++;
            sites[last] = q;
            boundaries[last] = boundary;
            boundaries[last + 1] = Float.POSITIVE_INFINITY;
        }

        int siteIndex = 0;
        for (int q = 0; q < length; q++) {
            while (boundaries[siteIndex + 1] < q) siteIndex++;
            float delta = q - sites[siteIndex];
            target[q] = delta * delta + source[sites[siteIndex]];
        }
    }

    private GlyphVector glyphVector(int codePoint, float pixelSize) {
        String value = new String(Character.toChars(codePoint));
        return font(pixelSize).createGlyphVector(FONT_CONTEXT, value);
    }

    private Font font(float pixelSize) {
        float normalized = Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : 16.0f;
        return source.deriveFont(normalized);
    }

    private record GlyphKey(int codePoint, int pixelSize, int spread) {
    }
}
