package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;
import java.util.Locale;

/** One independently styled range in a rich text value. */
public record TextRun(String text, FontFace font, float pixelSize, ColorView color,
                      float tracking, TextTransform transform) {
    public static final float DEFAULT_PIXEL_SIZE = 10.0f;

    public TextRun {
        text = text == null ? "" : text;
        transform = transform == null ? TextTransform.NONE : transform;
        if (transform == TextTransform.UPPERCASE) {
            text = text.toUpperCase(Locale.ROOT);
        }
        pixelSize = Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : DEFAULT_PIXEL_SIZE;
        tracking = Float.isFinite(tracking) ? Math.max(0.0f, tracking) : 0.0f;
        color = color == null ? null : new SnapshotColor(color.r(), color.g(), color.b(), color.a());
    }

    public TextRun(String text, FontFace font, float pixelSize) {
        this(text, font, pixelSize, null, 0.0f, TextTransform.NONE);
    }

    public TextRun(String text, FontFace font, float pixelSize, ColorView color) {
        this(text, font, pixelSize, color, 0.0f, TextTransform.NONE);
    }

    public TextRun(String text, FontFace font, float pixelSize, ColorView color, float tracking) {
        this(text, font, pixelSize, color, tracking, TextTransform.NONE);
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    private record SnapshotColor(float r, float g, float b, float a) implements ColorView {
    }
}
