package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;

/** One independently styled range in a rich text value. */
public record TextRun(String text, FontFace font, float pixelSize, ColorView color) {
    public static final float DEFAULT_PIXEL_SIZE = 10.0f;

    public TextRun {
        text = text == null ? "" : text;
        pixelSize = Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : DEFAULT_PIXEL_SIZE;
        color = color == null ? null : new SnapshotColor(color.r(), color.g(), color.b(), color.a());
    }

    public TextRun(String text, FontFace font, float pixelSize) {
        this(text, font, pixelSize, null);
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    private record SnapshotColor(float r, float g, float b, float a) implements ColorView {
    }
}
